import flask
import json
import logging
import os
import sys

from app import data_handler, templating
from auth487 import flask as ath, common as acm
from flask import request

if sys.version_info[0] != 3:
    raise EnvironmentError('Use Python 3')

app = flask.Flask(__name__)

# noinspection SpellCheckingInspection
LOG_FORMAT = '%(asctime)s %(levelname)s\t%(message)s\t%(pathname)s:%(lineno)d %(funcName)s %(process)d %(threadName)s'
LOG_LEVEL = os.environ.get('LOG_LEVEL', logging.INFO)

logging.basicConfig(format=LOG_FORMAT, level=LOG_LEVEL)
templating.setup_filters(app)

ADDITIONAL_HEADERS = {
    'Content-Security-Policy': (
        "default-src 'none'; "
        "style-src 'self'; "
        "script-src 'self'; "
        "img-src 'self';"
    ),
    'X-Frame-Options': 'deny'
}


@app.route('/')
@ath.protected_from_brute_force
@ath.require_auth(access=['sms'])
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
@ath.protected_from_brute_force
@ath.require_auth(access=['sms'])
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
@ath.protected_from_brute_force
@ath.require_auth(no_redirect=True, access=['sms'])
def add_sms():
    try:
        data_handler.add_sms(request.form)
        return create_json_response([{'status': 'OK'}])
    except data_handler.FormDataError as e:
        logging.info('Client error: %s', e)
        return create_json_response([{'error': str(e)}], status=400)


@app.route('/robots.txt')
def robots_txt():
    return flask.Response(
        response=(
            'User-Agent: *\n'
            'Disallow: /'
        ),
        headers={'Content-Type': 'text/plain; charset=utf-8'}
    )


if os.getenv('ENABLE_TEST_TOKEN_SET') == '1':
    @app.route('/set-token')
    def set_token():
        is_dev_env = os.getenv('FLASK_ENV') == 'dev' and app.debug
        if not is_dev_env:
            return create_json_response([{'error': 'Not in dev env'}], status=403)

        token_file = os.path.join(os.path.dirname(__file__), 'test_data', 'test-auth-token.txt')
        with open(token_file) as fp:
            auth_token = fp.read().strip()

        resp = flask.make_response('OK')
        resp.set_cookie(acm.AUTH_COOKIE_NAME, auth_token, httponly=True, secure=False)
        return resp


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

    app.jinja_env.globals.update(
        auth_link=acm.AUTH_DOMAIN,
        login=data_handler.get_login(),
    )

    html = flask.render_template(template_name, **data)

    resp = flask.make_response(html, status)
    resp.headers['content-type'] = 'text/html; charset=utf-8'

    for name, val in headers.items():
        resp.headers[name] = val

    for name, val in ADDITIONAL_HEADERS.items():
        resp.headers[name] = val

    return resp
