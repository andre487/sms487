import json
import logging
import os
import secrets
import sys

import flask
from auth487 import common as acm
from auth487 import flask as ath
from flask import request
from werkzeug.exceptions import UnsupportedMediaType

from app import data_handler, templating

if sys.version_info[0] < 3 or sys.version_info[1] < 6:
    raise EnvironmentError('Use Python >= 3.6')


def get_sw_content():
    with open(os.path.join(PROJECT_DIR, 'static', 'sw.js')) as fp:
        return fp.read()


PROJECT_DIR = os.path.dirname(__file__)
SW_JS = get_sw_content()

app = flask.Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 1024 * 1024

LOG_FORMAT = '%(asctime)s %(levelname)s\t%(message)s\t%(pathname)s:%(lineno)d %(funcName)s %(process)d %(threadName)s'
LOG_LEVEL = os.getenv('LOG_LEVEL', logging.INFO)

logging.basicConfig(format=LOG_FORMAT, level=LOG_LEVEL)
templating.setup_filters(app)

ADDITIONAL_HEADERS = {
    'Content-Security-Policy': (
        "default-src 'none'; "
        "style-src 'self'; "
        "script-src 'self' 'nonce-<nonce>'; "
        "img-src 'self'; "
        "manifest-src 'self';"
    ),
    'X-Frame-Options': 'deny',
}


@app.before_request
def before_request():
    ath.check_csrf_token(app, api_urls=(
        flask.url_for('add_sms'),
    ))


@app.route('/')
@ath.require_auth(access=['sms'])
def index():
    device_id = request.args.get('device-id', '').strip()
    limit = request.args.get('limit', '5')
    refresh = request.args.get('refresh') == '1'
    apply_filters = request.args.get('no-filters') != '1'

    if device_id == 'All':
        device_id = None

    if limit:
        try:
            limit = int(limit)
        except ValueError:
            return create_html_response('error.html', {'code': 400, 'message': 'Incorrect limit'}, status=400)

    try:
        result = data_handler.get_sms(device_id, limit, apply_filters=apply_filters)
    except data_handler.FormDataError as e:
        logging.info('Client error: %s', e)
        return create_html_response('error.html', {'code': 400, 'message': str(e)}, status=400)

    device_ids = [('All', not device_id)] + [
        (name, name == device_id) for name in data_handler.get_device_ids()
    ]

    return create_html_response('index.html', {
        'title': 'SMS 487 – Messages',
        'messages': result, 'limit': limit,
        'device_id': device_id, 'device_ids': device_ids,
        'refresh': refresh, 'apply_filters': apply_filters,
    })


@app.route('/get-sms')
@ath.require_auth(access=['sms'])
def get_sms():
    device_id = request.args.get('device-id', '').strip()
    limit = request.args.get('limit', '30')
    apply_filters = request.args.get('no-filters') != '1'

    if limit:
        try:
            limit = int(limit)
        except ValueError:
            return create_json_response({'error': 'Incorrect limit'}, status=400)

    try:
        result = data_handler.get_sms(device_id, limit, apply_filters=apply_filters)
    except data_handler.FormDataError as e:
        logging.info('Client error: %s', e)
        return create_json_response({'error': str(e)}, status=400)

    return create_json_response(result)


@app.route('/add-sms', methods=['POST'])
@ath.require_auth(no_redirect=True, access=['sms'])
def add_sms():
    try:
        try:
            data = request.json
        except UnsupportedMediaType:
            data = request.form
        count = data_handler.add_sms(data)
        return create_json_response({'status': 'OK', 'added': count})
    except data_handler.FormDataError as e:
        logging.info('Client error: %s', e)
        return create_json_response({'error': str(e)}, status=400)
    except Exception as e:
        logging.exception('General error: %s', e)
        return create_json_response({'error': str(e)}, status=500)


@app.route('/filters')
@ath.require_auth(access=['sms'])
def show_filters():
    filters = data_handler.get_filters()
    return create_html_response('show_filters.html', {
        'title': 'SMS 487 – Filters',
        'filters': filters,
    })


@app.route('/save-filters', methods=['POST'])
@ath.require_auth(access=['sms'], no_redirect=True)
def save_filters():
    try:
        data_handler.save_filters(request.form)
        return flask.redirect(flask.url_for('show_filters'))
    except data_handler.FormDataError as e:
        logging.info('Client error: %s', e)
        return create_json_response({'error': str(e)}, status=400)
    except Exception as e:
        logging.exception('General error: %s', e)
        return create_json_response({'error': str(e)}, status=500)


@app.route('/export-filters')
@ath.require_auth(access=['sms'], no_redirect=True)
def export_filters():
    filters = data_handler.get_filters()
    return create_json_response(filters, headers={
        'Content-Disposition': 'attachment; filename="sms487-filter-export.json"'
    }, pretty=True)


@app.route('/import-filters', methods=['POST'])
@ath.require_auth(access=['sms'], no_redirect=True)
def import_filters():
    try:
        data_handler.import_filters(request.files.get('filters_file'))
        return flask.redirect(flask.url_for('show_filters'))
    except data_handler.FormDataError as e:
        logging.info('Client error: %s', e)
        return create_json_response({'error': str(e)}, status=400)
    except Exception as e:
        logging.exception('General error: %s', e)
        return create_json_response({'error': str(e)}, status=500)


@app.route('/sw.js')
def sw_js():
    content = SW_JS
    if app.debug:
        content = get_sw_content()

    return flask.Response(
        headers={
            'Content-Type': 'application/javascript; charset=utf-8',
            'Cache-Control': 'private, max-age=0, no-transform',
        },
        response=[content],
    )


@app.route('/manifest.json')
def web_manifest():
    origin = flask.request.headers.get('origin', '')

    data = {
        'name': 'SMS 487 – test' if app.debug else 'SMS 487',
        'start_url': os.getenv("START_URL", origin),
    }

    return flask.Response(
        headers={
            'Content-Type': 'application/json; charset=utf-8',
            'Cache-Control': 'private, max-age=600, no-transform',
        },
        response=[flask.render_template('manifest.json', **data)],
    )


@app.route('/robots.txt')
def robots_txt():
    return flask.Response(
        response=[(
            'User-Agent: *\n'
            'Disallow: /'
        )],
        headers={'Content-Type': 'text/plain; charset=utf-8'}
    )


if os.getenv('ENABLE_TEST_TOKEN_SET') == '1':
    @app.route('/set-token')
    def set_token():
        is_dev_env = os.getenv('FLASK_ENV') == 'dev' and app.debug
        if not is_dev_env:
            return create_json_response({'error': 'Not in dev env'}, status=403)

        with open(os.path.join(os.path.dirname(__file__), 'test_data', 'auth_key.pem')) as fp:
            auth_private_key = fp.read()
        auth_token = acm.create_auth_token('test', {'access': {'sms': True}}, auth_private_key)

        resp = flask.make_response('OK')
        resp.set_cookie(acm.AUTH_COOKIE_NAME, auth_token, httponly=True, secure=False)
        return resp


@app.errorhandler(404)
def error_404(*_args):
    return create_json_response({'error': 'Not found'}, status=404)


@app.errorhandler(405)
def error_405(*_args):
    return create_json_response({'error': 'Method is not allowed'}, status=405)


def create_json_response(data, status=200, headers=None, pretty=False):
    if headers is None:
        headers = {}

    dump_options = {'ensure_ascii': False}
    if pretty:
        dump_options['indent'] = 2

    resp = flask.make_response(json.dumps(data, **dump_options), status)
    resp.headers['content-type'] = 'application/json; charset=utf-8'
    for name, val in headers.items():
        resp.headers[name] = val

    return resp


def create_html_response(template_name, data, status=200, headers=None):
    if headers is None:
        headers = {}

    resp = flask.Response(status=status)

    nonce = secrets.token_hex(4)
    app.jinja_env.globals.update(
        nonce=nonce,
        auth_link=acm.AUTH_BASE_URL,
        login=data_handler.get_login(),
        csrf_field_name=acm.CSRF_FIELD_NAME,
        csrf_token=ath.set_csrf_token(app, resp=resp),
    )

    resp.response = [flask.render_template(template_name, **data)]
    resp.headers['content-type'] = 'text/html; charset=utf-8'

    for name, val in headers.items():
        resp.headers[name] = val

    for name, val in ADDITIONAL_HEADERS.items():
        resp.headers[name] = val.replace('<nonce>', nonce)

    return resp
