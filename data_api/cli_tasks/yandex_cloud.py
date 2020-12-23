import base64
import json
import contextlib
import logging
import subprocess
from urllib import request

LOCKBOX_HANDLER = 'https://payload.lockbox.api.cloud.yandex.net/lockbox/v1/secrets'

_yc = None
_iam_token = None


def get_secret(sec_id):
    logging.info('Get secret %s', sec_id)
    iam_token = get_iam_token()

    url = f'{LOCKBOX_HANDLER}/{sec_id}/payload'
    req = request.Request(url, headers={
        'Authorization': f'Bearer {iam_token}',
    })

    with contextlib.closing(request.urlopen(req)) as fp:
        res = json.load(fp)

    secret_data = {}
    for item in res['entries']:
        if 'binaryValue' in item:
            val = base64.b64decode(item['binaryValue'])
        else:
            val = item['textValue'].encode('utf-8')

        secret_data[item['key']] = val

    return secret_data


def get_iam_token():
    global _iam_token
    if _iam_token:
        return _iam_token

    logging.info('Get IAM token')

    b_token = subprocess.check_output((get_yc(), 'iam', 'create-token', '--no-user-output'))
    _iam_token = str(b_token.strip(), 'utf-8')

    return _iam_token


def get_yc():
    global _yc
    if _yc:
        return _yc

    logging.info('Get Yandex Cloud tool')
    try:
        _yc = str(subprocess.check_output(('which', 'yc')).strip(), 'utf-8')
    except subprocess.CalledProcessError:
        logging.warning('Try to install and setup yc: https://clck.ru/Sak4W')
        raise

    return _yc
