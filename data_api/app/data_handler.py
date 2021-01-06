import json
import logging
import os
import pymongo
import re
import ssl
import time
from datetime import datetime, timedelta
from auth487 import flask as ath, common as acm
from bson import ObjectId

from collections import defaultdict


def get_env_param(name, def_val=None, try_file=False):
    val = os.getenv(name, def_val)

    if try_file and val and os.path.isfile(val):
        with open(val) as fp:
            return fp.read().strip()

    return val


CONNECT_TIMEOUT = 500
TZ_OFFSET = int(os.getenv('TZ_OFFSET', 3))

MONGO_HOST = get_env_param('MONGO_HOST', 'localhost', try_file=True)
MONGO_PORT = int(get_env_param('MONGO_PORT', '27017', try_file=True))

MONGO_REPLICA_SET = get_env_param('MONGO_REPLICA_SET', try_file=True)
MONGO_SSL_CERT = get_env_param('MONGO_SSL_CERT', try_file=True)

MONGO_USER = get_env_param('MONGO_USER', try_file=True)
MONGO_PASSWORD = get_env_param('MONGO_PASSWORD', try_file=True)
MONGO_AUTH_SOURCE = get_env_param('MONGO_AUTH_SOURCE', try_file=True)

MONGO_DB_NAME = get_env_param('MONGO_DB_NAME', 'sms487')

date_time_pattern = re.compile(r'^(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}(?::\d{2})?)(?:\s[+-]\d+)?$')
short_date_time_pattern = re.compile(r'^(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}).*')
message_type_pattern = re.compile(r'^\w{3,32}$')

_mongo_client = None


class FormDataError(Exception):
    pass


class EmptyData(Exception):
    pass


def get_login():
    login = acm.extract_auth_info(ath.get_auth_token()).get('login')
    if not login:
        raise FormDataError('Token has no login')
    return login


def get_sms(device_id, limit=None):
    params = {'login': get_login()}
    if device_id:
        params['device_id'] = device_id

    if not limit:
        limit = 256

    cursor = _get_sms_collection().find(params).sort(
        [('date_time', pymongo.DESCENDING), ('device_id', pymongo.ASCENDING)]
    ).limit(limit * 5)

    result = []
    for idx, doc in enumerate(deduplicate_messages(cursor)):
        if idx == limit:
            break

        result.append(dress_sms_doc(doc))

    return result


def add_sms(data):
    login = get_login()

    message_type = data.get('message_type', '').strip()
    device_id = data.get('device_id', '').strip()
    tel = data.get('tel', '').strip()
    date_time = data.get('date_time', '').strip()
    sms_date_time = data.get('sms_date_time', date_time).strip()
    text = data.get('text', '').strip()

    if not message_type:
        raise FormDataError('There is no message type')

    if not message_type_pattern.match(message_type):
        raise FormDataError('Wrong message type format')

    if not device_id:
        raise FormDataError('There is no device ID')

    if not tel:
        raise FormDataError('There is no tel')

    if not date_time:
        raise FormDataError('There is no date_time')

    if not sms_date_time:
        raise FormDataError('There is no sms_date_time')

    if not date_time_pattern.match(date_time):
        raise FormDataError('date_time is incorrect')

    if not date_time_pattern.match(sms_date_time):
        raise FormDataError('sms_date_time is incorrect')

    if not text:
        raise FormDataError('There is no text')

    if len(text) > 2048:
        raise FormDataError('Text is too long')

    _get_sms_collection().insert({
        'login': login,
        'message_type': message_type,
        'device_id': device_id,
        'tel': tel,
        'date_time': date_time,
        'sms_date_time': sms_date_time,
        'text': text,
    })


def deduplicate_messages(cursor):
    param_set = set()

    for message in cursor:
        params_key = tuple((k, message[k]) for k in ('device_id', 'tel', 'text'))
        if params_key in param_set:
            continue

        param_set.add(params_key)
        yield message


def dress_sms_doc(doc):
    result = {}

    for n, v in doc.items():
        if not n.startswith('_'):
            result[n] = v

    if 'message_type' not in result:
        result['message_type'] = 'sms'

    result['printable_message_type'] = 'SMS'
    if result['message_type'] == 'notification':
        result['printable_message_type'] = 'Notification'

    result['date_time'] = format_date_time(result['date_time'])

    if 'sms_date_time' in result:
        result['sms_date_time'] = format_date_time(result['sms_date_time'])
    else:
        result['sms_date_time'] = result['date_time']

    result['printable_date_time'] = result['date_time']
    if result['date_time'] != result['sms_date_time']:
        result['printable_date_time'] += ' (%s)' % result['sms_date_time']

    return result


def format_date_time(dt):
    match = short_date_time_pattern.match(dt)
    if not match:
        return dt

    raw_date = match.group(1) + ' UTC'
    date_time = datetime.strptime(raw_date, '%Y-%m-%d %H:%M %Z') + timedelta(hours=TZ_OFFSET)

    return date_time.strftime('%d %b %Y %H:%M')


def get_device_ids():
    login = get_login()
    device_ids = _get_sms_collection().distinct('device_id', filter={'login': login})
    device_ids.sort()
    return device_ids


def get_filters():
    login = get_login()
    cursor = _get_filters_collection().find({'login': login}).sort('created', pymongo.DESCENDING)

    result = []
    for mongo_item in cursor:
        result.append(dict(
            get_filter_fields(mongo_item, validate=False),
            id=str(mongo_item['_id']),
        ))

    return result


def get_filter_fields(filter_record, validate=True):
    fields = {}
    for name in ('op', 'tel', 'device_id', 'text', 'action'):
        val = filter_record.get(name, '').strip()
        fields[name] = str(val or '')

    if validate:
        if fields['op'] not in {'or', 'and'}:
            raise FormDataError(f'Invalid op: {fields["op"]}')

        if fields['action'] not in {'mark', 'hide'}:
            raise FormDataError(f'Invalid action: {fields["action"]}')

        if not fields['tel'] and not fields['device_id'] and not fields['text']:
            raise EmptyData('All text fields are empty')

    return fields


def save_filters(form_data):
    data = defaultdict(dict)
    for name, value in form_data.items():
        if name == acm.CSRF_FIELD_NAME:
            continue

        name_data = name.split(':')
        if len(name_data) != 2:
            raise FormDataError(f'Invalid field name: {name}')

        field, rec_id = name_data
        data[rec_id][field] = value

    login = get_login()

    delete_queries = []
    update_queries = []

    for rec_id, rec_data in data.items():
        if rec_id == 'new':
            continue

        if not ObjectId.is_valid(rec_id):
            raise FormDataError(f'Invalid filter ID: {rec_id}')

        rec_filter = {'login': login, '_id': ObjectId(rec_id)}

        if rec_data.get('remove') == '1':
            delete_queries.append({'filter': rec_filter})
            continue

        try:
            fields = get_filter_fields(rec_data)
        except EmptyData:
            delete_queries.append({'filter': rec_filter})
            continue

        update_queries.append({
            'filter': rec_filter,
            'update': {'$set': fields},
        })

    collection = _get_filters_collection()

    for query in delete_queries:
        collection.delete_one(**query)

    for query in update_queries:
        collection.update_one(**query)

    try:
        new_data = get_filter_fields(data.get('new', {}))
        doc = dict(new_data, login=login, created=time.time())
        collection.insert_one(doc)
    except EmptyData:
        logging.info('No new filter')


def import_filters(file_obj):
    if not file_obj.filename:
        raise FormDataError('No uploaded file')

    if file_obj.mimetype != 'application/json':
        raise FormDataError(f'Invalid content type: {file_obj.mimetype}. Need JSON')

    try:
        filter_data = json.load(file_obj.stream)
    except ValueError as e:
        raise FormDataError(f'Invalid JSON: {e}')

    if not isinstance(filter_data, list):
        raise FormDataError('Filter data should be a list')

    login = get_login()
    queries = []

    for filter_item in filter_data:
        rec_id = filter_item.get('id')
        if not ObjectId.is_valid(rec_id):
            raise FormDataError(f'Invalid filter ID: {rec_id}. Should be in "id" field')

        try:
            fields = get_filter_fields(filter_item)
        except EmptyData:
            continue

        queries.append({
            'filter': {'login': login, '_id': ObjectId(rec_id)},
            'update': {'$set': fields},
        })

    collection = _get_filters_collection()
    for query in queries:
        collection.update_one(upsert=True, **query)


def _get_mongo_client():
    global _mongo_client
    if _mongo_client:
        return _mongo_client

    logging.info('Connecting to MongoDB: %s:%s', MONGO_HOST, MONGO_PORT)

    mongo_options = dict(
        connectTimeoutMS=CONNECT_TIMEOUT,
        authSource=MONGO_DB_NAME,
    )

    if MONGO_REPLICA_SET:
        mongo_options['replicaSet'] = MONGO_REPLICA_SET

    _mongo_client = pymongo.MongoClient(
        MONGO_HOST, MONGO_PORT,
        connect=True,
        username=MONGO_USER,
        password=MONGO_PASSWORD,
        ssl_ca_certs=MONGO_SSL_CERT,
        ssl_cert_reqs=ssl.CERT_REQUIRED if MONGO_SSL_CERT else ssl.CERT_NONE,
        **mongo_options
    )

    return _mongo_client


def _get_sms_collection():
    client = _get_mongo_client()
    collection = client[MONGO_DB_NAME]['sms_items']

    collection.create_index([
        ('date_time', pymongo.ASCENDING),
    ], background=True, expireAfterSeconds=172800)

    collection.create_index([
        ('login', pymongo.ASCENDING),
        ('device_id', pymongo.ASCENDING),
    ], background=True)

    collection.create_index([
        ('login', pymongo.ASCENDING),
        ('tel', pymongo.ASCENDING),
    ], background=True)

    collection.create_index([
        ('login', pymongo.ASCENDING),
        ('date_time', pymongo.DESCENDING),
    ], background=True)

    collection.create_index([
        ('login', pymongo.ASCENDING),
        ('date_time', pymongo.DESCENDING),
        ('device_id', pymongo.ASCENDING),
    ], background=True)

    return collection


def _get_filters_collection():
    client = _get_mongo_client()
    collection = client[MONGO_DB_NAME]['filters']

    collection.create_index([
        ('login', pymongo.ASCENDING),
        ('created', pymongo.DESCENDING),
    ], background=True)

    return collection
