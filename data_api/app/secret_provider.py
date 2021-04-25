import os
from dataclasses import dataclass

deploy_type = os.getenv('DEPLOY_TYPE', 'not-set')
meta_service = os.getenv('YC_METADATA_SERVICE', '169.254.169.254')


@dataclass
class MongoSecrets:
    host: str
    port: int
    replica_set: str
    ssl_cert: str
    user: str
    password: str
    auth_source: str
    db_name: str

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
        if deploy_type == 'dev':
            return DevSecretProvider()

        raise RuntimeError(f'Unknown deploy type: {deploy_type}')

    @property
    def mongo_secrets(self):
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
            db_name=os.getenv('MONGO_DB_NAME'),
        ).validate()
