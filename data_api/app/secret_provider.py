import dataclasses
import logging
import os
from dataclasses import dataclass
from functools import cached_property
from typing import Dict

import requests
from cached_property import cached_property_with_ttl

LOCKBOX_SECRET_URL = 'https://payload.lockbox.api.cloud.yandex.net/lockbox/v1/secrets'
DEFAULT_DB_NAME = 'sms487'
SECRET_TTL = 90

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
    changed: bool = False

    def validate(self):
        check_fields_non_empty = ('host', 'port', 'db_name')

        empty_fields = []
        for name in check_fields_non_empty:
            if not getattr(self, name):
                empty_fields.append(name)

        if empty_fields:
            raise RuntimeError(f'This secrets are required: {", ".join(empty_fields)}')

        return self


@dataclass
class SqsSecrets:
    queue_url: str
    access_key: str
    secret_key: str
    endpoint_url: str = 'https://message-queue.api.cloud.yandex.net'
    changed: bool = False


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

    @cached_property
    def sqs_secrets(self):
        raise NotImplementedError()


class DevSecretProvider(SecretProvider):
    @property
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

    @property
    def sqs_secrets(self):
        return SqsSecrets(
            queue_url=os.getenv('SQS_QUEUE'),
            access_key=os.getenv('SQS_ACCESS_KEY'),
            secret_key=os.getenv('SQS_SECRET_KEY'),
        )


class YcSecretProvider(SecretProvider):
    mongo_secret_id = 'e6q502o8uulleoq6jnpg'
    sqs_secret_id = 'e6qq93te4b88t6qv2ak0'
    not_exist = object()

    _prev_mongo_secrets = None
    _prev_sqs_secrets = None

    @cached_property_with_ttl(ttl=SECRET_TTL)
    def mongo_secrets(self):
        sec_data = self._request_lockbox(self.mongo_secret_id)
        required_fields = ('host', 'port', 'ssl_cert', 'user', 'password')
        for name in required_fields:
            val = sec_data.get(name, self.not_exist)
            if val is self.not_exist:
                raise RuntimeError(f'Required field not found in secret data: {name}')

        result = MongoSecrets(**sec_data).validate()

        changed = result != self._prev_mongo_secrets
        self._prev_mongo_secrets = dataclasses.replace(result)
        result.changed = changed

        log_data = {k: v for k, v in vars(result).items() if k != 'password'}
        logging.info('MongoDB params: %s', log_data)

        return result

    @cached_property_with_ttl(ttl=SECRET_TTL)
    def sqs_secrets(self):
        sec_data = self._request_lockbox(self.sqs_secret_id)
        required_fields = ('access-key', 'secret-key', 'prod-queue')
        for name in required_fields:
            val = sec_data.get(name, self.not_exist)
            if val is self.not_exist:
                raise RuntimeError(f'Required field not found in secret data: {name}')

        result = SqsSecrets(
            access_key=sec_data['access-key'],
            secret_key=sec_data['secret-key'],
            queue_url=sec_data['prod-queue'],
        )

        changed = result != self._prev_sqs_secrets
        self._prev_sqs_secrets = dataclasses.replace(result)
        result.changed = changed

        return result

    @cached_property_with_ttl(ttl=SECRET_TTL)
    def iam_token(self):
        url = f'http://{meta_service}/computeMetadata/v1/instance/service-accounts/default/token'
        resp = requests.get(url, headers={'Metadata-Flavor': 'Google'})
        resp.raise_for_status()

        resp_data = resp.json()
        token = resp_data.get('access_token')
        if not token:
            raise RuntimeError(f'There is no access token in result: {resp_data}')

        return token

    def _request_lockbox(self, sec_id: str) -> Dict:
        url = f'{LOCKBOX_SECRET_URL}/{sec_id}/payload'
        resp = requests.get(url, headers={'Authorization': f'Bearer {self.iam_token}'})
        resp.raise_for_status()

        resp_data = resp.json()
        entries = resp_data['entries']

        sec_data = {}
        for cur_data in entries:
            name = cur_data['key']
            val = cur_data['textValue']

            sec_data[name] = None
            if name == 'port':
                try:
                    sec_data[name] = int(val)
                except (ValueError, TypeError) as e:
                    logging.error(e)
            else:
                sec_data[name] = val

        return sec_data
