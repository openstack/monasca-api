# Copyright 2017 FUJITSU LIMITED
# (C) Copyright 2017 Hewlett Packard Enterprise Development LP
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

from monasca_common.simport import simport

from monasca_api import config
from monasca_api.healthcheck import metrics_db_check as tdc
from monasca_api.tests import base

CONF = config.CONF


class TestMetricsDbHealthCheck(base.BaseTestCase):

    @mock.patch("monasca_api.healthcheck.metrics_db_check.simport")
    def test_health_check(self, simport_mock):
        metrics_repo_mock = simport_mock.load.return_value
        metrics_repo_mock.check_status.return_value = (True, 'OK')
        db_health = tdc.MetricsDbCheck()

        result = db_health.health_check()

        self.assertTrue(result.healthy)

        self.assertEqual(result.message, 'OK')

    @mock.patch("monasca_api.healthcheck.metrics_db_check.simport")
    def test_health_check_failed(self, simport_mock):
        metrics_repo_mock = simport_mock.load.return_value
        metrics_repo_mock.check_status.return_value = (False, 'Error')
        db_health = tdc.MetricsDbCheck()

        result = db_health.health_check()

        self.assertFalse(result.healthy)
        self.assertEqual(result.message, 'Error')

    @mock.patch("monasca_api.healthcheck.metrics_db_check.simport")
    def test_health_check_load_failed(self, simport_mock):
        simport_mock.load.side_effect = simport.ImportFailed(
            "Failed to import 'foo'. Error: bar")
        self.assertRaises(simport.ImportFailed, tdc.MetricsDbCheck)
