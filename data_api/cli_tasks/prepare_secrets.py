import logging
import os
from . import common, yandex_cloud

SECRETS = {
    'mongo': 'e6q0nap2vdn3v6u9r7h8',
}


def run(c, rebuild_venv, no_secret_cache=False):
    common.prepare_virtual_env(c, rebuild_venv)

    if not no_secret_cache and os.path.exists(common.SECRET_DIR):
        logging.info('Has secret data, use --no-secret-cache to renew')
        return

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
