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
from unittest import mock

from monasca_api import config
from monasca_api.healthcheck import base
from monasca_api import healthchecks
from monasca_api.tests import base as test_base


CONF = config.CONF
ENDPOINT = '/healthcheck'


class TestHealthChecks(test_base.BaseApiTestCase):

    @classmethod
    def tearDownClass(cls):
        """
        Tear down the class.

        Args:
            cls: (todo): write your description
        """
        if hasattr(CONF, 'sql_engine'):
            delattr(CONF, 'sql_engine')

    def setUp(self):
        """
        Sets the route for this application.

        Args:
            self: (todo): write your description
        """
        super(TestHealthChecks, self).setUp()
        self.resources = healthchecks.HealthChecks()
        self.app.add_route(
            ENDPOINT,
            self.resources
        )

    @mock.patch('monasca_api.healthcheck.alarms_db_check.sql_repository.get_engine')
    @mock.patch(
        'monasca_api.healthcheck.metrics_db_check.MetricsDbCheck')
    def test_should_return_200_for_head(self, metrics_db_check, _):
        """
        Returns true if the given a list contains a test.

        Args:
            self: (todo): write your description
            metrics_db_check: (bool): write your description
            _: (todo): write your description
        """
        result = self.simulate_request(path=ENDPOINT, method='HEAD')
        self.assertEqual(falcon.HTTP_NO_CONTENT, result.status)

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
        """
        Check if there s no health report?

        Args:
            self: (todo): write your description
            kafka_check: (bool): write your description
            alarms_db_check: (todo): write your description
            metrics_db_check: (bool): write your description
            _: (todo): write your description
        """
        kafka_check.health_check.return_value = base.CheckResult(True, 'OK')
        alarms_db_check.health_check.return_value = base.CheckResult(True,
                                                                     'OK')
        metrics_db_check.health_check.return_value = base.CheckResult(True,
                                                                      'OK')
        self.resources._kafka_check = kafka_check
        self.resources._alarm_db_check = alarms_db_check
        self.resources._metrics_db_check = metrics_db_check

        response = self.simulate_request(path=ENDPOINT,
                                         headers={
                                             'Content-Type': 'application/json'
                                         },
                                         method='GET')
        self.assertEqual(falcon.HTTP_OK, response.status)

        response = response.json
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
        """
        Check if there is_shouldka.

        Args:
            self: (todo): write your description
            kafka_check: (bool): write your description
            alarms_db_check: (todo): write your description
            metrics_db_check: (bool): write your description
            _: (todo): write your description
        """
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
            alarms_db_check.health_check.return_value = base.CheckResult(
                service['alarms_db']['healthy'], service['alarms_db']['message'])
            metrics_db_check.health_check.return_value = base.CheckResult(
                service['netrics_db']['healthy'], service['netrics_db']['message'])
            self.resources._kafka_check = kafka_check
            self.resources._alarm_db_check = alarms_db_check
            self.resources._metrics_db_check = metrics_db_check

            response = self.simulate_request(path=ENDPOINT,
                                             headers={
                                                 'Content-Type': 'application/json'
                                             },
                                             method='GET')
            self.assertEqual(falcon.HTTP_SERVICE_UNAVAILABLE,
                             response.status)

            response = response.json
            self.assertIn('kafka', response)
            self.assertIn('alarms_database', response)
            self.assertIn('metrics_database', response)
            self.assertEqual(service['kafka']['message'], response.get('kafka'))
            self.assertEqual(service['alarms_db']['message'], response.get('alarms_database'))
            self.assertEqual(service['netrics_db']['message'], response.get('metrics_database'))
