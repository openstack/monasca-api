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

import mock
from oslo_config import fixture as oo_cfg
from oslotest import base

from monasca_api.healthcheck import alarms_db_check as rdc
from monasca_api.v2.reference import cfg

CONF = cfg.CONF


class TestMetricsDbHealthCheckLogic(base.BaseTestCase):

    db_connection = "mysql+pymysql://test:test@localhost/mon?charset=utf8mb4"
    mocked_config = {
        'connection': db_connection
    }

    def __init__(self, *args, **kwargs):
        super(TestMetricsDbHealthCheckLogic, self).__init__(*args, **kwargs)
        self._conf = None

    def setUp(self):
        super(TestMetricsDbHealthCheckLogic, self).setUp()
        self._conf = self.useFixture(oo_cfg.Config(CONF))
        self._conf.config(group='database', **self.mocked_config)

    @mock.patch('monasca_api.healthcheck.alarms_db_check.'
                'sql_repository.get_engine')
    def test_should_pass_db_ok(self, _):

        db_health = rdc.AlarmsDbHealthCheck()
        db_health.check_db_status = mock.Mock(return_value=(True, 'OK'))
        result = db_health.health_check()

        self.assertTrue(result.healthy)
        self.assertEqual('OK', result.message)

    @mock.patch('monasca_api.healthcheck.alarms_db_check.'
                'sql_repository.get_engine')
    def test_should_fail_db_unavailable(self, _):

        db_health = rdc.AlarmsDbHealthCheck()
        db_health.check_db_status = mock.Mock(return_value=(False, 'bar'))
        result = db_health.health_check()

        self.assertFalse(result.healthy)
        self.assertEqual('bar', result.message)
