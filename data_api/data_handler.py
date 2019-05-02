import logging
import os
import re
from datetime import datetime, timedelta, timezone

import pymongo

CONNECT_TIMEOUT = 2000
TZ_OFFSET = int(os.environ.get('TZ_OFFSET', 3))

MONGO_HOST = os.environ.get('MONGO_HOST', 'localhost')
MONGO_PORT = int(os.environ.get('MONGO_PORT', 27017))
MONGO_LOGIN = os.environ.get('MONGO_LOGIN')
MONGO_PASSWORD = os.environ.get('MONGO_PASSWORD')
MONGO_DB_NAME = os.environ.get('MONGO_DB_NAME', 'sms487')

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
        try:
            date_time = datetime.strptime(result['date_time'], '%Y-%m-%d %H:%M:%S %z')
        except ValueError:
            date_time = datetime.strptime(result['date_time'], '%Y-%m-%d %H:%M %z')
        result['date_time'] = date_time.astimezone(time_zone).strftime('%d %b %Y %H:%M')

    return result


def get_device_ids():
    device_ids = _get_sms_collection().distinct('device_id')
    device_ids.sort()
    return device_ids


def _get_mongo_client():
    global _mongo_client
    if _mongo_client:
        return _mongo_client

    logging.info('Connecting to MongoDB: %s:%s', MONGO_HOST, MONGO_PORT)

    _mongo_client = pymongo.MongoClient(
        MONGO_HOST, MONGO_PORT,
        connect=True,
        connectTimeoutMS=CONNECT_TIMEOUT,
        username=MONGO_LOGIN,
        password=MONGO_PASSWORD,
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
