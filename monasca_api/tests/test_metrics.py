# Copyright 2019 FUJITSU LIMITED
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

import json

import falcon
import fixtures
from oslo_config import cfg

from monasca_api.tests import base
from monasca_api.v2.reference import metrics


CONF = cfg.CONF

TENANT_ID = u"fedcba9876543210fedcba9876543210"


class TestMetrics(base.BaseApiTestCase):
    def setUp(self):
        super(TestMetrics, self).setUp()

        self.useFixture(fixtures.MockPatch(
            'monasca_api.common.messaging.kafka_publisher.KafkaPublisher'
        ))
        self.metrics_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.influxdb.metrics_repository.MetricsRepository'
        )).mock

        # [messaging]
        self.conf_override(
            driver='monasca_api.common.messaging.'
                   'kafka_publisher:KafkaPublisher',
            group='messaging')

        self.metrics_resource = metrics.Metrics()

        self.app.add_route('/v2.0/metrics',
                           self.metrics_resource)

    def test_list_metrics(self):
        expected_elements = \
            {'elements': [{'id': '0',
                           'name': 'mon.fake_metric',
                           'dimensions':
                               {'hostname': 'host0',
                                'db': 'vegeta'}},
                          {'id': '1',
                           'name': 'cpu.idle_perc',
                           'dimensions':
                               {'hostname': 'host0',
                                'db': 'vegeta'}}
                          ]}

        return_value = self.metrics_repo_mock.return_value
        return_value.list_metrics.return_value = expected_elements['elements']

        response = self.simulate_request(path='/v2.0/metrics',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET')
        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_elements))

    def test_send_metrics(self):
        request_body = {
            "name": "mon.fake_metric",
            "dimensions": {
                "hostname": "host0",
                "db": "vegeta"
            },
            "timestamp": 1405630174123,
            "value": 1.0,
            "value_meta": {
                "key1": "value1",
                "key2": "value2"
            }}
        response = self.simulate_request(path='/v2.0/metrics',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         body=json.dumps(request_body),
                                         method='POST')
        self.assertEqual(falcon.HTTP_204, response.status)

    def test_send_incorrect_metric(self):
        request_body = {
            "name": "mon.fake_metric",
            "dimensions": 'oh no',
            "timestamp": 1405630174123,
            "value": 1.0}
        response = self.simulate_request(path='/v2.0/metrics',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         body=json.dumps(request_body),
                                         method='POST')
        self.assertEqual(falcon.HTTP_422, response.status)


class TestMeasurements(base.BaseApiTestCase):
    def setUp(self):
        super(TestMeasurements, self).setUp()

        self.metrics_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.influxdb.metrics_repository.MetricsRepository'
        )).mock

        self.metrics_resource = metrics.MetricsMeasurements()

        self.app.add_route('/v2.0/metrics/measurements',
                           self.metrics_resource)

    def test_get_measurements(self):
        expected_measurements = \
            {'elements': [
                {u'name': u'mon.fake_metric',
                 u'columns': [u'timestamp',
                              u'value',
                              u'value_meta'],
                 u'id': '0',
                 u'dimensions': {
                     u'hostname': u'devstack',
                     u'service': u'monitoring'},
                 u'measurements':
                     [[u'2019-03-12T12:37:10.106Z', 98.5, {}],
                      [u'2019-03-12T12:37:23.012Z', 98.8, {}],
                      [u'2019-03-12T12:37:38.031Z', 68.7, {}],
                      [u'2019-03-12T12:37:53.046Z', 55.3, {}],
                      [u'2019-03-12T12:38:08.048Z', 52.8, {}]]}]}

        return_value = self.metrics_repo_mock.return_value
        return_value.measurement_list.return_value = expected_measurements['elements']
        response = self.simulate_request(path='/v2.0/metrics/measurements',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='name=mon.fake_metric&'
                                                      'start_time=2015-03-01T00:00:01.000Z')
        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_measurements))

    def test_get_measurements_invalid_metric_name(self):
        response = self.simulate_request(path='/v2.0/metrics/measurements',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='name=z'
                                                      'start_time=2015-03-01T00:00:01.000Z')
        self.assertEqual(falcon.HTTP_422, response.status)

    def test_get_measurements_missing_start_time(self):
        response = self.simulate_request(path='/v2.0/metrics/measurements',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='name=mon.fake_metric')
        self.assertEqual(falcon.HTTP_422, response.status)

    def test_get_measurements_missing_name(self):
        response = self.simulate_request(path='/v2.0/metrics/measurements',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='start_time=2015-03-01T00:00:01.000Z')
        self.assertEqual(falcon.HTTP_422, response.status)


class TestStatistics(base.BaseApiTestCase):
    def setUp(self):
        super(TestStatistics, self).setUp()

        self.metrics_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.influxdb.metrics_repository.MetricsRepository'
        )).mock

        self.metrics_resource = metrics.MetricsStatistics()

        self.app.add_route('/v2.0/metrics/statistics',
                           self.metrics_resource)

    def test_get_statistics(self):
        expected_statistics = \
            {u'elements': [{u'name': u'mon.fake_metric',
                            u'columns':
                                [u'timestamp',
                                 u'avg'],
                            u'id': '0',
                            u'dimensions':
                                {u'hostname': u'devstack',
                                 u'service': u'monitoring'},
                            u'statistics':
                                [[u'2019-03-12T12:35:00Z', 49.25],
                                 [u'2019-03-12T12:40:00Z', 28.25],
                                 [u'2019-03-12T12:45:00Z', 27.5],
                                 [u'2019-03-12T12:50:00Z', 27],
                                 [u'2019-03-12T12:55:00Z', 28]]}]}
        return_value = self.metrics_repo_mock.return_value
        return_value.metrics_statistics.return_value = expected_statistics['elements']
        response = self.simulate_request(path='/v2.0/metrics/statistics',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='name=mon.fake_metric&'
                                                      'start_time=2015-03-01T00:00:01.000Z&'
                                                      'statistics=avg')
        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_statistics))


class TestMetricsNames(base.BaseApiTestCase):
    def setUp(self):
        super(TestMetricsNames, self).setUp()

        self.metrics_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.influxdb.metrics_repository.MetricsRepository'
        )).mock

        self.metrics_resource = metrics.MetricsNames()

        self.app.add_route('/v2.0/metrics/names',
                           self.metrics_resource)

    def test_get_metrics_names(self):
        expected_metrics_names = \
            {u'elements': [
                {u'name': u'cpu.frequency_mhz'},
                {u'name': u'cpu.idle_perc'},
                {u'name': u'cpu.idle_time'},
                {u'name': u'cpu.percent'},
                {u'name': u'cpu.stolen_perc'},
                {u'name': u'cpu.system_perc'},
                {u'name': u'cpu.system_time'}]}

        return_value = self.metrics_repo_mock.return_value
        return_value.list_metric_names.return_value = expected_metrics_names['elements']
        response = self.simulate_request(path='/v2.0/metrics/names',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET')
        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_metrics_names))


class TestDimensionNames(base.BaseApiTestCase):
    def setUp(self):
        super(TestDimensionNames, self).setUp()

        self.metrics_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.influxdb.metrics_repository.MetricsRepository'
        )).mock

        self.metrics_resource = metrics.DimensionNames()

        self.app.add_route('/v2.0/metrics/dimensions/names',
                           self.metrics_resource)

    def test_get_dimension_names(self):
        expected_dimension_names = \
            {u'elements': [
                {u'dimension_name': u'component'},
                {u'dimension_name': u'consumer_group'},
                {u'dimension_name': u'device'},
                {u'dimension_name': u'hostname'},
                {u'dimension_name': u'mode'}]}

        return_value = self.metrics_repo_mock.return_value
        return_value.list_dimension_names.return_value = expected_dimension_names['elements']
        response = self.simulate_request(path='/v2.0/metrics/dimensions/names',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET')
        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_dimension_names))


class TestDimensionValues(base.BaseApiTestCase):
    def setUp(self):
        super(TestDimensionValues, self).setUp()

        self.metrics_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.influxdb.metrics_repository.MetricsRepository'
        )).mock

        self.metrics_resource = metrics.DimensionValues()

        self.app.add_route('/v2.0/metrics/dimensions/values',
                           self.metrics_resource)

    def test_get_dimension_values(self):
        expected_dimension_names = \
            {u'elements': [
                {u'dimension_value': u'dummy_dimension_value'}]}

        return_value = self.metrics_repo_mock.return_value
        return_value.list_dimension_values.return_value = expected_dimension_names['elements']
        response = self.simulate_request(path='/v2.0/metrics/dimensions/values',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='dimension_name=dummy_dimension_name')
        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_dimension_names))
