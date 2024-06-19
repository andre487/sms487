import logging
import os
from . import common, yandex_cloud

SECRETS = {
    'sqs': 'e6qq93te4b88t6qv2ak0',
}


def run(c):
    common.prepare_virtual_env(c)

    for secret_name, secret_id in SECRETS.items():
        secret_data = yandex_cloud.get_secret(secret_id)
        write_secret_files(secret_name, secret_data)


def get_secret_dir():
    d = common.SECRET_DIR
    if os.path.exists(d):
        return d

    os.makedirs(d, mode=0o700)
    return d


def endure_secret_subdir(file_path):
    d = os.path.dirname(file_path)
    if os.path.exists(d):
        return file_path

    os.makedirs(d, mode=0o700)
    return file_path


def write_secret_files(secret_name, secret_data):
    for file_name, content in secret_data.items():
        logging.info('Write %s / %s', secret_name, file_name)

        file_path = os.path.join(get_secret_dir(), secret_name, file_name)
        with open(endure_secret_subdir(file_path), 'wb') as fp:
            fp.write(content)
