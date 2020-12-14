import logging
import os
import re
import ssl
from datetime import datetime, timedelta, timezone

import pymongo

CONNECT_TIMEOUT = 500
TZ_OFFSET = int(os.environ.get('TZ_OFFSET', 3))

MONGO_HOST = os.environ.get('MONGO_HOST', 'localhost')
MONGO_PORT = int(os.environ.get('MONGO_PORT', 27017))

MONGO_REPLICA_SET = os.environ.get('MONGO_REPLICA_SET')
MONGO_SSL_CERT = os.environ.get('MONGO_SSL_CERT')

MONGO_USER = os.environ.get('MONGO_USER')
MONGO_PASSWORD = os.environ.get('MONGO_PASSWORD')
MONGO_AUTH_SOURCE = os.environ.get('MONGO_AUTH_SOURCE')
MONGO_DB_NAME = os.environ.get('MONGO_DB_NAME', 'sms487')

date_time_pattern = re.compile(r'^(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}(?::\d{2})?)(?:\s[+-]\d+)?$')
short_date_time_pattern = re.compile(r'^(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}).*')
message_type_pattern = re.compile(r'^\w{3,32}$')
time_zone = timezone(offset=timedelta(hours=TZ_OFFSET))

_mongo_client = None


class FormDataError(Exception):
    pass


def get_sms(device_id, limit=None):
    params = {}
    if device_id:
        params['device_id'] = device_id

    if not limit:
        limit = 256

    cursor = _get_sms_collection().find(params).sort(
        [('date_time', pymongo.DESCENDING), ('device_id', pymongo.ASCENDING)]
    ).limit(limit)

    return [dress_sms_doc(doc) for doc in cursor]


def add_sms(data):
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

    if not text:
        raise FormDataError('There is no text')

    if len(text) > 1024:
        raise FormDataError('Text is too long')

    _get_sms_collection().insert({
        'message_type': message_type,
        'device_id': device_id,
        'tel': tel,
        'date_time': date_time,
        'sms_date_time': sms_date_time,
        'text': text,
    })


def dress_sms_doc(doc):
    result = {}

    for n, v in doc.items():
        if not n.startswith('_'):
            result[n] = v

    result['printable_message_type'] = 'SMS'
    if result.get('message_type') == 'notification':
        result['printable_message_type'] = 'Notification'

    if 'date_time' in result:
        result['date_time'] = format_date_time(result['date_time'])

    if 'sms_date_time' in result:
        result['sms_date_time'] = format_date_time(result['sms_date_time'])

    if 'date_time' in result or 'sms_date_time' in result:
        date_time = str(result.get('date_time', ''))
        sms_date_time = str(result.get('sms_date_time', ''))

        result['printable_date_time'] = 'Undefined'
        if date_time:
            result['printable_date_time'] = date_time

        if sms_date_time and date_time != sms_date_time:
            result['printable_date_time'] += ' (%s)' % sms_date_time

    return result


def format_date_time(dt):
    match = short_date_time_pattern.match(dt)
    if not match:
        return dt

    date_time = datetime.strptime(match.group(1), '%Y-%m-%d %H:%M')
    return date_time.astimezone(time_zone).strftime('%d %b %Y %H:%M')


def get_device_ids():
    device_ids = _get_sms_collection().distinct('device_id')
    device_ids.sort()
    return device_ids


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
        ('device_id', pymongo.ASCENDING),
    ], background=True)

    collection.create_index([
        ('tel', pymongo.ASCENDING),
    ], background=True)

    collection.create_index([
        ('date_time', pymongo.DESCENDING),
    ], background=True)

    collection.create_index([
        ('date_time', pymongo.DESCENDING),
        ('device_id', pymongo.ASCENDING),
    ], background=True)

    return collection
