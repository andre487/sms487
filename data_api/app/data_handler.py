import json
import logging
import os
import re
import time
from collections import defaultdict
from collections.abc import Mapping
from datetime import datetime, timedelta

import boto3
import pymongo
from auth487 import common as acm, flask as ath
from botocore.exceptions import ClientError
from bson import ObjectId

from app.secret_provider import SecretProvider

CONNECT_TIMEOUT = 500
TZ_OFFSET = int(os.getenv('TZ_OFFSET', 3))

date_time_pattern = re.compile(r'^(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}(?::\d{2})?)(?:\s[+-]\d+)?$')
short_date_time_pattern = re.compile(r'^(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}).*')
message_type_pattern = re.compile(r'^\w{3,32}$')

_mongo_client = None
_sqs_client = None


class FormDataError(Exception):
    pass


class EmptyData(Exception):
    pass


def get_login():
    login = ath.extract_auth_info_from_token().get('name')
    if not login:
        raise FormDataError('Token has no login')
    return login


def get_sms(device_id=None, limit=None, apply_filters=True, ids=None):
    if ids:
        query = {'_id': {'$in': ids}}
    else:
        query = {'login': get_login()}
        if device_id:
            query['device_id'] = device_id

    if not limit:
        limit = 256

    if apply_filters:
        query.update(get_filters_match_query())

    sort_key = {
        'date_time': pymongo.DESCENDING,
        'device_id': pymongo.ASCENDING,
        '_id': pymongo.DESCENDING,
    }

    aggregation = [
        {'$match': query},
        {'$sort': sort_key},
        {'$limit': limit * 10},
        {'$group': {
            '_id': {'device_id': '$device_id', 'tel': '$tel', 'text_prefix': {'$substrCP': ['$text', 0, 128]}},
            'message_type': {'$last': '$message_type'},
            'device_id': {'$last': '$device_id'},
            'tel': {'$last': '$tel'},
            'date_time': {'$last': '$date_time'},
            'sms_date_time': {'$last': '$sms_date_time'},
            'text': {'$last': '$text'},
        }},
        {'$sort': sort_key},
        {'$limit': limit},
    ]

    if apply_filters:
        aggregation.extend(get_filters_mongo_aggregations())

    cursor = _get_sms_collection().aggregate(aggregation)

    return [dress_sms_doc(doc) for doc in cursor]


def add_sms(data):
    if not isinstance(data, (list, tuple)):
        data = (data,)

    login = get_login()
    docs = [create_add_sms_doc(login, x) for x in data]

    res = _get_sms_collection().insert_many(docs)
    inserted_count = len(res.inserted_ids)

    inserted_docs = get_sms(apply_filters=True, ids=res.inserted_ids)
    if inserted_docs:
        sqs_client, sqs_params = get_sqs_client()
        if sqs_params.access_key:
            try:
                res = sqs_client.send_message(
                    QueueUrl=sqs_params.queue_url,
                    MessageGroupId='0',
                    MessageBody=json.dumps({
                        'type': 'new_messages',
                        'data': inserted_docs,
                    }),
                )
                logging.info(f'Message to SQS sent: {res}')
            except ClientError as e:
                logging.error(e)
        else:
            logging.warning('SQS Access KEY is empty')

    return inserted_count


def create_add_sms_doc(login, data):
    if not isinstance(data, Mapping):
        raise FormDataError('Message data should be an Object')

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

    return {
        'login': login,
        'message_type': message_type,
        'device_id': device_id,
        'tel': tel,
        'date_time': date_time,
        'sms_date_time': sms_date_time,
        'text': text,
        'created': datetime.utcnow(),
    }


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


def get_filters_match_query():
    result = []

    for data in get_filters():
        if data.get('action') != 'hide':
            continue

        tel = data.get('tel')
        device_id = data.get('device_id')
        text = data.get('text')

        item = []
        if tel:
            item.append({'tel': tel})

        if device_id:
            item.append({'device_id': device_id})

        if text:
            text = re.escape(text)
            item.append({'text': {'$regex': f'.*{text}.*'}})

        if not item:
            continue

        operand = '$and' if data.get('op') == 'and' else '$or'
        result.append({operand: item})

    if result:
        return {'$nor': [{'$or': result}]}

    return {}


def get_filters_mongo_aggregations():
    result = []

    for data in get_filters():
        if data.get('action') != 'mark':
            continue

        tel = data.get('tel')
        device_id = data.get('device_id')
        text = data.get('text')

        item = []
        if tel:
            item.append({'$eq': ('$tel', tel)})

        if device_id:
            item.append({'$eq': ('$device_id', device_id)})

        if text:
            text = re.escape(text)
            item.append({
                '$regexMatch': {
                    'input': '$text',
                    'regex': f'.*{text}.*',
                }
            })

        if not item:
            continue

        operand = '$and' if data.get('op') == 'and' else '$or'
        result.append({operand: item})

    if result:
        return (
            {'$addFields': {'marked': {'$or': result}}},
        )

    return ()


def get_filter_fields(filter_record, validate=True):
    fields = {}
    for name in ('op', 'tel', 'device_id', 'text', 'action'):
        val = filter_record.get(name, '').strip()
        fields[name] = str(val or '')

    if validate:
        if fields['op'] not in {'or', 'and'}:
            raise FormDataError(f'Invalid op: {fields["op"]} in {filter_record}')

        if fields['action'] not in {'mark', 'hide'}:
            raise FormDataError(f'Invalid action: {fields["action"]} in {filter_record}')

        if not fields['tel'] and not fields['device_id'] and not fields['text']:
            raise EmptyData(f'All text fields are empty in {filter_record}')

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

    new_data = data.get('new')
    if not new_data:
        return

    try:
        new_data = get_filter_fields(new_data)
        # noinspection PyTypeChecker
        doc = dict(new_data, login=login, created=time.time())
        collection.insert_one(doc)
    except EmptyData:
        logging.info('No new filter')


def import_filters(file_obj):
    if not file_obj or not file_obj.filename:
        raise FormDataError('No uploaded file')

    if file_obj.mimetype != 'application/json':
        raise FormDataError(f'Invalid content type: {file_obj.mimetype}. Need JSON')

    try:
        filter_data = json.load(file_obj.stream)
    except ValueError as e:
        raise FormDataError(f'Invalid JSON: {e}')

    if not isinstance(filter_data, list):
        type_name = filter_data.__class__.__name__
        raise FormDataError(f'Filter data should be a list, got {type_name} instead')

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


def get_mongo_client():
    global _mongo_client
    mongo_secrets = SecretProvider.get_instance().mongo_secrets

    if _mongo_client and not mongo_secrets.changed:
        return _mongo_client

    if _mongo_client and mongo_secrets.changed:
        logging.info('MongoDB credentials are changed, reconnect')
        _mongo_client.close()

    mongo_secrets = SecretProvider.get_instance().mongo_secrets
    mongo_options = dict(
        username=mongo_secrets.user,
        password=mongo_secrets.password,
        connectTimeoutMS=CONNECT_TIMEOUT,
        connect=True,
    )

    if mongo_secrets.replica_set:
        mongo_options['replicaSet'] = mongo_secrets.replica_set

    if mongo_secrets.auth_source:
        mongo_options['authSource'] = mongo_secrets.auth_source

    if mongo_secrets.ssl_cert:
        if not os.path.exists(mongo_secrets.ssl_cert):
            raise RuntimeError(f'SSL certificate file does not exist: {mongo_secrets.ssl_cert}')

        mongo_options.update({
            'tlsCAFile': mongo_secrets.ssl_cert,
        })

    logging.info('Connecting to MongoDB: %s:%s', mongo_secrets.host, mongo_secrets.port)
    _mongo_client = pymongo.MongoClient(mongo_secrets.host, mongo_secrets.port, **mongo_options)
    return _mongo_client


def get_sqs_client():
    global _sqs_client
    sqs_secrets = SecretProvider.get_instance().sqs_secrets

    if _sqs_client and not sqs_secrets.changed:
        return _sqs_client, sqs_secrets

    if sqs_secrets.changed:
        logging.info('SQS credentials are changed, rebuild client')

    _sqs_client = boto3.client(
        'sqs',
        endpoint_url=sqs_secrets.endpoint_url,
        region_name='ru-central1',
        aws_access_key_id=sqs_secrets.access_key,
        aws_secret_access_key=sqs_secrets.secret_key,
    )

    return _sqs_client, sqs_secrets


def get_mongo_db():
    mongo_secrets = SecretProvider.get_instance().mongo_secrets
    return get_mongo_client()[mongo_secrets.db_name]


def _get_sms_collection():
    mongo_secrets = SecretProvider.get_instance().mongo_secrets

    client = get_mongo_client()
    collection = client[mongo_secrets.db_name]['sms_items']

    collection.create_index([
        ('created', pymongo.ASCENDING),
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
    mongo_secrets = SecretProvider.get_instance().mongo_secrets

    client = get_mongo_client()
    collection = client[mongo_secrets.db_name]['filters']

    collection.create_index([
        ('login', pymongo.ASCENDING),
        ('created', pymongo.DESCENDING),
    ], background=True)

    return collection
