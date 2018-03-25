import flask
import json
import logging
import os
import sys
from functools import wraps

import data_handler
from flask import request

app = flask.Flask(__name__)

LOG_FORMAT = '%(asctime)s %(levelname)s\t%(message)s\t%(pathname)s:%(lineno)d %(funcName)s %(process)d %(threadName)s'
LOG_LEVEL = os.environ.get('LOG_LEVEL', logging.INFO)

correct_user_name = os.environ.get('SMS_USER_NAME', '').strip()
correct_user_key = os.environ.get('SMS_USER_KEY', '').strip()

if not correct_user_name or not correct_user_key:
    raise EnvironmentError('You should provide SMS_USER_NAME and SMS_USER_KEY')

logging.basicConfig(format=LOG_FORMAT, level=LOG_LEVEL)


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

        if not (auth.username == correct_user_name and auth.password == correct_user_key):
            logging.info(
                'Auth not working: %s != %s or %s != %s',
                auth.username, correct_user_name,
                auth.password, correct_user_key,
            )

            return create_json_response(
                {'error': 'Not authorized'}, status=403,
                headers={'WWW-Authenticate': 'Basic realm="SMS Login Required"'}
            )

        return func(*args, **kwargs)

    return decorated


@app.route('/get-sms')
@requires_auth
def get_sms():
    device_id = request.args.get('device_id', '').strip()
    limit = request.args.get('limit')

    if limit:
        try:
            limit = int(limit)
        except ValueError:
            return create_json_response([{'error': 'Incorrect limit'}], status=400)

    try:
        result = data_handler.get_sms(device_id, limit)
    except data_handler.FormDataError as e:
        logging.info('Client error: %s', e)
        return create_json_response([{'error': e.message}], status=400)

    return create_json_response(result)


@app.route('/add-sms', methods=['POST'])
@requires_auth
def add_sms():
    try:
        data_handler.add_sms(request.form)
        return create_json_response([{'status': 'OK'}])
    except data_handler.FormDataError as e:
        logging.info('Client error: %s', e)
        return create_json_response([{'error': e.message}], status=400)


@app.errorhandler(404)
def error_404(*args):
    return create_json_response([{'error': 'Not found'}], status=404)


@app.errorhandler(405)
def error_405(*args):
    return create_json_response([{'error': 'Method is not allowed'}], status=405)


def create_json_response(data, status=200, headers=None):
    if headers is None:
        headers = {}

    resp = flask.make_response(json.dumps(data, ensure_ascii=False), status)
    resp.headers['content-type'] = 'application/json; charset=utf-8'
    for name, val in headers.iteritems():
        resp.headers[name] = val

    return resp
