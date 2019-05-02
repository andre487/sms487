import argparse
import json
import logging
import os

import pymongo

MONGO_HOST = os.environ.get('MONGO_HOST', 'localhost')
MONGO_PORT = int(os.environ.get('MONGO_PORT', 27017))
MONGO_LOGIN = os.environ.get('MONGO_LOGIN')
MONGO_PASSWORD = os.environ.get('MONGO_PASSWORD')
MONGO_DB_NAME = os.environ.get('MONGO_DB_NAME', 'sms487_test')
AUTH_MONGO_DB_NAME = os.environ.get('AUTH_MONGO_DB_NAME', 'sms487_test')

logging.basicConfig(level=logging.INFO)


def main():
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('action', choices=('setup', 'tear-down'))

    args = arg_parser.parse_args()

    if args.action == 'setup':
        setup()
    elif args.action == 'tear-down':
        tear_down()


def setup():
    tear_down()

    logging.info('Setting up DB %s', MONGO_DB_NAME)

    fixture_file = os.path.join(os.path.dirname(__file__), 'fixture.json')
    with open(fixture_file) as fd:
        fixture_data = json.load(fd)

    db = get_mongo_db()

    for collection_name, data in fixture_data.items():
        logging.info('Setting up collection %s', collection_name)

        collection = db[collection_name]
        collection.insert_many(data)


def tear_down():
    logging.info('Tearing down DB %s', MONGO_DB_NAME)

    client = get_mongo_client()
    client.drop_database(MONGO_DB_NAME)
    client.drop_database(AUTH_MONGO_DB_NAME)


def get_mongo_db():
    mongo_client = get_mongo_client()
    return mongo_client[MONGO_DB_NAME]


def get_mongo_client():
    return pymongo.MongoClient(
        MONGO_HOST, MONGO_PORT,
        connect=True,
        username=MONGO_LOGIN,
        password=MONGO_PASSWORD,
    )


if __name__ == '__main__':
    main()
