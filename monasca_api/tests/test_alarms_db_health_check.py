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

from unittest import mock

from monasca_api import config
from monasca_api.healthcheck import alarms_db_check as rdc
from monasca_api.tests import base

CONF = config.CONF


class TestMetricsDbHealthCheckLogic(base.BaseTestCase):

    db_connection = "mysql+pymysql://test:test@localhost/mon?charset=utf8mb4"
    mocked_config = {
        'connection': db_connection
    }

    def setUp(self):
        """
        Sets the logging

        Args:
            self: (todo): write your description
        """
        super(TestMetricsDbHealthCheckLogic, self).setUp()
        self.conf_default(group='database', **self.mocked_config)

    @classmethod
    def tearDownClass(cls):
        """
        Tear down the class.

        Args:
            cls: (todo): write your description
        """
        if hasattr(CONF, 'sql_engine'):
            delattr(CONF, 'sql_engine')

    @mock.patch('monasca_api.healthcheck.alarms_db_check.'
                'sql_repository.get_engine')
    def test_should_pass_db_ok(self, _):
        """
        Check if health health health health is alive.

        Args:
            self: (todo): write your description
            _: (todo): write your description
        """

        db_health = rdc.AlarmsDbHealthCheck()
        db_health.check_db_status = mock.Mock(return_value=(True, 'OK'))
        result = db_health.health_check()

        self.assertTrue(result.healthy)
        self.assertEqual('OK', result.message)

    @mock.patch('monasca_api.healthcheck.alarms_db_check.'
                'sql_repository.get_engine')
    def test_should_fail_db_unavailable(self, _):
        """
        Whether the health health health health status false otherwise.

        Args:
            self: (todo): write your description
            _: (todo): write your description
        """

        db_health = rdc.AlarmsDbHealthCheck()
        db_health.check_db_status = mock.Mock(return_value=(False, 'bar'))
        result = db_health.health_check()

        self.assertFalse(result.healthy)
        self.assertEqual('bar', result.message)
