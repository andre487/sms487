import logging
import os
from dataclasses import dataclass
from functools import cached_property

import requests

LOCKBOX_SECRET_URL = 'https://payload.lockbox.api.cloud.yandex.net/lockbox/v1/secrets'
DEFAULT_DB_NAME = 'sms487'

deploy_type = os.getenv('DEPLOY_TYPE', 'not-set')
meta_service = os.getenv('YC_METADATA_SERVICE', '169.254.169.254')


@dataclass
class MongoSecrets:
    host: str
    port: int
    user: str
    password: str
    auth_source: str
    db_name: str
    ssl_cert: str = None
    replica_set: str = None

    def validate(self):
        check_fields_non_empty = ('host', 'port', 'db_name')

        empty_fields = []
        for name in check_fields_non_empty:
            if not getattr(self, name):
                empty_fields.append(name)

        if empty_fields:
            raise RuntimeError(f'This secrets are required: {", ".join(empty_fields)}')

        return self


class SecretProvider:
    _instance = None

    @classmethod
    def get_instance(cls):
        if cls._instance is None:
            cls._instance = cls._create_instance_by_deploy()
        return cls._instance

    @classmethod
    def _create_instance_by_deploy(cls):
        logging.info('Using secret configuration for %s', deploy_type)

        if deploy_type == 'yc':
            return YcSecretProvider()

        if deploy_type == 'dev':
            return DevSecretProvider()

        raise RuntimeError(f'Unknown deploy type: {deploy_type}')

    @cached_property
    def mongo_secrets(self):
        raise NotImplementedError()


class DevSecretProvider(SecretProvider):
    @cached_property
    def mongo_secrets(self):
        return MongoSecrets(
            host=os.getenv('MONGO_HOST', 'localhost'),
            port=int(os.getenv('MONGO_PORT', '27017')),
            replica_set=os.getenv('MONGO_REPLICA_SET'),
            ssl_cert=os.getenv('MONGO_SSL_CERT'),
            user=os.getenv('MONGO_USER'),
            password=os.getenv('MONGO_PASSWORD'),
            auth_source=os.getenv('MONGO_AUTH_SOURCE'),
            db_name=os.getenv('MONGO_DB_NAME', DEFAULT_DB_NAME),
        ).validate()


class YcSecretProvider(SecretProvider):
    mongo_secret_id = 'e6qeb5qh931hk2nt5d7l'
    not_exist = object()

    @cached_property
    def mongo_secrets(self):
        url = f'{LOCKBOX_SECRET_URL}/{self.mongo_secret_id}/payload'
        resp = requests.get(url, headers={'Authorization': f'Bearer {self.iam_token}'})
        resp.raise_for_status()

        resp_data = resp.json()
        entries = resp_data['entries']

        mongo_data = {}
        for cur_data in entries:
            name = cur_data['key']
            val = cur_data['textValue']

            mongo_data[name] = None
            if name == 'port':
                try:
                    mongo_data[name] = int(val)
                except (ValueError, TypeError) as e:
                    logging.error(e)
            else:
                mongo_data[name] = val

        required_fields = ('host', 'port', 'ssl_cert', 'user', 'password', 'auth_source')
        for name in required_fields:
            val = mongo_data.get(name, self.not_exist)
            if val is self.not_exist:
                raise RuntimeError(f'Required field not found in secret data: {name}')

        result = MongoSecrets(
            db_name=os.getenv('MONGO_DB_NAME', DEFAULT_DB_NAME),
            **mongo_data
        ).validate()

        log_data = vars(result)
        log_data.pop('password')
        logging.info('MongoDB params: %s', log_data)

        return result

    @cached_property
    def iam_token(self):
        url = f'http://{meta_service}/computeMetadata/v1/instance/service-accounts/default/token'
        resp = requests.get(url, headers={'Metadata-Flavor': 'Google'})
        resp.raise_for_status()

        resp_data = resp.json()
        token = resp_data.get('access_token')
        if not token:
            raise RuntimeError(f'There is no access token in result: {resp_data}')

        return token
