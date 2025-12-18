import sys
from datetime import datetime
from pprint import pprint

import click
import requests


@click.command()
@click.option('--addr', default='http://localhost:8080')
@click.option('--user', default='user')
@click.option('--password', default='password')
@click.option('--device-id', default='TestDevice')
@click.option('--message-type', default='sms')
@click.option('--date-time', default=datetime.now().strftime('%Y-%m-%dT%H:%M:%S'))
@click.option('--tel', default='000')
@click.option('--text', default='Hello world!')
def main(
    addr: str,
    user: str,
    password: str,
    device_id: str,
    message_type: str,
    date_time: str,
    tel: str,
    text: str,
):
    resp = requests.post(
        addr + '/add-sms',
        auth=(user, password),
        json=[
            {
                'device_id': device_id,
                'message_type': message_type,
                'date_time': date_time,
                'tel': tel,
                'text': text,
            }
        ],
    )
    resp.raise_for_status()

    pprint(dict(resp.headers), stream=sys.stderr)
    print(resp.text)


if __name__ == '__main__':
    main()
