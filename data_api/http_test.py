import json
import os

import requests
from auth487 import common as acm
from app import data_handler
from cli_tasks import common

APP_PORT = int(os.getenv('APP_PORT', 8080))
PROJECT_DIR = os.path.dirname(__file__)
TEST_DATA_PATH = os.path.join(PROJECT_DIR, 'test_data')

with open(os.path.join(TEST_DATA_PATH, 'fixture.json')) as fp:
    FIXTURE = json.load(fp)


def make_app_request(handler, method='GET', data=None, headers=None, cookies=None, set_token=True):
    if cookies is None:
        cookies = {}

    auth_token = common.get_auth_token()
    url = f'http://127.0.0.1:{APP_PORT}{handler}'

    if set_token:
        cookies[acm.AUTH_COOKIE_NAME] = auth_token

    return requests.request(method, url, cookies=cookies, headers=headers, allow_redirects=False, data=data)


class BaseTest:
    def setup_method(self):
        db = data_handler.get_mongo_db()

        for name in db.list_collection_names():
            collection = db.get_collection(name)
            collection.drop()

        for name, data in FIXTURE.items():
            db.get_collection(name).insert_many(data)

    def teardown_method(self):
        data_handler.get_mongo_client().drop_database(data_handler.MONGO_DB_NAME)


class TestHomePage(BaseTest):
    def test_no_auth(self):
        res = make_app_request('/', set_token=False)
        assert '<title>SMS 487 – Messages</title>' not in res.text
        assert 'Redirecting...' in res.text
        assert res.status_code == 302

    def test_main(self):
        res = make_app_request('/')
        assert '<title>SMS 487 – Messages</title>' in res.text
        assert res.headers['content-type'] == 'text/html; charset=utf-8'
        assert res.status_code == 200


class TestError404(BaseTest):
    def test_no_auth(self):
        res = make_app_request('/404', set_token=False)
        assert res.status_code == 404

    def test_main(self):
        res = make_app_request('/404')
        assert res.headers['content-type'] == 'application/json; charset=utf-8'
        assert res.status_code == 404

        assert res.json().get('error') == 'Not found'


class TestGetSms(BaseTest):
    def test_no_auth(self):
        res = make_app_request('/get-sms', set_token=False)
        assert '<title>SMS 487</title>' not in res.text
        assert 'Redirecting...' in res.text
        assert res.status_code == 302

    def test_main(self):
        res = make_app_request('/get-sms')
        assert res.headers['content-type'] == 'application/json; charset=utf-8'
        assert res.status_code == 200

        ans = res.json()
        assert len(ans) >= 2

        assert ans[0].get('date_time') == '01 Jan 2018 03:03'
        assert ans[0].get('sms_date_time') == '01 Jan 2018 03:03'
        assert ans[0].get('printable_date_time') == '01 Jan 2018 03:03'
        assert ans[0].get('tel') == '000'
        assert ans[0].get('device_id') == 'Test_3'
        assert ans[0].get('text') == 'Baz https://yandex.ru Quux'
        assert ans[0].get('message_type') == 'sms'
        assert ans[0].get('printable_message_type') == 'SMS'

        assert ans[1].get('date_time') == '01 Jan 2018 03:01'
        assert ans[1].get('sms_date_time') == '01 Jan 2018 02:59'
        assert ans[1].get('printable_date_time') == '01 Jan 2018 03:01 (01 Jan 2018 02:59)'
        assert ans[1].get('tel') == '000'
        assert ans[1].get('device_id') == 'Test_2'
        assert ans[1].get('text') == 'https://yandex.ru Bar https://google.ru <script>alert(1)</script>'
        assert ans[1].get('message_type') == 'notification'
        assert ans[1].get('printable_message_type') == 'Notification'

    def test_incorrect_limit(self):
        res = make_app_request('/get-sms?limit=foo')
        assert res.headers['content-type'] == 'application/json; charset=utf-8'
        assert res.status_code == 400

        assert res.json().get('error') == 'Incorrect limit'


class TestAddSms(BaseTest):
    def test_no_auth(self):
        res = make_app_request('/add-sms', method='POST', data={
            'message_type': 'sms',
            'date_time': '2018-01-01 00:04:00 +0000',
            'sms_date_time': '2017-12-31 23:04:00 +0000',
            'tel': '03',
            'device_id': 'TestTest',
            'text': 'Quux',
        }, set_token=False)
        assert res.status_code == 403
        assert res.headers['content-type'] == 'application/json'

        ans = res.json()
        assert ans.get('error') == 'Auth error'

    def test_main(self):
        res = make_app_request('/add-sms', method='POST', data={
            'message_type': 'sms',
            'date_time': '2018-01-01 00:04:00 +0000',
            'sms_date_time': '2017-12-31 23:04:00 +0000',
            'tel': '03',
            'device_id': 'TestTest',
            'text': 'Quux',
        })

        assert res.headers['content-type'] == 'application/json; charset=utf-8'
        assert res.status_code == 200

        assert res.json().get('status') == 'OK'

        res = make_app_request('/get-sms?limit=1')
        ans = res.json()
        assert len(ans) == 1

        assert ans[0].get('date_time') == '01 Jan 2018 03:04'
        assert ans[0].get('sms_date_time') == '01 Jan 2018 02:04'
        assert ans[0].get('printable_date_time') == '01 Jan 2018 03:04 (01 Jan 2018 02:04)'
        assert ans[0].get('tel') == '03'
        assert ans[0].get('device_id') == 'TestTest'
        assert ans[0].get('text') == 'Quux'
        assert ans[0].get('message_type') == 'sms'
        assert ans[0].get('printable_message_type') == 'SMS'

    def test_wrong_method(self):
        res = make_app_request('/add-sms', method='GET', data={
            'message_type': 'sms',
            'date_time': '2018-01-01 00:04:00 +0000',
            'sms_date_time': '2017-12-31 23:04:00 +0000',
            'tel': '03',
            'device_id': 'TestTest',
            'text': 'Quux',
        })

        assert res.status_code == 405
        assert res.headers['content-type'] == 'application/json; charset=utf-8'

        assert res.json().get('error') == 'Method is not allowed'

    def test_no_device_id(self):
        res = make_app_request('/add-sms', method='POST', data={
            'message_type': 'sms',
            'date_time': '2018-01-01 00:04:00 +0000',
            'sms_date_time': '2017-12-31 23:04:00 +0000',
            'tel': '03',
            'text': 'Quux',
        })

        assert res.status_code == 400
        assert res.headers['content-type'] == 'application/json; charset=utf-8'

        assert res.json().get('error') == 'There is no device ID'

    def test_no_tel(self):
        res = make_app_request('/add-sms', method='POST', data={
            'message_type': 'sms',
            'date_time': '2018-01-01 00:04:00 +0000',
            'sms_date_time': '2017-12-31 23:04:00 +0000',
            'device_id': 'TestTest',
            'text': 'Quux',
        })

        assert res.status_code == 400
        assert res.headers['content-type'] == 'application/json; charset=utf-8'

        assert res.json().get('error') == 'There is no tel'

    def test_no_date_time(self):
        res = make_app_request('/add-sms', method='POST', data={
            'message_type': 'sms',
            'sms_date_time': '2017-12-31 23:04:00 +0000',
            'device_id': 'TestTest',
            'tel': '000',
            'text': 'Quux',
        })

        assert res.status_code == 400
        assert res.headers['content-type'] == 'application/json; charset=utf-8'

        assert res.json().get('error') == 'There is no date_time'

    def test_no_text(self):
        res = make_app_request('/add-sms', method='POST', data={
            'message_type': 'sms',
            'date_time': '2017-12-31 23:04:00 +0000',
            'sms_date_time': '2017-12-31 23:04:00 +0000',
            'device_id': 'TestTest',
            'tel': '000',
        })

        assert res.status_code == 400
        assert res.headers['content-type'] == 'application/json; charset=utf-8'

        assert res.json().get('error') == 'There is no text'

    def test_no_message_type(self):
        res = make_app_request('/add-sms', method='POST', data={
            'date_time': '2017-12-31 23:04:00 +0000',
            'sms_date_time': '2017-12-31 23:04:00 +0000',
            'device_id': 'TestTest',
            'tel': '000',
            'text': 'Quux',
        })

        assert res.status_code == 400
        assert res.headers['content-type'] == 'application/json; charset=utf-8'

        assert res.json().get('error') == 'There is no message type'

    def test_wrong_message_type(self):
        res = make_app_request('/add-sms', method='POST', data={
            'message_type': '!',
            'date_time': '2017-12-31 23:04:00 +0000',
            'sms_date_time': '2017-12-31 23:04:00 +0000',
            'device_id': 'TestTest',
            'tel': '000',
            'text': 'Quux',
        })

        assert res.status_code == 400
        assert res.headers['content-type'] == 'application/json; charset=utf-8'

        assert res.json().get('error') == 'Wrong message type format'

    def test_wrong_date_time(self):
        res = make_app_request('/add-sms', method='POST', data={
            'message_type': 'sms',
            'date_time': '2017-12-31',
            'sms_date_time': '2017-12-31 23:04:00 +0000',
            'device_id': 'TestTest',
            'tel': '000',
            'text': 'Quux',
        })

        assert res.status_code == 400
        assert res.headers['content-type'] == 'application/json; charset=utf-8'

        assert res.json().get('error') == 'date_time is incorrect'

    def test_wrong_sms_date_time(self):
        res = make_app_request('/add-sms', method='POST', data={
            'message_type': 'sms',
            'date_time': '2017-12-31 23:04:00 +0000',
            'sms_date_time': '2017-12-31',
            'device_id': 'TestTest',
            'tel': '000',
            'text': 'Quux',
        })

        assert res.status_code == 400
        assert res.headers['content-type'] == 'application/json; charset=utf-8'

        assert res.json().get('error') == 'sms_date_time is incorrect'

    def test_too_long_text(self):
        res = make_app_request('/add-sms', method='POST', data={
            'message_type': 'sms',
            'date_time': '2017-12-31 23:04:00 +0000',
            'sms_date_time': '2017-12-31 23:04:00 +0000',
            'device_id': 'TestTest',
            'tel': '000',
            'text': 'Quux' * 2048,
        })

        assert res.status_code == 400
        assert res.headers['content-type'] == 'application/json; charset=utf-8'

        assert res.json().get('error') == 'Text is too long'


class TestFiltersPage(BaseTest):
    def test_no_auth(self):
        res = make_app_request('/filters', set_token=False)
        assert 'Redirecting...' in res.text
        assert res.status_code == 302

    def test_main(self):
        res = make_app_request('/filters')
        assert '<title>SMS 487 – Filters</title>' in res.text
        assert res.headers['content-type'] == 'text/html; charset=utf-8'
        assert res.status_code == 200


class TestExportFilters(BaseTest):
    def test_no_auth(self):
        res = make_app_request('/export-filters', set_token=False)
        assert res.status_code == 403
        assert res.headers['content-type'] == 'application/json'

        assert res.json().get('error') == 'Auth error'

    def test_main(self):
        res = make_app_request('/export-filters')

        assert res.status_code == 200
        assert res.headers['content-type'] == 'application/json; charset=utf-8'
        assert res.headers['content-disposition'] == 'attachment; filename="sms487-filter-export.json"'

        ans = res.json()
        first = ans[0]

        assert 'id' in first

        first.pop('id')
        assert ans[0] == {
            'op': 'and',
            'tel': '',
            'device_id': 'Test_1',
            'text': '',
            'action': 'mark',
        }


class TestSaveFilters(BaseTest):
    def test_no_auth(self):
        res = make_app_request('/save-filters', method='POST', set_token=False, cookies={
            acm.CSRF_COOKIE_NAME: common.get_csrf_token(),
        }, data={acm.CSRF_FIELD_NAME: common.get_csrf_token()})

        assert res.status_code == 403
        assert res.headers['content-type'] == 'application/json'

        assert res.json().get('error') == 'Auth error'

    def test_update(self):
        filters = self._get_filters()

        assert filters[0]['device_id'] == 'Test_1'
        filters[0]['device_id'] = 'Device Foo'

        req_data = self._create_update_data(filters)
        res = make_app_request('/save-filters', method='POST', data=req_data, cookies={
            acm.CSRF_COOKIE_NAME: common.get_csrf_token(),
        })

        assert 'Redirecting...' in res.text
        assert res.status_code == 302

        filters = self._get_filters()

        assert filters[0]['device_id'] == 'Device Foo'

    def test_update_invalid_id(self):
        filters = self._get_filters()

        filters[0]['id'] = 'invalid'

        req_data = self._create_update_data(filters)
        res = make_app_request('/save-filters', method='POST', data=req_data, cookies={
            acm.CSRF_COOKIE_NAME: common.get_csrf_token(),
        })

        assert res.headers['content-type'] == 'application/json; charset=utf-8'
        assert res.json().get('error') == 'Invalid filter ID: invalid'
        assert res.status_code == 400

    def test_remove_by_param(self):
        filters = self._get_filters()
        len0 = len(filters)

        req_data = self._create_update_data(filters)

        rec_id0 = filters[0]['id']
        req_data[f'remove:{rec_id0}'] = '1'

        res = make_app_request('/save-filters', method='POST', data=req_data, cookies={
            acm.CSRF_COOKIE_NAME: common.get_csrf_token(),
        })

        assert 'Redirecting...' in res.text
        assert res.status_code == 302

        filters = self._get_filters()

        assert len(filters) == len0 - 1

        ids = set()
        for rec in filters:
            ids.add(rec['id'])

        assert rec_id0 not in ids

    def test_remove_by_empty_update(self):
        filters = self._get_filters()
        len0 = len(filters)
        rec_id0 = filters[0]['id']

        req_data = self._create_update_data(filters)
        req_data[f'tel:{rec_id0}'] = req_data[f'device_id:{rec_id0}'] = req_data[f'text:{rec_id0}'] = ''

        res = make_app_request('/save-filters', method='POST', data=req_data, cookies={
            acm.CSRF_COOKIE_NAME: common.get_csrf_token(),
        })

        assert 'Redirecting...' in res.text
        assert res.status_code == 302

        filters = self._get_filters()

        assert len(filters) == len0 - 1

        ids = set()
        for rec in filters:
            ids.add(rec['id'])

        assert rec_id0 not in ids

    def test_create(self):
        filters = self._get_filters()

        assert filters[0]['device_id'] == 'Test_1'

        req_data = self._create_update_data(filters)
        req_data.update({
            'op:new': 'and',
            'tel:new': '487',
            'device_id:new': 'Device Foo',
            'text:new': 'Quux',
            'action:new': 'mark',
        })

        res = make_app_request('/save-filters', method='POST', data=req_data, cookies={
            acm.CSRF_COOKIE_NAME: common.get_csrf_token(),
        })

        assert 'Redirecting...' in res.text
        assert res.status_code == 302

        filters = self._get_filters()

        assert len(filters) > 1
        first = filters[0]
        second = filters[1]

        assert first['op'] == 'and'
        assert first['tel'] == '487'
        assert first['device_id'] == 'Device Foo'
        assert first['text'] == 'Quux'
        assert first['action'] == 'mark'

        assert second['device_id'] == 'Test_1'

    def test_create_no_csrf(self):
        res = make_app_request('/save-filters', method='POST', data={}, cookies={
            acm.CSRF_COOKIE_NAME: common.get_csrf_token(),
        })

        assert res.status_code == 403
        assert res.headers['content-type'] == 'text/html; charset=utf-8'

        assert res.text == 'No CSRF token'

    def test_create_invalid_field_name(self):
        filters = self._get_filters()
        req_data = self._create_update_data(filters)
        req_data.update({
            'op:new': 'and',
            'tel:new': '487',
            'device_id:new': 'Device Foo',
            'text:new': 'Quux',
            'action:new': 'mark',
            'invalid_field': 'foo',
        })

        res = make_app_request('/save-filters', method='POST', data=req_data, cookies={
            acm.CSRF_COOKIE_NAME: common.get_csrf_token(),
        })

        assert res.headers['content-type'] == 'application/json; charset=utf-8'
        assert res.json().get('error') == 'Invalid field name: invalid_field'
        assert res.status_code == 400

    def test_create_invalid_op(self):
        filters = self._get_filters()
        req_data = self._create_update_data(filters)
        req_data.update({
            'op:new': 'xor',
            'tel:new': '487',
            'device_id:new': 'Device Foo',
            'text:new': 'Quux',
            'action:new': 'mark',
        })

        res = make_app_request('/save-filters', method='POST', data=req_data, cookies={
            acm.CSRF_COOKIE_NAME: common.get_csrf_token(),
        })

        assert res.headers['content-type'] == 'application/json; charset=utf-8'
        assert res.json().get('error', '').startswith('Invalid op: xor')
        assert res.status_code == 400

    def test_create_invalid_action(self):
        filters = self._get_filters()
        req_data = self._create_update_data(filters)
        req_data.update({
            'op:new': 'or',
            'tel:new': '487',
            'device_id:new': 'Device Foo',
            'text:new': 'Quux',
            'action:new': 'make',
        })

        res = make_app_request('/save-filters', method='POST', data=req_data, cookies={
            acm.CSRF_COOKIE_NAME: common.get_csrf_token(),
        })

        assert res.headers['content-type'] == 'application/json; charset=utf-8'
        assert res.json().get('error', '').startswith('Invalid action: make')
        assert res.status_code == 400

    def _get_filters(self):
        filters = make_app_request('/export-filters').json()

        assert len(filters) > 0

        return filters

    def _create_update_data(self, filters):
        req_data = {acm.CSRF_FIELD_NAME: common.get_csrf_token()}
        for filter_data in filters:
            rec_id = filter_data['id']
            for name, val in filter_data.items():
                if name == 'id':
                    continue

                req_data[f'{name}:{rec_id}'] = val
        return req_data
