# Copyright 2016-2017 FUJITSU LIMITED
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

import ujson as json
import falcon
import mock
from monasca_log_api.app.base import exceptions as log_api_exceptions
from monasca_log_api.app.controller.api import headers
from monasca_log_api.app.controller.v3 import logs
from monasca_log_api.tests import base




ENDPOINT = '/logs'
TENANT_ID = 'bob'
ROLES = 'admin'


def _init_resource(test):
    resource = logs.Logs()
    test.api.add_route(ENDPOINT, resource)
    return resource


def _generate_v3_payload(log_count=None, messages=None):
    if not log_count and messages:
        log_count = len(messages)
        v3_logs = [{
            'message': messages[it],
            'dimensions': {
                'hostname': 'host_%d' % it,
                'component': 'component_%d' % it,
                'service': 'service_%d' % it
                }
            } for it in range(log_count)]
    else:
        v3_logs = [{
            'message': base.generate_unique_message(100),
            'dimensions': {
                'hostname': 'host_%d' % it,
                'component': 'component_%d' % it,
                'service': 'service_%d' % it
            }
        } for it in range(log_count)]
    v3_body = {
        'dimensions': {
            'origin': __name__
        },
        'logs': v3_logs
    }

    return v3_body, v3_logs


class TestApiLogsVersion(base.BaseApiTestCase):

    @mock.patch('monasca_log_api.app.controller.v3.aid'
                '.bulk_processor.BulkProcessor')
    def test_should_return_v3_as_version(self, _):
        logs_resource = logs.Logs()
        self.assertEqual('v3.0', logs_resource.version)


@mock.patch('monasca_log_api.app.base.log_publisher.producer.KafkaProducer')
@mock.patch('monasca_log_api.monitoring.client.monascastatsd.Connection')
class TestApiLogsMonitoring(base.BaseApiTestCase):

    def test_monitor_bulk_rejected(self, __, _):
        res = _init_resource(self)

        in_counter = res._logs_in_counter.increment = mock.Mock()
        bulk_counter = res._bulks_rejected_counter.increment = mock.Mock()
        rejected_counter = res._logs_rejected_counter.increment = mock.Mock()
        size_gauge = res._logs_size_gauge.send = mock.Mock()

        res._get_logs = mock.Mock(
            side_effect=log_api_exceptions.HTTPUnprocessableEntity(''))

        log_count = 1
        v3_body, _ = _generate_v3_payload(log_count)
        payload = json.dumps(v3_body)
        content_length = len(payload)

        self.simulate_request(
            ENDPOINT,
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_TENANT_ID.name: TENANT_ID,
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            },
            body=payload
        )

        self.assertEqual(1, bulk_counter.call_count)
        self.assertEqual(0, in_counter.call_count)
        self.assertEqual(0, rejected_counter.call_count)
        self.assertEqual(0, size_gauge.call_count)

    def test_monitor_not_all_logs_ok(self, __, _):
        res = _init_resource(self)

        in_counter = res._logs_in_counter.increment = mock.Mock()
        bulk_counter = res._bulks_rejected_counter.increment = mock.Mock()
        rejected_counter = res._logs_rejected_counter.increment = mock.Mock()
        size_gauge = res._logs_size_gauge.send = mock.Mock()

        log_count = 5
        reject_logs = 1
        v3_body, _ = _generate_v3_payload(log_count)
        payload = json.dumps(v3_body)
        content_length = len(payload)

        side_effects = [{} for ___ in range(log_count - reject_logs)]
        side_effects.append(log_api_exceptions.HTTPUnprocessableEntity(''))

        res._processor._get_dimensions = mock.Mock(side_effect=side_effects)

        self.simulate_request(
            ENDPOINT,
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_TENANT_ID.name: TENANT_ID,
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            },
            body=payload
        )

        self.assertEqual(1, bulk_counter.call_count)
        self.assertEqual(0,
                         bulk_counter.mock_calls[0][2]['value'])

        self.assertEqual(1, in_counter.call_count)
        self.assertEqual(log_count - reject_logs,
                         in_counter.mock_calls[0][2]['value'])

        self.assertEqual(1, rejected_counter.call_count)
        self.assertEqual(reject_logs,
                         rejected_counter.mock_calls[0][2]['value'])

        self.assertEqual(1, size_gauge.call_count)
        self.assertEqual(content_length,
                         size_gauge.mock_calls[0][2]['value'])

    def test_monitor_all_logs_ok(self, __, _):
        res = _init_resource(self)

        in_counter = res._logs_in_counter.increment = mock.Mock()
        bulk_counter = res._bulks_rejected_counter.increment = mock.Mock()
        rejected_counter = res._logs_rejected_counter.increment = mock.Mock()
        size_gauge = res._logs_size_gauge.send = mock.Mock()

        res._send_logs = mock.Mock()

        log_count = 10

        v3_body, _ = _generate_v3_payload(log_count)

        payload = json.dumps(v3_body)
        content_length = len(payload)
        self.simulate_request(
            ENDPOINT,
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_TENANT_ID.name: TENANT_ID,
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            },
            body=payload
        )

        self.assertEqual(1, bulk_counter.call_count)
        self.assertEqual(0,
                         bulk_counter.mock_calls[0][2]['value'])

        self.assertEqual(1, in_counter.call_count)
        self.assertEqual(log_count,
                         in_counter.mock_calls[0][2]['value'])

        self.assertEqual(1, rejected_counter.call_count)
        self.assertEqual(0,
                         rejected_counter.mock_calls[0][2]['value'])

        self.assertEqual(1, size_gauge.call_count)
        self.assertEqual(content_length,
                         size_gauge.mock_calls[0][2]['value'])


class TestApiLogs(base.BaseApiTestCase):

    @mock.patch('monasca_log_api.app.controller.v3.aid.bulk_processor.'
                'BulkProcessor')
    def test_should_pass_cross_tenant_id(self, bulk_processor):
        logs_resource = _init_resource(self)
        logs_resource._processor = bulk_processor

        v3_body, v3_logs = _generate_v3_payload(1)
        payload = json.dumps(v3_body)
        content_length = len(payload)
        self.simulate_request(
            '/logs',
            method='POST',
            query_string='tenant_id=1',
            headers={
                headers.X_ROLES.name: ROLES,
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            },
            body=payload
        )
        self.assertEqual(falcon.HTTP_204, self.srmock.status)
        logs_resource._processor.send_message.assert_called_with(
            logs=v3_logs,
            global_dimensions=v3_body['dimensions'],
            log_tenant_id='1')

    @mock.patch('monasca_log_api.app.controller.v3.aid.bulk_processor.'
                'BulkProcessor')
    def test_should_fail_not_delegate_ok_cross_tenant_id(self, _):
        _init_resource(self)
        self.simulate_request(
            '/logs',
            method='POST',
            query_string='tenant_id=1',
            headers={
                headers.X_ROLES.name: ROLES,
                'Content-Type': 'application/json',
                'Content-Length': '0'
            }
        )
        self.assertEqual(falcon.HTTP_400, self.srmock.status)

    @mock.patch('monasca_log_api.app.controller.v3.aid.bulk_processor.'
                'BulkProcessor')
    def test_should_pass_empty_cross_tenant_id_wrong_role(self,
                                                          bulk_processor):
        logs_resource = _init_resource(self)
        logs_resource._processor = bulk_processor

        v3_body, _ = _generate_v3_payload(1)
        payload = json.dumps(v3_body)
        content_length = len(payload)
        self.simulate_request(
            '/logs',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            },
            body=payload
        )
        self.assertEqual(falcon.HTTP_204, self.srmock.status)
        self.assertEqual(1, bulk_processor.send_message.call_count)

    @mock.patch('monasca_log_api.app.controller.v3.aid.bulk_processor.'
                'BulkProcessor')
    def test_should_pass_empty_cross_tenant_id_ok_role(self,
                                                       bulk_processor):
        logs_resource = _init_resource(self)
        logs_resource._processor = bulk_processor

        v3_body, _ = _generate_v3_payload(1)
        payload = json.dumps(v3_body)
        content_length = len(payload)
        self.simulate_request(
            '/logs',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            },
            body=payload
        )
        self.assertEqual(falcon.HTTP_204, self.srmock.status)
        self.assertEqual(1, bulk_processor.send_message.call_count)


class TestUnicodeLogs(base.BaseApiTestCase):

    @mock.patch('monasca_log_api.app.base.log_publisher.producer.'
                'KafkaProducer')
    def test_should_send_unicode_messages(self, _):
        _init_resource(self)

        messages = [m['input'] for m in base.UNICODE_MESSAGES]
        v3_body, _ = _generate_v3_payload(messages=messages)
        payload = json.dumps(v3_body, ensure_ascii=False)
        content_length = len(payload)
        self.simulate_request(
            '/logs',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            },
            body=payload
        )
        self.assertEqual(falcon.HTTP_204, self.srmock.status)
