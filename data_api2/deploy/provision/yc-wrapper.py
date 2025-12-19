#!/usr/bin/env python3
import functools
import json
import os
import sys
from urllib.request import Request, urlopen

SEC_CONFIG = (
    ('SQS_ACCESS_KEY', 'e6qq93te4b88t6qv2ak0', 'access-key'),
    ('SQS_SECRET_KEY', 'e6qq93te4b88t6qv2ak0', 'secret-key'),
    ('SQS_QUEUE_URL', 'e6qq93te4b88t6qv2ak0', 'prod-queue'),
    ('HTTP_USER', 'e6qq93te4b88t6qv2ak0', 'api2-http-user'),
    ('HTTP_PASSWORD', 'e6qq93te4b88t6qv2ak0', 'api2-http-password'),
)


def main():
    if len(sys.argv) < 2:
        raise Exception('No EXE path')

    exe_path = sys.argv[1]
    if not os.path.exists(exe_path):
        raise Exception('EXE not found')

    env = os.environ.copy()
    env.update({
        'LISTEN_ADDR': '127.0.0.1',
        'LISTEN_PORT': '8080',
        'TIME_ZONE': 'Europe/Moscow',
    })
    for env_name, sec_id, field_key in SEC_CONFIG:
        env[env_name] = get_lb_secret_field(sec_id, field_key)

    os.execve(exe_path, [exe_path], env)


@functools.lru_cache
def get_iam_token():
    meta_host = os.getenv('YC_METADATA_SERVICE', '169.254.169.254')
    iam_url = f'http://{meta_host}/computeMetadata/v1/instance/service-accounts/default/token'
    req = Request(
        method='GET',
        url=iam_url,
        headers={'Metadata-Flavor': 'Google'},
    )
    with urlopen(req, timeout=10) as resp:
        if resp.status != 200:
            raise Exception(f'IAM HTTP Error {resp.status}')
        body = resp.read()
    data = json.loads(body)

    access_token = data.get('access_token')
    if not access_token:
        raise Exception('No access_token')
    return access_token


@functools.lru_cache(maxsize=128)
def get_lb_secret(sec_id):
    lb_url = f'https://payload.lockbox.api.cloud.yandex.net/lockbox/v1/secrets/{sec_id}/payload'
    req = Request(
        method='GET',
        url=lb_url,
        headers={'Authorization': f'Bearer {get_iam_token()}'},
    )
    with urlopen(req, timeout=10) as resp:
        if resp.status != 200:
            raise Exception(f'LB HTTP Error {resp.status}')
        body = resp.read()
    return json.loads(body)


def get_lb_secret_field(sec_id, field_key):
    data = get_lb_secret(sec_id)
    for item in data['entries']:
        if item['key'] == field_key:
            return item['textValue']
    raise Exception(f'Key {field_key} not fount in {sec_id}')


if __name__ == '__main__':
    main()
