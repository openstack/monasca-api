# Copyright 2017 FUJITSU LIMITED
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
from falcon import testing
import mock
from oslo_config import fixture as oo_cfg

from monasca_api.healthcheck import base
from monasca_api import healthchecks
from monasca_api.v2.reference import cfg
from monasca_common.rest import utils

CONF = cfg.CONF
ENDPOINT = '/healthcheck'


class TestHealthChecks(testing.TestBase):
    def setUp(self):
        super(TestHealthChecks, self).setUp()
        self.conf = self.useFixture(oo_cfg.Config(CONF))

    def set_route(self):
        self.resources = healthchecks.HealthChecks()
        self.api.add_route(
            ENDPOINT,
            self.resources
        )

    @mock.patch('monasca_api.healthcheck.alarms_db_check.sql_repository.get_engine')
    def test_should_return_200_for_head(self, _):
        self.set_route()
        self.simulate_request(ENDPOINT, method='HEAD')
        self.assertEqual(falcon.HTTP_NO_CONTENT, self.srmock.status)

    @mock.patch('monasca_api.healthcheck.kafka_check.KafkaHealthCheck')
    @mock.patch(
        'monasca_api.healthcheck.alarms_db_check.AlarmsDbHealthCheck')
    @mock.patch(
        'monasca_api.healthcheck.metrics_db_check.MetricsDbCheck')
    @mock.patch(
        'monasca_api.healthcheck.alarms_db_check.sql_repository.SQLRepository')
    def test_should_report_healthy_if_all_services_healthy(self, kafka_check,
                                                           alarms_db_check,
                                                           metrics_db_check,
                                                           _):
        kafka_check.health_check.return_value = base.CheckResult(True, 'OK')
        alarms_db_check.health_check.return_value = base.CheckResult(True,
                                                                     'OK')
        metrics_db_check.health_check.return_value = base.CheckResult(True,
                                                                      'OK')
        self.set_route()
        self.resources._kafka_check = kafka_check
        self.resources._alarm_db_check = alarms_db_check
        self.resources._metrics_db_check = metrics_db_check

        response = self.simulate_request(ENDPOINT,
                                         headers={
                                             'Content-Type': 'application/json'
                                         },
                                         decode='utf8',
                                         method='GET')
        self.assertEqual(falcon.HTTP_OK, self.srmock.status)

        response = utils.from_json(response)
        self.assertIn('kafka', response)
        self.assertIn('alarms_database', response)
        self.assertIn('metrics_database', response)
        self.assertEqual('OK', response.get('kafka'))
        self.assertEqual('OK', response.get('alarms_database'))
        self.assertEqual('OK', response.get('metrics_database'))

    @mock.patch('monasca_api.healthcheck.kafka_check.KafkaHealthCheck')
    @mock.patch(
        'monasca_api.healthcheck.alarms_db_check.AlarmsDbHealthCheck')
    @mock.patch(
        'monasca_api.healthcheck.metrics_db_check.MetricsDbCheck')
    @mock.patch(
        'monasca_api.healthcheck.alarms_db_check.sql_repository.SQLRepository')
    def test_should_report_not_healthy_if_one_service_not_healthy(self,
                                                                  kafka_check,
                                                                  alarms_db_check,
                                                                  metrics_db_check,
                                                                  _):
        test_list = [
            {'kafka': {'healthy': False, 'message': 'Unavailable'},
             'alarms_db': {'healthy': True, 'message': 'OK'},
             'netrics_db': {'healthy': True, 'message': 'OK'}
             },
            {'kafka': {'healthy': True, 'message': 'OK'},
             'alarms_db': {'healthy': False, 'message': 'Connection Error'},
             'netrics_db': {'healthy': True, 'message': 'OK'}
             },
            {'kafka': {'healthy': True, 'message': 'OK'},
             'alarms_db': {'healthy': True, 'message': 'OK'},
             'netrics_db': {'healthy': False, 'message': 'Error'}
             },
        ]

        for service in test_list:
            kafka_check.health_check.return_value = base.CheckResult(service['kafka']['healthy'],
                                                                     service['kafka']['message'])
            alarms_db_check.health_check.return_value = base.CheckResult(service['alarms_db']['healthy'],
                                                                         service['alarms_db']['message'])
            metrics_db_check.health_check.return_value = base.CheckResult(service['netrics_db']['healthy'],
                                                                          service['netrics_db']['message'])
            self.set_route()
            self.resources._kafka_check = kafka_check
            self.resources._alarm_db_check = alarms_db_check
            self.resources._metrics_db_check = metrics_db_check

            response = self.simulate_request(ENDPOINT,
                                             headers={
                                                 'Content-Type': 'application/json'
                                             },
                                             decode='utf8',
                                             method='GET')
            self.assertEqual(falcon.HTTP_SERVICE_UNAVAILABLE, self.srmock.status)

            response = utils.from_json(response)
            self.assertIn('kafka', response)
            self.assertIn('alarms_database', response)
            self.assertIn('metrics_database', response)
            self.assertEqual(service['kafka']['message'], response.get('kafka'))
            self.assertEqual(service['alarms_db']['message'], response.get('alarms_database'))
            self.assertEqual(service['netrics_db']['message'], response.get('metrics_database'))
