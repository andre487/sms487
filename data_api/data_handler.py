import logging
import os
import re
from datetime import datetime, timedelta, timezone

import pymongo

CONNECT_TIMEOUT = 2000
AUTH_MISTAKES_TO_BAN = int(os.environ.get('AUTH_MISTAKES_TO_BAN', 3))
AUTH_BAN_TIME = int(os.environ.get('AUTH_BAN_TIME', 86400))
TZ_OFFSET = int(os.environ.get('TZ_OFFSET', 3))

mongo_host = os.environ.get('MONGO_HOST', 'localhost')
mongo_port = int(os.environ.get('MONGO_PORT', 27017))
mongo_login = os.environ.get('MONGO_LOGIN')
mongo_password = os.environ.get('MONGO_PASSWORD')
mongo_db_name = os.environ.get('MONGO_DB_NAME', 'sms487')

date_time_pattern = re.compile(r'^(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}(?::\d{2})?)(?:\s[+-]\d+)?$')
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
    device_id = data.get('device_id', '').strip()
    tel = data.get('tel', '').strip()
    date_time = data.get('date_time', '').strip()
    text = data.get('text', '').strip()

    if not device_id:
        raise FormDataError('There is no device ID')

    if not tel:
        raise FormDataError('There is no tel')

    if not date_time:
        raise FormDataError('There is no date_time')

    if not date_time_pattern.match(date_time):
        raise FormDataError('date_time is incorrect')

    if not text:
        raise FormDataError('There is no text')

    _get_sms_collection().insert({
        'device_id': device_id,
        'tel': tel,
        'date_time': date_time,
        'text': text,
    })


def dress_sms_doc(doc):
    result = {}

    for n, v in doc.items():
        if not n.startswith('_'):
            result[n] = v

    if 'date_time' in result:
        date_time = datetime.strptime(result['date_time'], '%Y-%m-%d %H:%M:%S %z')
        result['date_time'] = date_time.astimezone(time_zone).strftime('%d %b %Y %H:%M')

    return result


def get_device_ids():
    device_ids = _get_sms_collection().distinct('device_id')
    device_ids.sort()
    return device_ids


def is_remote_addr_clean(remote_addr):
    collection = _get_remote_addr_collection()

    addr_data = collection.find_one({'remote_addr': remote_addr})
    if addr_data is None:
        return True

    if addr_data['mistakes'] >= AUTH_MISTAKES_TO_BAN:
        return False

    return True


def mark_auth_mistake(remote_addr):
    collection = _get_remote_addr_collection()
    result = collection.update({'remote_addr': remote_addr}, {'$inc': {'mistakes': 1}})

    if not result['updatedExisting']:
        collection.insert({'remote_addr': remote_addr, 'mistakes': 1})


def get_banned_addresses():
    collection = _get_remote_addr_collection()

    return [doc['remote_addr'] for doc in collection.find({})]


def _get_mongo_client():
    global _mongo_client
    if _mongo_client:
        return _mongo_client

    logging.info('Connecting to MongoDB: %s:%s', mongo_host, mongo_port)

    _mongo_client = pymongo.MongoClient(
        mongo_host, mongo_port,
        connect=True,
        connectTimeoutMS=CONNECT_TIMEOUT,
        username=mongo_login,
        password=mongo_password,
    )

    return _mongo_client


def _get_sms_collection():
    client = _get_mongo_client()
    collection = client[mongo_db_name]['sms_items']

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


def _get_remote_addr_collection():
    client = _get_mongo_client()
    collection = client[mongo_db_name]['remote_addresses']

    collection.create_index([
        ('remote_addr', pymongo.ASCENDING),
    ], background=True, unique=True, expireAfterSeconds=AUTH_BAN_TIME)

    return collection
