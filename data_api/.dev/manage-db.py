import argparse
import json
import logging
import os

import pymongo

mongo_host = os.environ.get('MONGO_HOST', 'localhost')
mongo_port = int(os.environ.get('MONGO_PORT', 27017))
mongo_login = os.environ.get('MONGO_LOGIN')
mongo_password = os.environ.get('MONGO_PASSWORD')
mongo_db_name = os.environ.get('MONGO_DB_NAME', 'sms487_test')

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

    logging.info('Setting up DB %s', mongo_db_name)

    fixture_file = os.path.join(os.path.dirname(__file__), 'fixture.json')
    with open(fixture_file) as fd:
        fixture_data = json.load(fd)

    db = get_mongo_db()

    for collection_name, data in fixture_data.items():
        logging.info('Setting up collection %s', collection_name)

        collection = db[collection_name]
        collection.insert_many(data)


def tear_down():
    logging.info('Tearing down DB %s', mongo_db_name)

    client = get_mongo_client()
    client.drop_database(mongo_db_name)


def get_mongo_db():
    mongo_client = get_mongo_client()
    return mongo_client[mongo_db_name]


def get_mongo_client():
    return pymongo.MongoClient(
        mongo_host, mongo_port,
        connect=True,
        username=mongo_login,
        password=mongo_password,
    )


if __name__ == '__main__':
    main()
