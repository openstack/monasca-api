# Copyright 2016 FUJITSU LIMITED
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
import mock
from monasca_log_api.app.controller.api import headers
from monasca_log_api.app.controller.v2 import logs as v2_logs
from monasca_log_api.app.controller.v3 import logs as v3_logs
from monasca_log_api.tests import base




class TestApiSameV2V3Output(base.BaseApiTestCase):

    # noinspection PyProtectedMember
    @mock.patch('monasca_log_api.app.base.log_publisher.'
                'producer.KafkaProducer')
    def test_send_identical_messages(self, _):
        # mocks only log publisher, so the last component that actually
        # sends data to kafka
        # case is to verify if publisher was called with same arguments
        # for both cases

        v2 = v2_logs.Logs()
        v3 = v3_logs.Logs()

        publish_mock = mock.Mock()

        v2._kafka_publisher._kafka_publisher.publish = publish_mock
        v3._processor._kafka_publisher.publish = publish_mock

        component = 'monasca-log-api'
        service = 'laas'
        hostname = 'kornik'
        tenant_id = 'ironMan'
        roles = 'admin'

        v2_dimensions = 'hostname:%s,service:%s' % (hostname, service)
        v3_dimensions = {
            'hostname': hostname,
            'component': component,
            'service': service
        }

        v2_body = {
            'message': 'test'
        }

        v3_body = {
            'logs': [
                {
                    'message': 'test',
                    'dimensions': v3_dimensions
                }
            ]
        }

        self.api.add_route('/v2.0', v2)
        self.api.add_route('/v3.0', v3)

        self.simulate_request(
            '/v2.0',
            method='POST',
            headers={
                headers.X_ROLES.name: roles,
                headers.X_DIMENSIONS.name: v2_dimensions,
                headers.X_APPLICATION_TYPE.name: component,
                headers.X_TENANT_ID.name: tenant_id,
                'Content-Type': 'application/json',
                'Content-Length': '100'
            },
            body=json.dumps(v2_body)
        )

        self.simulate_request(
            '/v3.0',
            method='POST',
            headers={
                headers.X_ROLES.name: roles,
                headers.X_TENANT_ID.name: tenant_id,
                'Content-Type': 'application/json',
                'Content-Length': '100'
            },
            body=json.dumps(v3_body)
        )

        self.assertEqual(2, publish_mock.call_count)

        # in v2 send_messages is called with single envelope
        v2_send_msg_arg = publish_mock.mock_calls[0][1][1]

        # in v3 it is always called with list of envelopes
        v3_send_msg_arg = publish_mock.mock_calls[1][1][1]

        self.maxDiff = None

        # at this point we know that both args should be identical
        self.assertEqual(type(v2_send_msg_arg), type(v3_send_msg_arg))
        self.assertIsInstance(v3_send_msg_arg, list)

        self.assertEqual(len(v2_send_msg_arg), len(v3_send_msg_arg))
        self.assertEqual(1, len(v2_send_msg_arg))

        v2_msg_as_dict = json.loads(v2_send_msg_arg[0])
        v3_msg_as_dict = json.loads(v3_send_msg_arg[0])

        self.assertDictEqual(v2_msg_as_dict, v3_msg_as_dict)
