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

from cassandra import cluster as cl
import requests

import mock
from oslo_config import fixture as oo_cfg
from oslotest import base

from monasca_api.common.repositories import exceptions
from monasca_api.healthcheck import metrics_db_check as tdc
from monasca_api.v2.reference import cfg

CONF = cfg.CONF


class TestMetricsDbHealthCheck(base.BaseTestCase):
    cassandra_conf = {
        'cluster_ip_addresses': 'localhost',
        'keyspace': 'test'
    }

    def __init__(self, *args, **kwargs):
        super(TestMetricsDbHealthCheck, self).__init__(*args, **kwargs)
        self._conf = None

    def setUp(self):
        super(TestMetricsDbHealthCheck, self).setUp()
        self._conf = self.useFixture(oo_cfg.Config(CONF))
        self._conf.config(group='cassandra', **self.cassandra_conf)

    def test_should_detect_influxdb_db(self):
        db_health = tdc.MetricsDbCheck()

        # check if influxdb is detected
        self.assertEqual('influxdb', db_health._detected_database_type(
            'influxdb.metrics_repository'))

    def test_should_detect_cassandra_db(self):
        db_health = tdc.MetricsDbCheck()
        # check if cassandra is detected
        self.assertEqual('cassandra', db_health._detected_database_type(
            'cassandra.metrics_repository'))

    def test_should_raise_exception_during_db_detection(self):
        db_health = tdc.MetricsDbCheck()
        # check exception
        db = 'postgresql.metrics_repository'
        self.assertRaises(exceptions.UnsupportedDriverException, db_health._detected_database_type, db)

    @mock.patch.object(requests, 'head')
    def test_should_fail_influxdb_connection(self, req):
        response_mock = mock.Mock()
        response_mock.ok = False
        response_mock.status_code = 500
        req.return_value = response_mock

        influxdb_conf = {
            'ip_address': 'localhost',
            'port': 8086
        }
        messaging_conf = {
            'metrics_driver': 'influxdb.metrics_repository:MetricsRepository'
        }
        self._conf.config(group='repositories', **messaging_conf)
        self._conf.config(group='influxdb', **influxdb_conf)

        db_health = tdc.MetricsDbCheck()
        result = db_health.health_check()

        self.assertFalse(result.healthy)
        self.assertEqual('Error: 500', result.message)

    @mock.patch.object(requests, 'head')
    def test_should_fail_influxdb_wrong_port_number(self, req):
        response_mock = mock.Mock()
        response_mock.ok = False
        response_mock.status_code = 404
        req.return_value = response_mock
        influxdb_conf = {
            'ip_address': 'localhost',
            'port': 8099
        }
        messaging_conf = {
            'metrics_driver': 'influxdb.metrics_repository:MetricsRepository'
        }
        self._conf.config(group='repositories', **messaging_conf)
        self._conf.config(group='influxdb', **influxdb_conf)

        db_health = tdc.MetricsDbCheck()
        result = db_health.health_check()

        self.assertFalse(result.healthy)
        self.assertEqual('Error: 404', result.message)

    @mock.patch.object(requests, 'head')
    def test_should_fail_influxdb_service_unavailable(self, req):
        response_mock = mock.Mock()
        req.side_effect = requests.HTTPError()
        req.return_value = response_mock
        influxdb_conf = {
            'ip_address': 'localhost',
            'port': 8096
        }
        messaging_conf = {
            'metrics_driver': 'influxdb.metrics_repository:MetricsRepository'
        }
        self._conf.config(group='repositories', **messaging_conf)
        self._conf.config(group='influxdb', **influxdb_conf)

        db_health = tdc.MetricsDbCheck()
        result = db_health.health_check()

        self.assertFalse(result.healthy)

    @mock.patch.object(requests, 'head')
    def test_should_pass_infuxdb_available(self, req):
        response_mock = mock.Mock()
        response_mock.ok = True
        response_mock.status_code = 204
        req.return_value = response_mock
        influxdb_conf = {
            'ip_address': 'localhost',
            'port': 8086
        }
        messaging_conf = {
            'metrics_driver': 'influxdb.metrics_repository:MetricsRepository'
        }
        self._conf.config(group='repositories', **messaging_conf)
        self._conf.config(group='influxdb', **influxdb_conf)

        db_health = tdc.MetricsDbCheck()
        result = db_health.health_check()

        self.assertTrue(result.healthy)
        self.assertEqual('OK', result.message)

    @mock.patch('monasca_api.healthcheck.metrics_db_check.importutils.try_import')
    def test_should_fail_cassandra_unavailable(self, try_import):
        messaging_conf = {
            'metrics_driver': 'cassandra.metrics_repository:MetricsRepository'
        }
        cassandra_conf = {
            'cluster_ip_addresses': 'localhost',
            'keyspace': 'test'
        }
        self._conf.config(group='repositories', **messaging_conf)
        self._conf.config(group='cassandra', **cassandra_conf)

        cluster = mock.Mock()
        cas_mock = mock.Mock()
        cas_mock.side_effect = cl.NoHostAvailable(message='Host unavailable',
                                                  errors='Unavailable')
        cluster.Cluster = cas_mock
        try_import.return_value = cluster

        db_health = tdc.MetricsDbCheck()
        result = db_health.health_check()

        self.assertFalse(result.healthy)

    @mock.patch('monasca_api.healthcheck.metrics_db_check.importutils.try_import')
    def test_should_fail_cassandra_no_driver(self, try_import):
        messaging_conf = {
            'metrics_driver': 'cassandra.metrics_repository:MetricsRepository'
        }
        cassandra_conf = {
            'cluster_ip_addresses': 'localhost',
            'keyspace': 'test'
        }
        self._conf.config(group='repositories', **messaging_conf)
        self._conf.config(group='cassandra', **cassandra_conf)

        # Simulate cassandra driver not available
        try_import.return_value = None

        db_health = tdc.MetricsDbCheck()
        db_health.cluster = None
        result = db_health.health_check()

        self.assertFalse(result.healthy)

    @mock.patch('monasca_api.healthcheck.metrics_db_check.importutils.try_import')
    def test_should_pass_cassandra_is_available(self, _):
        messaging_conf = {
            'metrics_driver': 'cassandra.metrics_repository:MetricsRepository'
        }
        cassandra_conf = {
            'cluster_ip_addresses': 'localhost',
            'keyspace': 'test'
        }
        self._conf.config(group='repositories', **messaging_conf)
        self._conf.config(group='cassandra', **cassandra_conf)

        db_health = tdc.MetricsDbCheck()
        result = db_health.health_check()

        self.assertTrue(result.healthy)
