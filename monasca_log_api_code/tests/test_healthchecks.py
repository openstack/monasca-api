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
import simplejson as json

from monasca_log_api.app.controller import healthchecks
from monasca_log_api.healthcheck import kafka_check as healthcheck
from monasca_log_api.tests import base

ENDPOINT = '/healthcheck'


class TestApiHealthChecks(base.BaseApiTestCase):

    def before(self):
        self.resource = healthchecks.HealthChecks()
        self.api.add_route(
            ENDPOINT,
            self.resource
        )

    def test_should_return_200_for_head(self):
        self.simulate_request(ENDPOINT, method='HEAD')
        self.assertEqual(falcon.HTTP_NO_CONTENT, self.srmock.status)

    @mock.patch('monasca_log_api.healthcheck.kafka_check.KafkaHealthCheck')
    def test_should_report_healthy_if_kafka_healthy(self, kafka_check):
        kafka_check.healthcheck.return_value = healthcheck.CheckResult(True,
                                                                       'OK')
        self.resource._kafka_check = kafka_check

        ret = self.simulate_request(ENDPOINT,
                                    headers={
                                        'Content-Type': 'application/json'
                                    },
                                    decode='utf8',
                                    method='GET')
        self.assertEqual(falcon.HTTP_OK, self.srmock.status)

        ret = json.loads(ret)
        self.assertIn('kafka', ret)
        self.assertEqual('OK', ret.get('kafka'))

    @mock.patch('monasca_log_api.healthcheck.kafka_check.KafkaHealthCheck')
    def test_should_report_unhealthy_if_kafka_healthy(self, kafka_check):
        url = 'localhost:8200'
        err_str = 'Could not connect to kafka at %s' % url
        kafka_check.healthcheck.return_value = healthcheck.CheckResult(False,
                                                                       err_str)
        self.resource._kafka_check = kafka_check

        ret = self.simulate_request(ENDPOINT,
                                    headers={
                                        'Content-Type': 'application/json'
                                    },
                                    decode='utf8',
                                    method='GET')
        self.assertEqual(falcon.HTTP_SERVICE_UNAVAILABLE, self.srmock.status)

        ret = json.loads(ret)
        self.assertIn('kafka', ret)
        self.assertEqual(err_str, ret.get('kafka'))
