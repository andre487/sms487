import os
import requests
from auth487 import common as acm
from cli_tasks import common

APP_PORT = int(os.getenv('APP_PORT', 8080))


def make_app_request(handler, method='GET', data=None, headers=None, cookies=None, set_token=True):
    if cookies is None:
        cookies = {}

    auth_token = common.get_auth_token()
    url = f'http://127.0.0.1:{APP_PORT}{handler}'

    if set_token:
        cookies[acm.AUTH_COOKIE_NAME] = auth_token

    return requests.request(method, url, cookies=cookies, headers=headers, allow_redirects=False, data=data)


class TestHomePage:
    def test_no_auth(self):
        res = make_app_request('/', set_token=False)
        assert '<title>SMS487</title>' not in res.text
        assert 'Redirecting...' in res.text
        assert res.status_code == 302

    def test_main(self):
        res = make_app_request('/')
        assert '<title>SMS487</title>' in res.text
        assert res.headers['content-type'] == 'text/html; charset=utf-8'
        assert res.status_code == 200


class TestError404:
    def test_no_auth(self):
        res = make_app_request('/404', set_token=False)
        assert res.status_code == 404

    def test_main(self):
        res = make_app_request('/404')
        assert res.headers['content-type'] == 'application/json; charset=utf-8'
        assert res.status_code == 404

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'Not found'


class TestGetSms:
    def test_no_auth(self):
        res = make_app_request('/get-sms', set_token=False)
        assert '<title>SMS487</title>' not in res.text
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
        assert ans[0].get('text') == 'Baz'
        assert ans[0].get('message_type') == 'sms'
        assert ans[0].get('printable_message_type') == 'SMS'

        assert ans[1].get('date_time') == '01 Jan 2018 03:01'
        assert ans[1].get('sms_date_time') == '01 Jan 2018 02:59'
        assert ans[1].get('printable_date_time') == '01 Jan 2018 03:01 (01 Jan 2018 02:59)'
        assert ans[1].get('tel') == '000'
        assert ans[1].get('device_id') == 'Test_2'
        assert ans[1].get('text') == 'Bar'
        assert ans[1].get('message_type') == 'notification'
        assert ans[1].get('printable_message_type') == 'Notification'

    def test_incorrect_limit(self):
        res = make_app_request('/get-sms?limit=foo')
        assert res.headers['content-type'] == 'application/json; charset=utf-8'
        assert res.status_code == 400

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'Incorrect limit'


class TestAddSms:
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

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('status') == 'OK'

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

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'Method is not allowed'

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

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'There is no device ID'

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

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'There is no tel'

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

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'There is no date_time'

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

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'There is no text'

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

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'There is no message type'

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

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'Wrong message type format'

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

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'date_time is incorrect'

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

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'sms_date_time is incorrect'

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

        ans = res.json()
        assert len(ans) == 1
        assert ans[0].get('error') == 'Text is too long'
