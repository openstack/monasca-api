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

import falcon
import simplejson as json
from unittest import mock

from monasca_api.tests import base
from monasca_api.v2.reference import logs

ENDPOINT = '/logs'
TENANT_ID = 'bob'
ROLES = 'admin'


def _init_resource(test):
    resource = logs.Logs()
    test.app.add_route(ENDPOINT, resource)
    return resource


def _generate_payload(log_count=None, messages=None):
    if not log_count and messages:
        log_count = len(messages)
        logs = [{
            'message': messages[it],
            'dimensions': {
                'hostname': 'host_%d' % it,
                'component': 'component_%d' % it,
                'service': 'service_%d' % it
            }
        } for it in range(log_count)]
    else:
        logs = [{
            'message': base.generate_unique_message(100),
            'dimensions': {
                'hostname': 'host_%d' % it,
                'component': 'component_%d' % it,
                'service': 'service_%d' % it
            }
        } for it in range(log_count)]
    body = {
        'dimensions': {
            'origin': __name__
        },
        'logs': logs
    }

    return body, logs


class TestApiLogsVersion(base.BaseApiTestCase):

    @mock.patch('monasca_api.v2.common.bulk_processor.BulkProcessor')
    def test_should_return_as_version(self, _):
        logs_resource = logs.Logs()
        self.assertEqual('v2.0', logs_resource.version)


class TestApiLogs(base.BaseApiTestCase):

    @mock.patch('monasca_api.v2.common.bulk_processor.BulkProcessor')
    def test_should_pass_cross_tenant_id(self, bulk_processor):
        logs_resource = _init_resource(self)
        logs_resource._processor = bulk_processor

        body, logs = _generate_payload(1)
        payload = json.dumps(body)
        content_length = len(payload)
        response = self.simulate_request(
            path='/logs',
            method='POST',
            query_string='project_id=1',
            headers={
                'X_ROLES': ROLES,
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            },
            body=payload
        )
        self.assertEqual(falcon.HTTP_204, response.status)
        logs_resource._processor.send_message.assert_called_with(
            logs=logs,
            global_dimensions=body['dimensions'],
            log_tenant_id='1')

    @mock.patch('monasca_api.v2.common.bulk_processor.BulkProcessor')
    def test_should_fail_not_delegate_ok_cross_tenant_id(self, _):
        _init_resource(self)
        response = self.simulate_request(
            path='/logs',
            method='POST',
            query_string='project_id=1',
            headers={
                'X-Roles': ROLES,
                'Content-Type': 'application/json',
                'Content-Length': '0'
            }
        )
        self.assertEqual(falcon.HTTP_400, response.status)

    @mock.patch('monasca_api.v2.common.bulk_processor.BulkProcessor')
    def test_should_pass_empty_cross_tenant_id_wrong_role(self,
                                                          bulk_processor):
        logs_resource = _init_resource(self)
        logs_resource._processor = bulk_processor

        body, _ = _generate_payload(1)
        payload = json.dumps(body)
        content_length = len(payload)
        response = self.simulate_request(
            path='/logs',
            method='POST',
            headers={
                'X-Roles': ROLES,
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            },
            body=payload
        )
        self.assertEqual(falcon.HTTP_204, response.status)
        self.assertEqual(1, bulk_processor.send_message.call_count)

    @mock.patch('monasca_api.v2.common.bulk_processor.BulkProcessor')
    def test_should_pass_empty_cross_tenant_id_ok_role(self,
                                                       bulk_processor):
        logs_resource = _init_resource(self)
        logs_resource._processor = bulk_processor

        body, _ = _generate_payload(1)
        payload = json.dumps(body)
        content_length = len(payload)
        response = self.simulate_request(
            path='/logs',
            method='POST',
            headers={
                'X-Roles': ROLES,
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            },
            body=payload
        )
        self.assertEqual(falcon.HTTP_204, response.status)
        self.assertEqual(1, bulk_processor.send_message.call_count)


class TestUnicodeLogs(base.BaseApiTestCase):

    @mock.patch('monasca_api.api.core.log.log_publisher.client_factory'
                '.get_kafka_producer')
    def test_should_send_unicode_messages(self, _):
        _init_resource(self)

        messages = [m['input'] for m in base.UNICODE_MESSAGES]
        body, _ = _generate_payload(messages=messages)
        payload = json.dumps(body, ensure_ascii=False)
        response = self.simulate_request(
            path='/logs',
            method='POST',
            headers={
                'X-Roles': ROLES,
                'Content-Type': 'application/json'
            },
            body=payload
        )
        self.assertEqual(falcon.HTTP_204, response.status)
