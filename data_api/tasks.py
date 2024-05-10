import json
import logging
import sys
import urllib.parse
import urllib.request
from datetime import datetime

from invoke import task

import cli_tasks
from cli_tasks import common


@task
def run_dev(c, port=8080, clear_db=False):
    """Run Flask dev server"""
    cli_tasks.run_dev.run(c, port, clear_db)


@task
def lint(c):
    """Run flake8"""
    cli_tasks.run_linters.run(c)


@task
def install(c, packages=''):
    """Install packages: invoke install --packages='flask pytest'"""
    cli_tasks.install.run(c, packages)


@task
def http_test(c, k=None):
    """Run HTTP handlers test on dev instance"""
    cli_tasks.http_test.run(c, test_filter=k)


@task
def docker_build(c):
    """Build app Docker image"""
    cli_tasks.docker_build.run(c)


@task
def docker_push(c):
    """Push app Docker image to registry"""
    cli_tasks.docker_push.run(c)


@task
def docker_run(c, port=8181, rebuild=False):
    """Run app in Docker container"""
    if rebuild:
        cli_tasks.docker_build.run(c)
    cli_tasks.docker_run.run(c, port)


@task
def docker_test(c, rebuild=False):
    """Run HTTP handlers test on Docker instance"""
    if rebuild:
        cli_tasks.docker_build.run(c)
    cli_tasks.docker_test.run(c)


@task
def prepare_secrets(c):
    """Prepare secrets for production"""
    cli_tasks.prepare_secrets.run(c)


@task
def create_local_venv(c, rebuild_venv=True):
    """Prepare .venv dir for using in IDE"""
    common.prepare_virtual_env(c, rebuild_venv)


@task
def make_deploy(c):
    """Deploy current work dir to production"""
    tag = datetime.utcnow().strftime('t%Y%m%d_%H%M%S')

    cli_tasks.run_linters.run(c)
    cli_tasks.prepare_secrets.run(c)

    cli_tasks.docker_build.run(c, tag=tag)
    cli_tasks.docker_test.run(c, tag=tag)
    cli_tasks.docker_push.run(c, tag=tag)

    c.run(f'{common.PROJECT_DIR}/deploy/yandex_cloud/update_container.sh {tag}')

    try:
        c.run('docker-clean')
    except Exception as e:
        logging.warning(e)


@task
def create_dev_auth_token(_):
    print(common.create_dev_auth_token())


@task
def add_dev_sms(_, port=8080):
    auth_token = common.create_dev_auth_token()
    req = urllib.request.Request(
        method='POST',
        url=f'http://127.0.0.1:{port}/add-sms',
        headers={
            'Content-Type': 'application/x-www-form-urlencoded',
            'Cookie': f'Dev-Auth-Token={auth_token}',
        },
        data=urllib.parse.urlencode({
            'message_type': 'sms',
            'device_id': 'test',
            'tel': '01',
            'date_time': '2020-01-01 20:00:00',
            'sms_date_time': '2020-01-01 20:00:00',
            'text': LOREM_IPSUM,
        }).encode('utf8'),
    )

    try:
        with urllib.request.urlopen(req) as resp:
            print(resp.read().decode('utf8'))
    except urllib.request.HTTPError as e:
        print('Error:', e)
        print(e.read().decode('utf8'))
        sys.exit(1)


@task
def add_dev_sms_batch(_, port=8080):
    auth_token = common.create_dev_auth_token()
    req = urllib.request.Request(
        method='POST',
        url=f'http://127.0.0.1:{port}/add-sms',
        headers={
            'Content-Type': 'application/json',
            'Cookie': f'Dev-Auth-Token={auth_token}',
        },
        data=json.dumps([
            {
                "message_type": "sms",
                "device_id": "test",
                "tel": "01",
                "date_time": "2020-01-01 00:00",
                "sms_date_time": "2020-01-01 00:00",
                "text": "SMS test"
            },
            {
                "message_type": "notification",
                "device_id": "test",
                "tel": "01",
                "date_time": "2020-01-01 00:00",
                "sms_date_time": "2020-01-01 00:00",
                "text": "Notification test"
            },
        ]).encode('utf8'),
    )

    try:
        with urllib.request.urlopen(req) as resp:
            print(resp.read().decode('utf8'))
    except urllib.request.HTTPError as e:
        print('Error:', e)
        print(e.read().decode('utf8'))
        sys.exit(1)


# flake8: noqa: W291
# noinspection SpellCheckingInspection
LOREM_IPSUM = """
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin in mollis ipsum. Proin id ornare
 turpis, vitae accumsan est. Nunc lobortis non leo at hendrerit. Nullam nunc mauris, accumsan sollicitudin mauris sed,
 efficitur aliquam enim.
""".strip()


@task
def send_sms_to_sqs_test(
    _,
    device_id='TestDevice',
    tel='000',
    message_type='sms',
    printable_message_type='SMS',
    text=LOREM_IPSUM,
):
    sqs_test_queue = common.get_sqs_params()[1]
    send_sqs_message(sqs_test_queue, device_id, message_type, printable_message_type, tel, text)


@task
def send_sms_to_sqs_prod(
    _,
    device_id='TestDevice',
    tel='000',
    message_type='sms',
    printable_message_type='SMS',
    text=LOREM_IPSUM,
):
    sqs_prod_queue = common.get_sqs_params()[0]
    send_sqs_message(sqs_prod_queue, device_id, message_type, printable_message_type, tel, text)


def send_sqs_message(queue_url, device_id, message_type, printable_message_type, tel, text):
    _, _, sqs_access_key, sqs_secret_key = common.get_sqs_params()
    sqs_client = common.create_yandex_sqs_client(sqs_access_key, sqs_secret_key)
    dt = common.get_fmt_date()
    res = sqs_client.send_message(
        QueueUrl=queue_url,
        MessageGroupId='0',
        MessageBody=json.dumps({
            'type': 'new_messages',
            'data': [{
                'device_id': device_id,
                'tel': tel,
                'message_type': message_type,
                'printable_message_type': printable_message_type,
                'date_time': dt,
                'sms_date_time': dt,
                'printable_date_time': dt,
                'text': text,
            }],
        }),
    )
    logging.info(f'Message to SQS sent: {res}')
