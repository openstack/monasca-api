# Copyright 2015 kornicameister@gmail.com
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

import falcon
import mock

from monasca_log_api.app.base import exceptions as log_api_exceptions
from monasca_log_api.app.controller.api import headers
from monasca_log_api.app.controller.v2 import logs
from monasca_log_api.tests import base

ROLES = 'admin'


def _init_resource(test):
    resource = logs.Logs()
    test.api.add_route('/log/single', resource)
    return resource


class TestApiLogsVersion(base.BaseApiTestCase):
    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    @mock.patch('monasca_log_api.app.controller.v2.aid.service.LogCreator')
    def test_should_return_v2_as_version(self, _, __):
        logs_resource = logs.Logs()
        self.assertEqual('v2.0', logs_resource.version)


class TestApiLogs(base.BaseApiTestCase):

    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    @mock.patch('monasca_log_api.app.controller.v2.aid.service.LogCreator')
    def test_should_contain_deprecated_details_in_successful_response(self,
                                                                      _,
                                                                      __):
        _init_resource(self)

        self.simulate_request(
            '/log/single',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_DIMENSIONS.name: 'a:1',
                'Content-Type': 'application/json',
                'Content-Length': '0'
            }
        )

        self.assertEqual(falcon.HTTP_204, self.srmock.status)
        self.assertIn('deprecated', self.srmock.headers_dict)
        self.assertIn('link', self.srmock.headers_dict)

    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    @mock.patch('monasca_log_api.app.controller.v2.aid.service.LogCreator')
    def test_should_fail_not_delegate_ok_cross_tenant_id(self, _, __):
        _init_resource(self)
        self.simulate_request(
            '/log/single',
            method='POST',
            query_string='tenant_id=1',
            headers={
                'Content-Type': 'application/json',
                'Content-Length': '0'
            }
        )
        self.assertEqual(falcon.HTTP_401, self.srmock.status)

    @mock.patch('monasca_log_api.app.controller.v2.aid.service.LogCreator')
    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    def test_should_pass_empty_cross_tenant_id_wrong_role(self,
                                                          log_creator,
                                                          kafka_publisher):
        logs_resource = _init_resource(self)
        logs_resource._log_creator = log_creator
        logs_resource._kafka_publisher = kafka_publisher

        self.simulate_request(
            '/log/single',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_DIMENSIONS.name: 'a:1',
                'Content-Type': 'application/json',
                'Content-Length': '0'
            }
        )
        self.assertEqual(falcon.HTTP_204, self.srmock.status)

        self.assertEqual(1, kafka_publisher.send_message.call_count)
        self.assertEqual(1, log_creator.new_log.call_count)
        self.assertEqual(1, log_creator.new_log_envelope.call_count)

    @mock.patch('monasca_log_api.app.controller.v2.aid.service.LogCreator')
    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    def test_should_pass_empty_cross_tenant_id_ok_role(self,
                                                       log_creator,
                                                       kafka_publisher):
        logs_resource = _init_resource(self)
        logs_resource._log_creator = log_creator
        logs_resource._kafka_publisher = kafka_publisher

        self.simulate_request(
            '/log/single',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_DIMENSIONS.name: 'a:1',
                'Content-Type': 'application/json',
                'Content-Length': '0'
            }
        )
        self.assertEqual(falcon.HTTP_204, self.srmock.status)

        self.assertEqual(1, kafka_publisher.send_message.call_count)
        self.assertEqual(1, log_creator.new_log.call_count)
        self.assertEqual(1, log_creator.new_log_envelope.call_count)

    @mock.patch('monasca_log_api.app.controller.v2.aid.service.LogCreator')
    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    def test_should_pass_delegate_cross_tenant_id_ok_role(self,
                                                          log_creator,
                                                          log_publisher):
        resource = _init_resource(self)
        resource._log_creator = log_creator
        resource._kafka_publisher = log_publisher

        self.simulate_request(
            '/log/single',
            method='POST',
            query_string='tenant_id=1',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_DIMENSIONS.name: 'a:1',
                'Content-Type': 'application/json',
                'Content-Length': '0'
            }
        )
        self.assertEqual(falcon.HTTP_204, self.srmock.status)

        self.assertEqual(1, log_publisher.send_message.call_count)
        self.assertEqual(1, log_creator.new_log.call_count)
        self.assertEqual(1, log_creator.new_log_envelope.call_count)

    @mock.patch('monasca_common.rest.utils')
    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    def test_should_fail_empty_dimensions_delegate(self, _, rest_utils):
        _init_resource(self)
        rest_utils.read_body.return_value = True

        self.simulate_request(
            '/log/single',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_DIMENSIONS.name: '',
                'Content-Type': 'application/json',
                'Content-Length': '0'
            },
            body='{"message":"test"}'
        )
        self.assertEqual(log_api_exceptions.HTTP_422, self.srmock.status)

    @mock.patch('monasca_log_api.app.controller.v2.aid.service.LogCreator')
    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    def test_should_fail_for_invalid_content_type(self, _, __):
        _init_resource(self)

        self.simulate_request(
            '/log/single',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_DIMENSIONS.name: '',
                'Content-Type': 'video/3gpp',
                'Content-Length': '0'
            }
        )
        self.assertEqual(falcon.HTTP_415, self.srmock.status)

    @mock.patch('monasca_log_api.app.controller.v2.aid.service.LogCreator')
    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    def test_should_pass_payload_size_not_exceeded(self, _, __):
        _init_resource(self)

        max_log_size = 1000
        content_length = max_log_size - 100
        self.conf_override(max_log_size=max_log_size, group='service')

        self.simulate_request(
            '/log/single',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_DIMENSIONS.name: '',
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            }
        )
        self.assertEqual(falcon.HTTP_204, self.srmock.status)

    @mock.patch('monasca_log_api.app.controller.v2.aid.service.LogCreator')
    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    def test_should_fail_payload_size_exceeded(self, _, __):
        _init_resource(self)

        max_log_size = 1000
        content_length = max_log_size + 100
        self.conf_override(max_log_size=max_log_size, group='service')

        self.simulate_request(
            '/log/single',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_DIMENSIONS.name: '',
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            }
        )
        self.assertEqual(falcon.HTTP_413, self.srmock.status)

    @mock.patch('monasca_log_api.app.controller.v2.aid.service.LogCreator')
    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    def test_should_fail_payload_size_equal(self, _, __):
        _init_resource(self)

        max_log_size = 1000
        content_length = max_log_size
        self.conf_override(max_log_size=max_log_size, group='service')

        self.simulate_request(
            '/log/single',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_DIMENSIONS.name: '',
                'Content-Type': 'application/json',
                'Content-Length': str(content_length)
            }
        )
        self.assertEqual(falcon.HTTP_413, self.srmock.status)

    @mock.patch('monasca_log_api.app.controller.v2.aid.service.LogCreator')
    @mock.patch('monasca_log_api.app.base.log_publisher.LogPublisher')
    def test_should_fail_content_length(self, _, __):
        _init_resource(self)

        self.simulate_request(
            '/log/single',
            method='POST',
            headers={
                headers.X_ROLES.name: ROLES,
                headers.X_DIMENSIONS.name: '',
                'Content-Type': 'application/json'
            }
        )
        self.assertEqual(falcon.HTTP_411, self.srmock.status)
