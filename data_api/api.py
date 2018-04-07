import flask
import hashlib
import json
import logging
import os
import sys
from functools import wraps

import data_handler
from flask import request

if sys.version_info[0] != 3:
    raise EnvironmentError('Use Python 3')

app = flask.Flask(__name__)

# noinspection SpellCheckingInspection
LOG_FORMAT = '%(asctime)s %(levelname)s\t%(message)s\t%(pathname)s:%(lineno)d %(funcName)s %(process)d %(threadName)s'
LOG_LEVEL = os.environ.get('LOG_LEVEL', logging.INFO)

correct_user_name = os.environ.get('SMS_USER_NAME', '').strip()
correct_user_key = os.environ.get('SMS_USER_KEY', '').strip()

if not correct_user_name or not correct_user_key:
    raise EnvironmentError('You should provide SMS_USER_NAME and SMS_USER_KEY')

logging.basicConfig(format=LOG_FORMAT, level=LOG_LEVEL)


def protected_from_brute_force(func):
    @wraps(func)
    def decorated(*args, **kwargs):
        remote_addr = request.remote_addr
        if not data_handler.is_remote_addr_clean(remote_addr):
            logging.info('Addr %s is not clean, so ban', remote_addr)
            data_handler.mark_auth_mistake(remote_addr)
            return create_json_response([{'error': 'Banned'}], status=403)

        result = func(*args, **kwargs)
        if result.status.startswith('401') or result.status.startswith('403'):
            logging.info('Addr %s has auth mistake: %s', remote_addr, result.status)
            data_handler.mark_auth_mistake(remote_addr)

        return result

    return decorated


def requires_auth(func):
    @wraps(func)
    def decorated(*args, **kwargs):
        auth = request.authorization
        if not auth:
            logging.info('No auth')

            return create_json_response(
                {'error': 'Not authorized'}, status=401,
                headers={'WWW-Authenticate': 'Basic realm="SMS Login Required"'}
            )

        user_key_hash = create_user_key_hash(auth.password)
        if not (auth.username == correct_user_name and user_key_hash == correct_user_key):
            return create_json_response(
                {'error': 'Not authorized'}, status=403,
                headers={'WWW-Authenticate': 'Basic realm="SMS Login Required"'}
            )

        return func(*args, **kwargs)

    return decorated


def create_user_key_hash(current_user_key):
    h = hashlib.sha256()
    h.update(current_user_key.encode())
    return h.hexdigest()


@app.route('/')
@protected_from_brute_force
@requires_auth
def index():
    device_id = request.args.get('device_id', '').strip()
    limit = request.args.get('limit', '30')

    if device_id == 'All':
        device_id = None

    if limit:
        try:
            limit = int(limit)
        except ValueError:
            return create_html_response('error.html', {'code': 400, 'message': 'Incorrect limit'}, status=400)

    try:
        result = data_handler.get_sms(device_id, limit)
    except data_handler.FormDataError as e:
        logging.info('Client error: %s', e)
        return create_html_response('error.html', {'code': 400, 'message': str(e)}, status=400)

    device_ids = [('All', not device_id)] + [
        (name, name == device_id) for name in data_handler.get_device_ids()
    ]

    return create_html_response('index.html', {
        'messages': result, 'limit': limit,
        'device_id': device_id, 'device_ids': device_ids,
    })


@app.route('/get-sms')
@protected_from_brute_force
@requires_auth
def get_sms():
    device_id = request.args.get('device_id', '').strip()
    limit = request.args.get('limit', '30')

    if limit:
        try:
            limit = int(limit)
        except ValueError:
            return create_json_response([{'error': 'Incorrect limit'}], status=400)

    try:
        result = data_handler.get_sms(device_id, limit)
    except data_handler.FormDataError as e:
        logging.info('Client error: %s', e)
        return create_json_response([{'error': str(e)}], status=400)

    return create_json_response(result)


@app.route('/add-sms', methods=['POST'])
@protected_from_brute_force
@requires_auth
def add_sms():
    try:
        data_handler.add_sms(request.form)
        return create_json_response([{'status': 'OK'}])
    except data_handler.FormDataError as e:
        logging.info('Client error: %s', e)
        return create_json_response([{'error': str(e)}], status=400)


@app.route('/get-banned-addresses')
@protected_from_brute_force
@requires_auth
def get_banned_addresses():
    result = data_handler.get_banned_addresses()
    return create_json_response(result)


# noinspection PyUnusedLocal
@app.errorhandler(404)
def error_404(*args):
    return create_json_response([{'error': 'Not found'}], status=404)


# noinspection PyUnusedLocal
@app.errorhandler(405)
def error_405(*args):
    return create_json_response([{'error': 'Method is not allowed'}], status=405)


def create_json_response(data, status=200, headers=None):
    if headers is None:
        headers = {}

    resp = flask.make_response(json.dumps(data, ensure_ascii=False), status)
    resp.headers['content-type'] = 'application/json; charset=utf-8'
    for name, val in headers.items():
        resp.headers[name] = val

    return resp


def create_html_response(template_name, data, status=200, headers=None):
    if headers is None:
        headers = {}

    html = flask.render_template(template_name, **data)

    resp = flask.make_response(html, status)
    resp.headers['content-type'] = 'text/html; charset=utf-8'

    for name, val in headers.items():
        resp.headers[name] = val

    return resp
