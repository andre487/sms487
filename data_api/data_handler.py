import logging
import os
import re

import pymongo

CONNECT_TIMEOUT = 2000

mongo_host = os.environ.get('MONGO_HOST', 'localhost')
mongo_port = int(os.environ.get('MONGO_PORT', 27017))
mongo_login = os.environ.get('MONGO_LOGIN')
mongo_password = os.environ.get('MONGO_PASSWORD')

date_time_pattern = re.compile(r'^\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}(?:\s[+-]\d+)?$')

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

    return [dress_item(doc) for doc in cursor]

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


def dress_item(doc):
    result = {}
    for n, v in doc.iteritems():
        if not n.startswith('_'):
            result[n] = v
    return result


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
    collection = client['sms487']['sms_items']

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
