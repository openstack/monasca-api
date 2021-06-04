# -*- coding: utf-8 -*-
# Copyright 2015 Cray Inc.
# (C) Copyright 2015,2017 Hewlett Packard Enterprise Development LP
# Copyright 2016-2017 FUJITSU LIMITED
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

from collections import OrderedDict
import copy
import datetime
import json

import falcon.testing
import fixtures
import testtools.matchers as matchers

from unittest.mock import Mock

import oslo_config.fixture
import six

from monasca_api.common.repositories.model import sub_alarm_definition
from monasca_api.tests import base
from monasca_api.v2.common.exceptions import HTTPUnprocessableEntityError
from monasca_api.v2.reference import alarm_definitions
from monasca_api.v2.reference import alarms

CONF = oslo_config.cfg.CONF

TENANT_ID = u"fedcba9876543210fedcba9876543210"

ALARM_HISTORY = OrderedDict((
    # Only present in data returned from InfluxDB:
    (u"time", u"2015-01-01T00:00:00.000Z"),
    # Only present in data returned from API:
    (u"timestamp", u"2015-01-01T00:00:00.000Z"),
    (u"alarm_id", u"10000000-1000-1000-1000-10000000000"),
    (u"metrics", [{
        u"id": None,
        u"name": u"test.metric",
        u"dimensions": {u"dim1": u"dval1", u"dim2": u"dval2"}
    }]),
    (u"new_state", u"ALARM"),
    (u"old_state", u"OK"),
    (u"reason", u"Alarm reason"),
    (u"reason_data", u"{}"),
    (u"sub_alarms", [{
        u"sub_alarm_expression": {
            u"function": u"MAX",
            # Only present in data returned from InfluxDB:
            u"metric_definition": {
                u"id": None,
                u"name": u"test.metric",
                u"dimensions": {u"dim1": u"dval1"},
            },
            # Only present in data returned from API:
            u'metric_name': u'test.metric',
            # Only present in data returned from API:
            u'dimensions': {u'dim1': u'dval1'},
            u"operator": u"GT",
            u"threshold": 50.0,
            u"period": 60,
            u"periods": 1
        },
        u"sub_alarm_state": u"ALARM",
        u"current_values": [50.1],
    }]),
    # Only present in data returned from InfluxDB:
    (u"tenant_id", TENANT_ID),
    # Only present in data returned from API:
    (u"id", u"1420070400000"),
))


class InfluxClientAlarmHistoryResponseFixture(fixtures.MockPatch):
    def _build_series(self, name, column_dict):
        return {
            "name": name,
            "columns": column_dict.keys(),
            "values": [column_dict.values(), ],
        }

    def _setUp(self):
        super(InfluxClientAlarmHistoryResponseFixture, self)._setUp()

        mock_data = copy.deepcopy(ALARM_HISTORY)

        del mock_data[u"id"]
        del mock_data[u"timestamp"]
        del mock_data[u"sub_alarms"][0][u"sub_alarm_expression"][u"metric_name"]
        del mock_data[u"sub_alarms"][0][u"sub_alarm_expression"][u"dimensions"]
        mock_data[u"sub_alarms"] = json.dumps(mock_data[u"sub_alarms"])
        mock_data[u"metrics"] = json.dumps(mock_data[u"metrics"])

        self.mock.return_value.query.return_value.raw = {
            "series": [self._build_series("alarm_state_history", mock_data)]
        }


class RESTResponseEquals(object):
    """Match if the supplied data contains a single string containing a JSON
    object which decodes to match expected_data, excluding the contents of
    the 'links' key.
    """

    def __init__(self, expected_data):
        self.expected_data = expected_data

        if u"links" in expected_data:
            del expected_data[u"links"]

    def __str__(self):
        return 'RESTResponseEquals(%s)' % (self.expected,)

    def match(self, actual):
        response_data = actual.json

        if u"links" in response_data:
            del response_data[u"links"]

        return matchers.Equals(self.expected_data).match(response_data)


class AlarmTestBase(base.BaseApiTestCase):
    def setUp(self):
        super(AlarmTestBase, self).setUp()

        self.useFixture(fixtures.MockPatch(
            'monasca_api.common.messaging.kafka_publisher.KafkaPublisher'))

        # [messaging]
        self.conf_override(
            driver='monasca_api.common.messaging.'
                   'kafka_publisher:KafkaPublisher',
            group='messaging')

        # [repositories]
        self.conf_override(
            alarms_driver='monasca_api.common.repositories.sqla.'
                          'alarms_repository:AlarmsRepository',
            group='repositories')
        self.conf_override(
            alarm_definitions_driver='monasca_api.common.repositories.'
                                     'alarm_definitions_repository:'
                                     'AlarmDefinitionsRepository',
            group='repositories')
        self.conf_override(
            metrics_driver='monasca_api.common.repositories.influxdb.'
                           'metrics_repository:MetricsRepository',
            group='repositories')


class TestAlarmsStateHistory(AlarmTestBase):
    def setUp(self):
        super(TestAlarmsStateHistory, self).setUp()

        self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.sqla.'
            'alarms_repository.AlarmsRepository'))
        self.useFixture(InfluxClientAlarmHistoryResponseFixture(
            'monasca_api.common.repositories.influxdb.'
            'metrics_repository.client.InfluxDBClient'))

        self.alarms_resource = alarms.AlarmsStateHistory()
        self.app.add_route(
            '/v2.0/alarms/{alarm_id}/state-history', self.alarms_resource)

        self.app.add_route(
            '/v2.0/alarms/state-history', self.alarms_resource)

    def test_alarm_state_history(self):
        expected_elements = {u"elements": [dict(ALARM_HISTORY)]}
        del expected_elements[u"elements"][0][u"time"]
        del (expected_elements[u"elements"][0][u"sub_alarms"][0]
             [u"sub_alarm_expression"][u"metric_definition"])
        del expected_elements[u"elements"][0][u"tenant_id"]
        response = self.simulate_request(
            path=u'/v2.0/alarms/%s/state-history/' % ALARM_HISTORY[u"alarm_id"],
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID,
            })
        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))

    def test_alarm_state_history_no_alarm_id(self):
        expected_elements = {u'elements': []}

        response = self.simulate_request(
            path=u'/v2.0/alarms/state-history/',
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID,
            })
        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))


class TestAlarmsCount(AlarmTestBase):
    def setUp(self):
        super(TestAlarmsCount, self).setUp()

        self.alarms_get_alarms_count_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.sqla.alarms_repository.AlarmsRepository'
        )).mock

        self.alarms_count_resource = alarms.AlarmsCount()
        self.app.add_route('/v2.0/alarms/count',
                           self.alarms_count_resource)

    def test_get_alarm_count(self):
        return_value = self.alarms_get_alarms_count_mock.return_value
        expected_elements = {'counts': [[4]], 'columns': ['count']}

        return_value.get_alarms_count.return_value = [{'count': 4}]

        response = self.simulate_request(path='/v2.0/alarms/count',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))

    def test_get_alarm_count_state_parameter(self):
        return_value = self.alarms_get_alarms_count_mock.return_value
        expected_elements = {'counts': [[4]], 'columns': ['count']}

        return_value.get_alarms_count.return_value = [{'count': 4}]

        response = self.simulate_request(path='/v2.0/alarms/count',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='state=OK')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))

    def test_get_alarm_count_severity_parameter(self):
        return_value = self.alarms_get_alarms_count_mock.return_value
        expected_elements = {'counts': [[4]], 'columns': ['count']}

        return_value.get_alarms_count.return_value = [{'count': 4}]

        response = self.simulate_request(path='/v2.0/alarms/count',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='severity=LOW')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))

    def test_get_alarm_count_group_by_parameter(self):
        return_value = self.alarms_get_alarms_count_mock.return_value
        expected_elements = {'columns': ['count', 'metric_name'],
                             'counts': [[2, 'cpu.idle_perc'],
                                        [1, 'cpu.sys_mem']]}

        return_value.get_alarms_count.return_value = [{'metric_name': u'cpu.idle_perc', 'count': 2},
                                                      {'metric_name': u'cpu.sys_mem', 'count': 1}]

        response = self.simulate_request(path='/v2.0/alarms/count',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='group_by=metric_name')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))

        expected_elements = {'columns': ['count', 'metric_name', 'dimension_name'],
                             'counts': [[2, 'cpu.idle_perc', 'hostname'],
                                        [1, 'cpu.sys_mem', 'hostname']]}

        return_value.get_alarms_count.return_value = [{'metric_name': u'cpu.idle_perc',
                                                       'dimension_name': 'hostname',
                                                       'count': 2},
                                                      {'metric_name': u'cpu.sys_mem',
                                                       'dimension_name': 'hostname',
                                                       'count': 1}]

        response = self.simulate_request(path='/v2.0/alarms/count',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='group_by=metric_name,dimension_name')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))

    def test_get_alarm_count_incorrect_group_by_parameter(self):
        return_value = self.alarms_get_alarms_count_mock.return_value

        return_value.get_alarms_count.return_value = [{'metric_name': u'cpu.idle_perc', 'count': 2},
                                                      {'metric_name': u'cpu.sys_mem', 'count': 1}]

        response = self.simulate_request(
            path='/v2.0/alarms/count',
            headers={'X-Roles': CONF.security.default_authorized_roles[0],
                     'X-Tenant-Id': TENANT_ID},
            method='GET',
            query_string='group_by=hahahah')

        self.assertEqual(response.status, falcon.HTTP_422)

    def test_get_alarm_count_offset(self):
        return_value = self.alarms_get_alarms_count_mock.return_value
        expected_elements = {'columns': ['count', 'metric_name'],
                             'counts': [[2, 'cpu.idle_perc']]}

        return_value.get_alarms_count.return_value = [{'metric_name': u'cpu.idle_perc', 'count': 2}]

        response = self.simulate_request(path='/v2.0/alarms/count',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='group_by=metric_name&offset=1')
        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))

    def test_get_alarm_count_incorrect_offset(self):
        return_value = self.alarms_get_alarms_count_mock.return_value
        expected_elements = {'description': 'Offset must be a valid integer, was hahahah',
                             'title': 'Unprocessable Entity'}

        return_value.get_alarms_count.return_value = [{'metric_name': u'cpu.idle_perc', 'count': 2}]

        response = self.simulate_request(path='/v2.0/alarms/count',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='group_by=metric_name&offset=hahahah')
        self.assertEqual(response.status, falcon.HTTP_422)
        self.assertThat(response, RESTResponseEquals(expected_elements))

    def test_get_alarm_count_limit_parameter(self):
        return_value = self.alarms_get_alarms_count_mock.return_value
        expected_elements = {'counts': [[4]], 'columns': ['count']}

        return_value.get_alarms_count.return_value = [{'count': 4}]

        response = self.simulate_request(path='/v2.0/alarms/count',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='limit=1')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))

        return_value.get_alarms_count.return_value = [{'count': 4}]
        expected_elements = {'counts': [], 'columns': ['count']}

        response = self.simulate_request(path='/v2.0/alarms/count',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='limit=0')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))

    def test_get_alarm_count_when_count_is_zero(self):
        return_value = self.alarms_get_alarms_count_mock.return_value
        expected_elements = {'columns': ['count', 'metric_name'], 'counts': [[0, None]]}

        return_value.get_alarms_count.return_value = [{'count': 0}]

        response = self.simulate_request(path='/v2.0/alarms/count',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='group_by=metric_name')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))

        expected_elements = {'columns': ['count'], 'counts': [[0]]}
        response = self.simulate_request(path='/v2.0/alarms/count',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))


class TestAlarms(AlarmTestBase):
    def setUp(self):
        super(TestAlarms, self).setUp()

        self.alarms_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.sqla.alarms_repository.AlarmsRepository'
        )).mock

        self.alarms_resource = alarms.Alarms()
        self.app.add_route('/v2.0/alarms',
                           self.alarms_resource)
        self.app.add_route('/v2.0/alarms/{alarm_id}',
                           self.alarms_resource)

    def test_alarms_get_alarms(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarms.return_value = \
            [{'alarm_definition_id': '1',
              'metric_dimensions': 'instance_id=123,service=monitoring',
              'alarm_definition_name': '90% CPU',
              'state': 'OK',
              'state_updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'metric_name': 'cpu.idle_perc',
              'link': 'http://somesite.com/this-alarm-info',
              'severity': 'LOW',
              'created_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'alarm_id': '1',
              'lifecycle_state': 'OPEN'}]

        expected_alarms = {
            'elements': [{
                'alarm_definition': {
                    'id': '1',
                    'links': [{
                        'href': 'http://falconframework.org/v2.0/alarm-definitions/1',
                        'rel': 'self'}],
                    'name': '90% CPU',
                    'severity': 'LOW'},
                'created_timestamp': '2015-03-14T09:26:53Z',
                'id': '1',
                'lifecycle_state': 'OPEN',
                'link': 'http://somesite.com/this-alarm-info',
                'links': [{
                    'href': 'http://falconframework.org/v2.0/alarms/1',
                    'rel': 'self'}],
                'metrics': [{
                    'dimensions': {
                        'instance_id': '123',
                        'service': 'monitoring'},
                    'name': 'cpu.idle_perc'}],
                'state': 'OK',
                'state_updated_timestamp': '2015-03-14T09:26:53Z',
                'updated_timestamp': '2015-03-14T09:26:53Z'}]}

        response = self.simulate_request(path='/v2.0/alarms',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_alarms))

    def test_alarms_get_alarm(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarm.return_value = \
            [{'alarm_definition_id': '1',
              'metric_dimensions': 'instance_id=123,service=monitoring',
              'alarm_definition_name': '90% CPU',
              'state': 'OK',
              'state_updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'metric_name': 'cpu.idle_perc',
              'link': 'http://somesite.com/this-alarm-info',
              'severity': 'LOW',
              'created_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'alarm_id': '1',
              'lifecycle_state': 'OPEN'}]

        expected_alarms = {'alarm_definition': {
            'id': '1',
            'links': [{
                'href': 'http://falconframework.org/v2.0/alarm-definitions/1',
                'rel': 'self'}],
            'name': '90% CPU',
            'severity': 'LOW'},
            'created_timestamp': '2015-03-14T09:26:53Z',
            'id': '1',
            'lifecycle_state': 'OPEN',
            'link': 'http://somesite.com/this-alarm-info',
            'links': [{
                'href': 'http://falconframework.org/v2.0/alarms/1',
                'rel': 'self'}],
            'metrics': [{
                'dimensions': {
                    'instance_id': '123',
                    'service': 'monitoring'},
                'name': 'cpu.idle_perc'}],
            'state': 'OK',
            'state_updated_timestamp': '2015-03-14T09:26:53Z',
            'updated_timestamp': '2015-03-14T09:26:53Z'}

        response = self.simulate_request(path='/v2.0/alarms/1',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_alarms))

    def test_alarms_get_alarms_state_parameter(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarms.return_value = \
            [{'alarm_definition_id': '1',
              'metric_dimensions': 'instance_id=123,service=monitoring',
              'alarm_definition_name': '90% CPU',
              'state': 'OK',
              'state_updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'metric_name': 'cpu.idle_perc',
              'link': 'http://somesite.com/this-alarm-info',
              'severity': 'LOW',
              'created_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'alarm_id': '1',
              'lifecycle_state': 'OPEN'}]

        expected_alarms = {
            'elements': [{
                'alarm_definition': {
                    'id': '1',
                    'links': [{
                        'href': 'http://falconframework.org/v2.0/alarm-definitions/1',
                        'rel': 'self'}],
                    'name': '90% CPU',
                    'severity': 'LOW'},
                'created_timestamp': '2015-03-14T09:26:53Z',
                'id': '1',
                'lifecycle_state': 'OPEN',
                'link': 'http://somesite.com/this-alarm-info',
                'links': [{
                    'href': 'http://falconframework.org/v2.0/alarms/1',
                    'rel': 'self'}],
                'metrics': [{
                    'dimensions': {
                        'instance_id': '123',
                        'service': 'monitoring'},
                    'name': 'cpu.idle_perc'}],
                'state': 'OK',
                'state_updated_timestamp': '2015-03-14T09:26:53Z',
                'updated_timestamp': '2015-03-14T09:26:53Z'}]}
        response = self.simulate_request(path='/v2.0/alarms',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='state=OK')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_alarms))

    def test_alarms_get_alarms_severity_parameter(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarms.return_value = \
            [{'alarm_definition_id': '1',
              'metric_dimensions': 'instance_id=123,service=monitoring',
              'alarm_definition_name': '90% CPU',
              'state': 'OK',
              'state_updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'metric_name': 'cpu.idle_perc',
              'link': 'http://somesite.com/this-alarm-info',
              'severity': 'LOW',
              'created_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'alarm_id': '1',
              'lifecycle_state': 'OPEN'}]

        expected_alarms = {
            'elements': [{
                'alarm_definition': {
                    'id': '1',
                    'links': [{
                        'href': 'http://falconframework.org/v2.0/alarm-definitions/1',
                        'rel': 'self'}],
                    'name': '90% CPU',
                    'severity': 'LOW'},
                'created_timestamp': '2015-03-14T09:26:53Z',
                'id': '1',
                'lifecycle_state': 'OPEN',
                'link': 'http://somesite.com/this-alarm-info',
                'links': [{
                    'href': 'http://falconframework.org/v2.0/alarms/1',
                    'rel': 'self'}],
                'metrics': [{
                    'dimensions': {
                        'instance_id': '123',
                        'service': 'monitoring'},
                    'name': 'cpu.idle_perc'}],
                'state': 'OK',
                'state_updated_timestamp': '2015-03-14T09:26:53Z',
                'updated_timestamp': '2015-03-14T09:26:53Z'}]}
        response = self.simulate_request(path='/v2.0/alarms',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='severity=LOW')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_alarms))

    def test_alarms_get_alarms_with_offset(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarms.return_value = \
            [{'alarm_definition_id': '1',
              'metric_dimensions': 'instance_id=123,service=monitoring',
              'alarm_definition_name': '90% CPU',
              'state': 'OK',
              'state_updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'metric_name': 'cpu.idle_perc',
              'link': 'http://somesite.com/this-alarm-info',
              'severity': 'LOW',
              'created_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'alarm_id': '1',
              'lifecycle_state': 'OPEN'}]

        expected_alarms = {
            'elements': [{
                'alarm_definition': {
                    'id': '1',
                    'links': [{
                        'href': 'http://falconframework.org/v2.0/alarm-definitions/1',
                        'rel': 'self'}],
                    'name': '90% CPU',
                    'severity': 'LOW'},
                'created_timestamp': '2015-03-14T09:26:53Z',
                'id': '1',
                'lifecycle_state': 'OPEN',
                'link': 'http://somesite.com/this-alarm-info',
                'links': [{
                    'href': 'http://falconframework.org/v2.0/alarms/1',
                    'rel': 'self'}],
                'metrics': [{
                    'dimensions': {
                        'instance_id': '123',
                        'service': 'monitoring'},
                    'name': 'cpu.idle_perc'}],
                'state': 'OK',
                'state_updated_timestamp': '2015-03-14T09:26:53Z',
                'updated_timestamp': '2015-03-14T09:26:53Z'}]}
        response = self.simulate_request(path='/v2.0/alarms',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='offset=1')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_alarms))

    def test_alarms_get_alarms_with_incorrect_offset(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarms.return_value = \
            [{'alarm_definition_id': '1',
              'metric_dimensions': 'instance_id=123,service=monitoring',
              'alarm_definition_name': '90% CPU',
              'state': 'OK',
              'state_updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'metric_name': 'cpu.idle_perc',
              'link': 'http://somesite.com/this-alarm-info',
              'severity': 'LOW',
              'created_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'alarm_id': '1',
              'lifecycle_state': 'OPEN'}]

        response = self.simulate_request(
            path='/v2.0/alarms',
            headers={'X-Roles': CONF.security.default_authorized_roles[0],
                     'X-Tenant-Id': TENANT_ID},
            method='GET',
            query_string='offset=ninccorect_offset')

        self.assertEqual(response.status, falcon.HTTP_422)

    def test_alarms_get_alarms_sort_by_parameter(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarms.return_value = \
            [{'alarm_definition_id': '1',
              'metric_dimensions': 'instance_id=123,service=monitoring',
              'alarm_definition_name': '90% CPU',
              'state': 'OK',
              'state_updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'metric_name': 'cpu.idle_perc',
              'link': 'http://somesite.com/this-alarm-info',
              'severity': 'LOW',
              'created_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'alarm_id': '1',
              'lifecycle_state': 'OPEN'}]

        expected_alarms = {
            'elements': [{
                'alarm_definition': {
                    'id': '1',
                    'links': [{
                        'href': 'http://falconframework.org/v2.0/alarm-definitions/1',
                        'rel': 'self'}],
                    'name': '90% CPU',
                    'severity': 'LOW'},
                'created_timestamp': '2015-03-14T09:26:53Z',
                'id': '1',
                'lifecycle_state': 'OPEN',
                'link': 'http://somesite.com/this-alarm-info',
                'links': [{
                    'href': 'http://falconframework.org/v2.0/alarms/1',
                    'rel': 'self'}],
                'metrics': [{
                    'dimensions': {
                        'instance_id': '123',
                        'service': 'monitoring'},
                    'name': 'cpu.idle_perc'}],
                'state': 'OK',
                'state_updated_timestamp': '2015-03-14T09:26:53Z',
                'updated_timestamp': '2015-03-14T09:26:53Z'}]}
        response = self.simulate_request(path='/v2.0/alarms',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='sort_by=alarm_id')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_alarms))

    def test_alarms_get_alarms_incorrect_sort_by_parameter(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarms.return_value = \
            [{'alarm_definition_id': '1',
              'metric_dimensions': 'instance_id=123,service=monitoring',
              'alarm_definition_name': '90% CPU',
              'state': 'OK',
              'state_updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'metric_name': 'cpu.idle_perc',
              'link': 'http://somesite.com/this-alarm-info',
              'severity': 'LOW',
              'created_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'updated_timestamp': datetime.datetime(2015, 3, 14, 9, 26, 53),
              'alarm_id': '1',
              'lifecycle_state': 'OPEN'}]

        response = self.simulate_request(path='/v2.0/alarms',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET',
                                         query_string='sort_by=random_string')

        self.assertEqual(response.status, falcon.HTTP_422)

    def test_alarms_delete_alarms(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarm_metrics.return_value = \
            [{'alarm_id': u'2',
              'name': u'cpu.idle_perc',
              'dimensions': u'instance_id=123,service=monitoring'}]
        return_value.get_sub_alarms.return_value = \
            [{'sub_alarm_id': u'1',
              'alarm_id': u'2',
              'expression': u'avg(cpu.idle_perc{instance_id=123, service=monitoring}) > 10',
              'alarm_definition_id': u'1'}]
        response = self.simulate_request(path='/v2.0/alarms/2',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='DELETE')
        self.assertEqual(response.status, falcon.HTTP_204)

    def test_alarms_put(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarm_metrics.return_value = \
            [{'alarm_id': u'2',
              'name': u'cpu.idle_perc',
              'dimensions': u'instance_id=123,service=monitoring'}]
        return_value.get_sub_alarms.return_value = \
            [{'sub_alarm_id': u'1',
              'alarm_id': u'2',
              'expression': u'avg(cpu.idle_perc{instance_id=123, service=monitoring}) > 10',
              'alarm_definition_id': u'1'}]
        return_value.update_alarm.return_value = \
            ({'state': u'UNDETERMINED',
              'link': u'http://somesite.com/this-alarm-info',
              'lifecycle_state': u'OPEN'},
             1550835096962)
        return_value.get_alarm_definition.return_value = \
            {'description': None,
             'tenant_id': u'bob',
             'created_at': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
             'updated_at': datetime.datetime(2019, 2, 22, 12, 44, 25, 850963),
             'name': u'90% CPU',
             'actions_enabled': False,
             'match_by': None,
             'deleted_at': None,
             'expression': u'avg(cpu.idle_perc{instance_id=123, service=monitoring}) > 10',
             'id': u'1',
             'severity': u'LOW'}
        return_value.get_alarm.return_value = \
            [{'alarm_definition_id': '1',
              'metric_dimensions': 'instance_id=123,service=monitoring',
              'alarm_definition_name': '90% CPU',
              'state': 'OK',
              'state_updated_timestamp': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
              'metric_name': 'cpu.idle_perc',
              'link': 'http://somesite.com/this-alarm-info',
              'severity': 'LOW',
              'created_timestamp': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
              'updated_timestamp': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
              'alarm_id': '1',
              'lifecycle_state': 'ALARM'}]
        alarm_new_fields = {'state': 'ALARM',
                            'lifecycle_state': 'OPEN',
                            'link': 'http://somesite.com/this-alarm-info'}

        expected_alarm = {u'alarm_definition': {u'id': u'1',
                                                u'links': [
                                                    {u'href': u'http://falconframework.org'
                                                              u'/v2.0/alarm-definitions/1',
                                                     u'rel': u'self'}],
                                                u'name': u'90% CPU',
                                                u'severity': u'LOW'},
                          u'created_timestamp': u'2019-02-22T12:44:25.850947Z',
                          u'id': u'1',
                          u'lifecycle_state': u'ALARM',
                          u'link': u'http://somesite.com/this-alarm-info',
                          u'metrics': [{u'dimensions': {u'instance_id': u'123',
                                                        u'service': u'monitoring'},
                                        u'name': u'cpu.idle_perc'}],
                          u'state': u'OK',
                          u'state_updated_timestamp': u'2019-02-22T12:44:25.850947Z',
                          u'updated_timestamp': u'2019-02-22T12:44:25.850947Z'}

        response = self.simulate_request(path='/v2.0/alarms/2',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='PUT',
                                         body=json.dumps(alarm_new_fields))

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_alarm))

    def test_alarms_put_without_link(self):
        alarm_new_fields = {'state': 'ALARM',
                            'lifecycle_state': 'OPEN'}
        expected_response = {u'description': u"Field 'link' is required",
                             u'title': u'Unprocessable Entity'}
        response = self.simulate_request(path='/v2.0/alarms/2',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='PUT',
                                         body=json.dumps(alarm_new_fields))

        self.assertEqual(response.status, falcon.HTTP_422)
        self.assertThat(response, RESTResponseEquals(expected_response))

    def test_alarms_put_without_lifecycle_state(self):
        alarm_new_fields = {'state': 'ALARM',
                            'link': 'http://somesite.com/this-alarm-info'}
        expected_response = {u'description': u"Field 'lifecycle_state' is required",
                             u'title': u'Unprocessable Entity'}
        response = self.simulate_request(path='/v2.0/alarms/2',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='PUT',
                                         body=json.dumps(alarm_new_fields))

        self.assertEqual(response.status, falcon.HTTP_422)
        self.assertThat(response, RESTResponseEquals(expected_response))

    def test_alarms_put_without_state(self):
        alarm_new_fields = {'lifecycle_state': 'OPEN',
                            'link': 'http://somesite.com/this-alarm-info'}
        expected_response = {u'description': u"Field 'state' is required",
                             u'title': u'Unprocessable Entity'}
        response = self.simulate_request(path='/v2.0/alarms/2',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='PUT',
                                         body=json.dumps(alarm_new_fields))

        self.assertEqual(response.status, falcon.HTTP_422)
        self.assertThat(response, RESTResponseEquals(expected_response))

    def test_alarms_patch(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarm_metrics.return_value = \
            [{'alarm_id': u'2',
              'name': u'cpu.idle_perc',
              'dimensions': u'instance_id=123,service=monitoring'}]
        return_value.get_sub_alarms.return_value = \
            [{'sub_alarm_id': u'1',
              'alarm_id': u'2',
              'expression': u'avg(cpu.idle_perc{instance_id=123, service=monitoring}) > 10',
              'alarm_definition_id': u'1'}]
        return_value.update_alarm.return_value = \
            ({'state': u'UNDETERMINED',
              'link': u'http://somesite.com/this-alarm-info',
              'lifecycle_state': u'OPEN'},
             1550835096962)
        return_value.get_alarm_definition.return_value = \
            {'description': None,
             'tenant_id': u'bob',
             'created_at': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
             'updated_at': datetime.datetime(2019, 2, 22, 12, 44, 25, 850963),
             'name': u'90% CPU',
             'actions_enabled': False,
             'match_by': None,
             'deleted_at': None,
             'expression': u'avg(cpu.idle_perc{instance_id=123, service=monitoring}) > 10',
             'id': u'1',
             'severity': u'LOW'}
        return_value.get_alarm.return_value = \
            [{'alarm_definition_id': '1',
              'metric_dimensions': 'instance_id=123,service=monitoring',
              'alarm_definition_name': '90% CPU',
              'state': 'OK',
              'state_updated_timestamp': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
              'metric_name': 'cpu.idle_perc',
              'link': 'http://somesite.com/this-alarm-info',
              'severity': 'LOW',
              'created_timestamp': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
              'updated_timestamp': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
              'alarm_id': '1',
              'lifecycle_state': 'ALARM'}]
        alarm_new_fields = {'state': 'ALARM',
                            'lifecycle_state': 'OPEN',
                            'link': 'http://somesite.com/this-alarm-info'}

        expected_alarm = {u'alarm_definition': {u'id': u'1',
                                                u'links': [
                                                    {u'href': u'http://falconframework.org'
                                                              u'/v2.0/alarm-definitions/1',
                                                     u'rel': u'self'}],
                                                u'name': u'90% CPU',
                                                u'severity': u'LOW'},
                          u'created_timestamp': u'2019-02-22T12:44:25.850947Z',
                          u'id': u'1',
                          u'lifecycle_state': u'ALARM',
                          u'link': u'http://somesite.com/this-alarm-info',
                          u'metrics': [{u'dimensions': {u'instance_id': u'123',
                                                        u'service': u'monitoring'},
                                        u'name': u'cpu.idle_perc'}],
                          u'state': u'OK',
                          u'state_updated_timestamp': u'2019-02-22T12:44:25.850947Z',
                          u'updated_timestamp': u'2019-02-22T12:44:25.850947Z'}

        response = self.simulate_request(path='/v2.0/alarms/2',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='PATCH',
                                         body=json.dumps(alarm_new_fields))

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_alarm))

    def test_alarms_patch_without_new_fields(self):
        return_value = self.alarms_repo_mock.return_value
        return_value.get_alarm_metrics.return_value = \
            [{'alarm_id': u'2',
              'name': u'cpu.idle_perc',
              'dimensions': u'instance_id=123,service=monitoring'}]
        return_value.get_sub_alarms.return_value = \
            [{'sub_alarm_id': u'1',
              'alarm_id': u'2',
              'expression': u'avg(cpu.idle_perc{instance_id=123, service=monitoring}) > 10',
              'alarm_definition_id': u'1'}]
        return_value.update_alarm.return_value = \
            ({'state': u'UNDETERMINED',
              'link': u'http://somesite.com/this-alarm-info',
              'lifecycle_state': u'OPEN'},
             1550835096962)
        return_value.get_alarm_definition.return_value = \
            {'description': None,
             'tenant_id': u'bob',
             'created_at': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
             'updated_at': datetime.datetime(2019, 2, 22, 12, 44, 25, 850963),
             'name': u'90% CPU',
             'actions_enabled': False,
             'match_by': None,
             'deleted_at': None,
             'expression': u'avg(cpu.idle_perc{instance_id=123, service=monitoring}) > 10',
             'id': u'1',
             'severity': u'LOW'}
        return_value.get_alarm.return_value = \
            [{'alarm_definition_id': '1',
              'metric_dimensions': 'instance_id=123,service=monitoring',
              'alarm_definition_name': '90% CPU',
              'state': 'OK',
              'state_updated_timestamp': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
              'metric_name': 'cpu.idle_perc',
              'link': 'http://somesite.com/this-alarm-info',
              'severity': 'LOW',
              'created_timestamp': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
              'updated_timestamp': datetime.datetime(2019, 2, 22, 12, 44, 25, 850947),
              'alarm_id': '1',
              'lifecycle_state': 'ALARM'}]
        alarm_new_fields = {}

        expected_alarm = {u'alarm_definition': {u'id': u'1',
                                                u'links': [
                                                    {u'href': u'http://falconframework.org'
                                                              u'/v2.0/alarm-definitions/1',
                                                     u'rel': u'self'}],
                                                u'name': u'90% CPU',
                                                u'severity': u'LOW'},
                          u'created_timestamp': u'2019-02-22T12:44:25.850947Z',
                          u'id': u'1',
                          u'lifecycle_state': u'ALARM',
                          u'link': u'http://somesite.com/this-alarm-info',
                          u'metrics': [{u'dimensions': {u'instance_id': u'123',
                                                        u'service': u'monitoring'},
                                        u'name': u'cpu.idle_perc'}],
                          u'state': u'OK',
                          u'state_updated_timestamp': u'2019-02-22T12:44:25.850947Z',
                          u'updated_timestamp': u'2019-02-22T12:44:25.850947Z'}

        response = self.simulate_request(path='/v2.0/alarms/2',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='PATCH',
                                         body=json.dumps(alarm_new_fields))

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_alarm))


class TestAlarmDefinition(AlarmTestBase):
    def setUp(self):
        super(TestAlarmDefinition, self).setUp()

        self.alarm_def_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.'
            'alarm_definitions_repository.AlarmDefinitionsRepository'
        )).mock

        self.alarm_definition_resource = alarm_definitions.AlarmDefinitions()
        self.alarm_definition_resource.send_event = Mock()
        self._send_event = self.alarm_definition_resource.send_event

        self.app.add_route("/v2.0/alarm-definitions",
                           self.alarm_definition_resource)
        self.app.add_route("/v2.0/alarm-definitions/{alarm_definition_id}",
                           self.alarm_definition_resource)

    def test_alarm_definition_create(self):
        return_value = self.alarm_def_repo_mock.return_value
        return_value.get_alarm_definitions.return_value = []
        return_value.create_alarm_definition.return_value = u"00000001-0001-0001-0001-000000000001"

        alarm_def = {
            "name": "Test Definition",
            "expression": "test.metric > 10"
        }

        expected_data = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [],
            u'name': u'Test Definition',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'expression': u'test.metric > 10',
            u'deterministic': False,
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'severity': u'LOW',
        }

        response = self.simulate_request(path="/v2.0/alarm-definitions/",
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method="POST",
                                         body=json.dumps(alarm_def))

        self.assertEqual(response.status, falcon.HTTP_201)
        self.assertThat(response, RESTResponseEquals(expected_data))

    def test_alarm_definition_create_with_valid_expressions(self):
        return_value = self.alarm_def_repo_mock.return_value
        return_value.get_alarm_definitions.return_value = []
        return_value.create_alarm_definition.return_value = u"00000001-0001-0001-0001-000000000001"

        valid_expressions = [
            u"max(-_.千幸福的笑脸{घोड़ा=馬,  "
            u"dn2=dv2,千幸福的笑脸घ=千幸福的笑脸घ}) gte 100 "
            u"times 3 && "
            u"(min(ເຮືອນ{dn3=dv3,家=дом}) < 10 or sum(biz{dn5=dv5}) >99 and "
            u"count(fizzle) lt 0or count(baz) > 1)",

            u"max(foo{hostname=mini-mon,千=千}, 120) > 100 and (max(bar)>100 "
            u" or max(biz)>100)",

            u"max(foo)>=100",

            u"test_metric{this=that, that =  this} < 1",

            u"max  (  3test_metric5  {  this  =  that  })  lt  5 times    3",

            u"3test_metric5 lt 3",

            u"ntp.offset > 1 or ntp.offset < -5",
        ]

        alarm_def = {
            u'name': u'Test Definition',
            u'expression': u'test.metric > 10'
        }

        expected_data = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [],
            u'name': u'Test Definition',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'expression': u'test.metric > 10',
            u'deterministic': False,
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'severity': u'LOW',
        }

        for expression in valid_expressions:
            alarm_def[u'expression'] = expression
            expected_data[u'expression'] = expression
            response = self.simulate_request(path="/v2.0/alarm-definitions/",
                                             headers={'X-Roles':
                                                      CONF.security.default_authorized_roles[0],
                                                      'X-Tenant-Id': TENANT_ID},
                                             method="POST",
                                             body=json.dumps(alarm_def))

            self.assertEqual(response.status, falcon.HTTP_201,
                             u'Expression {} should have passed'.format(expression))
            self.assertThat(response, RESTResponseEquals(expected_data))

    def test_alarm_definition_create_with_invalid_expressions(self):
        bad_expressions = [
            "test=metric > 10",
            "test.metric{dim=this=that} > 10",
            "test_metric(5) > 2"
            "test_metric > 10 and or alt_metric > 10"
        ]

        alarm_def = {
            u'name': 'Test Definition',
            u'expression': 'test.metric > 10'
        }

        for expression in bad_expressions:
            alarm_def[u'expression'] = expression
            response = self.simulate_request(
                path="/v2.0/alarm-definitions/",
                headers={'X-Roles': CONF.security.default_authorized_roles[0],
                         'X-Tenant-Id': TENANT_ID},
                method="POST",
                body=json.dumps(alarm_def))

            self.assertEqual(response.status, '422 Unprocessable Entity',
                             u'Expression {} should have failed'.format(expression))

    def test_alarm_definition_create_with_occupied_alarm_definition_name(self):
        self.alarm_def_repo_mock.return_value.get_alarm_definitions.return_value = [{
            'alarm_actions': None,
            'ok_actions': None,
            'description': None,
            'match_by': u'hostname',
            'name': u'Test Alarm',
            'actions_enabled': 1,
            'undetermined_actions': None,
            'expression': u'max(test.metric{hostname=host}) gte 1',
            'id': u'00000001-0001-0001-0001-000000000001',
            'severity': u'LOW'
        }]
        alarm_def = {
            u'name': u'Test Definition',
            u'expression': u'max(test.metric{hostname=host}) gte 1'
        }
        response = self.simulate_request(
            path="/v2.0/alarm-definitions/",
            headers={'X-Roles': CONF.security.default_authorized_roles[0],
                     'X-Tenant-Id': TENANT_ID},
            method="POST",
            body=json.dumps(alarm_def))
        self.assertEqual(response.status, falcon.HTTP_409)

    def test_alarm_definition_update(self):
        self.alarm_def_repo_mock.return_value.get_alarm_definitions.return_value = []
        self.alarm_def_repo_mock.return_value.update_or_patch_alarm_definition.return_value = (
            {u'alarm_actions': [],
             u'ok_actions': [],
             u'description': u'Non-ASCII character: \u2603',
             u'match_by': u'hostname',
             u'name': u'Test Alarm',
             u'actions_enabled': True,
             u'undetermined_actions': [],
             u'is_deterministic': False,
             u'expression': u'max(test.metric{hostname=host}) gte 1',
             u'id': u'00000001-0001-0001-0001-000000000001',
             u'severity': u'LOW'},
            {'old': {'11111': sub_alarm_definition.SubAlarmDefinition(
                row={'id': '11111',
                     'alarm_definition_id': u'00000001-0001-0001-0001-000000000001',
                     'function': 'max',
                     'metric_name': 'test.metric',
                     'dimensions': 'hostname=host',
                     'operator': 'gte',
                     'threshold': 1,
                     'period': 60,
                     'is_deterministic': False,
                     'periods': 1})},
                'changed': {},
                'new': {},
                'unchanged': {'11111': sub_alarm_definition.SubAlarmDefinition(
                    row={'id': '11111',
                         'alarm_definition_id': u'00000001-0001-0001-0001-000000000001',
                         'function': 'max',
                         'metric_name': 'test.metric',
                         'dimensions': 'hostname=host',
                         'operator': 'gte',
                         'threshold': 1,
                         'period': 60,
                         'is_deterministic': False,
                         'periods': 1})}})

        expected_def = {
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'Non-ASCII character: \u2603',
            u'links': [{u'href': u'http://falconframework.org/v2.0/alarm-definitions/'
                                 u'00000001-0001-0001-0001-000000000001',
                        u'rel': u'self'}],
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW',
        }

        alarm_def = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW',
        }

        result = self.simulate_request(path="/v2.0/alarm-definitions/%s" % expected_def[u'id'],
                                       headers={'X-Roles':
                                                CONF.security.default_authorized_roles[0],
                                                'X-Tenant-Id': TENANT_ID},
                                       method="PUT",
                                       body=json.dumps(alarm_def))

        self.assertEqual(result.status, falcon.HTTP_200)
        result_def = result.json
        self.assertEqual(result_def, expected_def)

    def test_alarm_definition_patch_incorrect_id(self):
        self.alarm_def_repo_mock.return_value.get_alarm_definitions.return_value = [{
            'alarm_actions': None,
            'ok_actions': None,
            'description': None,
            'match_by': u'hostname',
            'name': u'Test Alarm',
            'actions_enabled': 1,
            'undetermined_actions': None,
            'expression': u'max(test.metric{hostname=host}) gte 1',
            'id': u'00000001-0001-0001-0001-000000000001',
            'severity': u'LOW'
        }]
        alarm_def = {
            u'name': u'Test Alarm Definition Updated',
        }
        response = self.simulate_request(
            path="/v2.0/alarm-definitions/9999999-0001-0001-0001-000000000001",
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID},
            method="PATCH",
            body=json.dumps(alarm_def))

        self.assertEqual(response.status, falcon.HTTP_409)

    def test_alarm_definition_put_incorrect_period_value(self):
        self.alarm_def_repo_mock.return_value.get_alarm_definitions.return_value = []
        period = 'times 0'
        alarm_def = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1 ' + period,
            u'severity': u'LOW',
        }

        response = self.simulate_request(
            path="/v2.0/alarm-definitions/00000001-0001-0001-0001-000000000001",
            headers={'X-Roles': CONF.security.default_authorized_roles[0],
                     'X-Tenant-Id': TENANT_ID},
            method="PUT",
            body=json.dumps(alarm_def))

        self.assertEqual(response.status, falcon.HTTP_422)

    def test_alarm_definition_patch_no_id(self):
        alarm_def = {
            u'name': u'Test Alarm Definition Updated',
        }

        response = self.simulate_request(
            path="/v2.0/alarm-definitions/",
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID},
            method="PATCH",
            body=json.dumps(alarm_def))

        self.assertEqual(response.status, falcon.HTTP_400)

    def test_alarm_definition_update_no_id(self):
        alarm_def = {
            u'name': u'Test Alarm Definition Updated',
        }

        response = self.simulate_request(
            path="/v2.0/alarm-definitions/",
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID},
            method="PUT",
            body=json.dumps(alarm_def))

        self.assertEqual(response.status, falcon.HTTP_400)

    def test_alarm_definition_delete(self):

        self.alarm_def_repo_mock.return_value.get_get_sub_alarm_definitions.return_value = [{
            'alarm_definition_id': '123',
            'dimensions': 'flavor_id=777',
            'function': 'AVG',
            'id': '111',
            'metric_name': 'cpu.idle_perc',
            'operator': 'GT',
            'period': 60,
            'periods': 1,
            'is_deterministic': False,
            'threshold': 10.0}]
        self.alarm_def_repo_mock.return_value.get_alarm_metrics.return_value = [{
            'alarm_id': '1',
            'dimensions': 'flavor_id=777',
            'name': 'cpu.idle_perc'}]
        self.alarm_def_repo_mock.return_value.get_sub_alarms.return_value = [{
            'alarm_definition_id': '1',
            'alarm_id': '2',
            'expression': 'avg(cpu.idle_perc{flavor_id=777}) > 10',
            'sub_alarm_id': '43'}]
        self.alarm_def_repo_mock.return_value.delete_alarm_definition.return_value = True

        response = self.simulate_request(
            path='/v2.0/alarm-definitions/00000001-0001-0001-0001-000000000001',
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID},
            method='DELETE')

        self.assertEqual(response.status, falcon.HTTP_204)

    def test_alarm_definition_delete_alarm_definition_not_exist(self):
        self.alarm_def_repo_mock.return_value.get_get_sub_alarm_definitions.return_value = []
        self.alarm_def_repo_mock.return_value.get_alarm_metrics.return_value = []
        self.alarm_def_repo_mock.return_value.get_sub_alarms.return_value = []
        self.alarm_def_repo_mock.return_value.delete_alarm_definition.return_value = False

        response = self.simulate_request(
            path='/v2.0/alarm-definitions/00000001-0001-0001-0001-000000000001',
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID},
            method='DELETE')

        self.assertEqual(response.status, falcon.HTTP_404)

    def test_alarm_definition_delete_no_id(self):

        response = self.simulate_request(
            path="/v2.0/alarm-definitions/",
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID},
            method="DELETE")

        self.assertEqual(response.status, falcon.HTTP_400)

    def test_alarm_definition_patch(self):
        self.alarm_def_repo_mock.return_value.get_alarm_definitions.return_value = []
        description = u'Non-ASCII character: \u2603'
        new_name = u'Test Alarm Updated'
        actions_enabled = True
        alarm_def_id = u'00000001-0001-0001-0001-000000000001'
        alarm_expression = u'max(test.metric{hostname=host}) gte 1'
        severity = u'LOW'
        match_by = u'hostname'
        self.alarm_def_repo_mock.return_value.update_or_patch_alarm_definition.return_value = (
            {u'alarm_actions': [],
             u'ok_actions': [],
             u'description': description,
             u'match_by': match_by,
             u'name': new_name,
             u'actions_enabled': actions_enabled,
             u'undetermined_actions': [],
             u'is_deterministic': False,
             u'expression': alarm_expression,
             u'id': alarm_def_id,
             u'severity': severity},
            {'old': {'11111': sub_alarm_definition.SubAlarmDefinition(
                row={'id': '11111',
                     'alarm_definition_id': u'00000001-0001-0001-0001-000000000001',
                     'function': 'max',
                     'metric_name': 'test.metric',
                     'dimensions': 'hostname=host',
                     'operator': 'gte',
                     'threshold': 1,
                     'period': 60,
                     'is_deterministic': False,
                     'periods': 1})},
                'changed': {},
                'new': {},
                'unchanged': {'11111': sub_alarm_definition.SubAlarmDefinition(
                    row={'id': '11111',
                         'alarm_definition_id': u'00000001-0001-0001-0001-000000000001',
                         'function': 'max',
                         'metric_name': 'test.metric',
                         'dimensions': 'hostname=host',
                         'operator': 'gte',
                         'threshold': 1,
                         'period': 60,
                         'is_deterministic': False,
                         'periods': 1})}})

        expected_def = {
            u'id': alarm_def_id,
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': description,
            u'links': [{u'href': u'http://falconframework.org/v2.0/alarm-definitions/'
                                 u'00000001-0001-0001-0001-000000000001',
                        u'rel': u'self'}],
            u'match_by': [match_by],
            u'name': new_name,
            u'actions_enabled': actions_enabled,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': alarm_expression,
            u'severity': severity,
        }

        alarm_def = {
            u'name': u'Test Alarm Updated',
        }

        result = self.simulate_request(path="/v2.0/alarm-definitions/%s" % expected_def[u'id'],
                                       headers={'X-Roles':
                                                CONF.security.default_authorized_roles[0],
                                                'X-Tenant-Id': TENANT_ID},
                                       method="PATCH",
                                       body=json.dumps(alarm_def))

        self.assertEqual(result.status, falcon.HTTP_200)
        result_def = result.json
        self.assertEqual(result_def, expected_def)
        # If the alarm-definition-updated event does not have all of the
        # fields set, the Threshold Engine will get confused. For example,
        # if alarmActionsEnabled is none, thresh will read that as false
        # and pass that value onto the Notification Engine which will not
        # create a notification even actions_enabled is True in the
        # database. So, ensure all fields are set correctly
        ((_, event), _) = self._send_event.call_args
        expr = u'max(test.metric{hostname=host}, 60) gte 1 times 1'
        sub_expression = {'11111': {u'expression': expr,
                                    u'function': 'max',
                                    u'metricDefinition': {
                                        u'dimensions': {'hostname': 'host'},
                                        u'name': 'test.metric'},
                                    u'operator': 'gte',
                                    u'period': 60,
                                    u'periods': 1,
                                    u'threshold': 1}}
        fields = {u'alarmActionsEnabled': actions_enabled,
                  u'alarmDefinitionId': alarm_def_id,
                  u'alarmDescription': description,
                  u'alarmExpression': alarm_expression,
                  u'alarmName': new_name,
                  u'changedSubExpressions': {},
                  u'matchBy': [match_by],
                  u'severity': severity,
                  u'tenantId': u'fedcba9876543210fedcba9876543210',
                  u'newAlarmSubExpressions': {},
                  u'oldAlarmSubExpressions': sub_expression,
                  u'unchangedSubExpressions': sub_expression}
        reference = {u'alarm-definition-updated': fields}
        self.assertEqual(reference, event)

    def test_alarm_definition_update_missing_fields(self):
        self.alarm_def_repo_mock.return_value.get_alarm_definitions.return_value = []
        self.alarm_def_repo_mock.return_value.update_or_patch_alarm_definition.return_value = (
            {u'alarm_actions': [],
             u'ok_actions': [],
             u'description': u'Non-ASCII character: \u2603',
             u'match_by': u'hostname',
             u'name': u'Test Alarm',
             u'actions_enabled': True,
             u'undetermined_actions': [],
             u'expression': u'max(test.metric{hostname=host}) gte 1',
             u'id': u'00000001-0001-0001-0001-000000000001',
             u'is_deterministic': False,
             u'severity': u'LOW'},
            {'old': {'11111': sub_alarm_definition.SubAlarmDefinition(
                row={'id': '11111',
                     'alarm_definition_id': u'00000001-0001-0001-0001-000000000001',
                     'function': 'max',
                     'metric_name': 'test.metric',
                     'dimensions': 'hostname=host',
                     'operator': 'gte',
                     'threshold': 1,
                     'period': 60,
                     'periods': 1,
                     'is_deterministic': False})},
                'changed': {},
                'new': {},
                'unchanged': {'11111': sub_alarm_definition.SubAlarmDefinition(
                    row={'id': '11111',
                         'alarm_definition_id': u'00000001-0001-0001-0001-000000000001',
                         'function': 'max',
                         'metric_name': 'test.metric',
                         'dimensions': 'hostname=host',
                         'operator': 'gte',
                         'threshold': 1,
                         'period': 60,
                         'periods': 1,
                         'is_deterministic': False})}})

        expected_def = {
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'Non-ASCII character: \u2603',
            u'links': [{u'href': u'http://falconframework.org/v2.0/alarm-definitions/'
                                 u'00000001-0001-0001-0001-000000000001',
                        u'rel': u'self'}],
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW',
            u'deterministic': False
        }

        alarm_def = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW'
        }

        result = self.simulate_request(path="/v2.0/alarm-definitions/%s" % expected_def[u'id'],
                                       headers={'X-Roles':
                                                CONF.security.default_authorized_roles[0],
                                                'X-Tenant-Id': TENANT_ID},
                                       method="PUT",
                                       body=json.dumps(alarm_def))

        self.assertEqual(result.status, falcon.HTTP_200)
        result_def = result.json
        self.assertEqual(result_def, expected_def)

        for key, value in list(alarm_def.items()):
            del alarm_def[key]

            response = self.simulate_request(
                path="/v2.0/alarm-definitions/%s" % expected_def[u'id'],
                headers={'X-Roles':
                         CONF.security.default_authorized_roles[0],
                         'X-Tenant-Id': TENANT_ID},
                method="PUT",
                body=json.dumps(alarm_def))
            self.assertEqual(response.status, "422 Unprocessable Entity",
                             u"should have failed without key {}".format(key))
            alarm_def[key] = value

    def test_alarm_definition_get_specific_alarm(self):

        self.alarm_def_repo_mock.return_value.get_alarm_definition.return_value = {
            'alarm_actions': None,
            'ok_actions': None,
            # The description field was decoded to unicode when the
            # alarm_definition was created.
            'description': u'Non-ASCII character: \u2603',
            'match_by': u'hostname',
            'name': u'Test Alarm',
            'actions_enabled': 1,
            'undetermined_actions': None,
            'deterministic': False,
            'expression': u'max(test.metric{hostname=host}) gte 1',
            'id': u'00000001-0001-0001-0001-000000000001',
            'severity': u'LOW'
        }

        expected_data = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'Non-ASCII character: \u2603',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'severity': u'LOW',
        }

        response = self.simulate_request(
            path='/v2.0/alarm-definitions/%s' % (expected_data[u'id']),
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID,
            })

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_data))

    def test_alarm_definition_get_specific_alarm_description_none(self):

        self.alarm_def_repo_mock.return_value.get_alarm_definition.return_value = {
            'alarm_actions': None,
            'ok_actions': None,
            'description': None,
            'match_by': u'hostname',
            'name': u'Test Alarm',
            'actions_enabled': 1,
            'undetermined_actions': None,
            'expression': u'max(test.metric{hostname=host}) gte 1',
            'id': u'00000001-0001-0001-0001-000000000001',
            'severity': u'LOW'
        }

        expected_data = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': None,
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'severity': u'LOW',
        }

        response = self.simulate_request(
            path='/v2.0/alarm-definitions/%s' % (expected_data[u'id']),
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID,
            })

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_data))

    def test_get_alarm_definitions_with_multibyte_character(self):
        def_name = 'ａｌａｒｍ＿ｄｅｆｉｎｉｔｉｏｎ'
        if six.PY2:
            def_name = def_name.decode('utf8')

        expected_data = {
            u'alarm_actions': [], u'ok_actions': [],
            u'description': None, u'match_by': [u'hostname'],
            u'actions_enabled': True, u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'severity': u'LOW', u'name': def_name
        }

        self.alarm_def_repo_mock.return_value.get_alarm_definition.return_value = {
            'alarm_actions': None,
            'ok_actions': None,
            'description': None,
            'match_by': u'hostname',
            'name': def_name,
            'actions_enabled': 1,
            'undetermined_actions': None,
            'expression': u'max(test.metric{hostname=host}) gte 1',
            'id': u'00000001-0001-0001-0001-000000000001',
            'severity': u'LOW'
        }

        response = self.simulate_request(
            path='/v2.0/alarm-definitions/%s' % (expected_data[u'id']),
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID,
            }
        )
        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_data))

    def test_alarm_definition_get_alarm_definition_list(self):
        self.alarm_def_repo_mock.return_value.get_alarm_definitions.return_value = [{
            'alarm_actions': None,
            'ok_actions': None,
            'description': None,
            'match_by': u'hostname',
            'name': u'Test Alarm',
            'actions_enabled': 1,
            'undetermined_actions': None,
            'expression': u'max(test.metric{hostname=host}) gte 1',
            'id': u'00000001-0001-0001-0001-000000000001',
            'severity': u'LOW'
        }]
        link = 'http://falconframework.org/v2.0/alarm-definitions/' \
               '00000001-0001-0001-0001-000000000001'
        expected_data = {
            u'elements': [{
                u'alarm_actions': [],
                u'ok_actions': [],
                u'description': '',
                u'match_by': [u'hostname'],
                u'name': u'Test Alarm',
                u'actions_enabled': True,
                u'undetermined_actions': [],
                u'deterministic': False,
                u'expression': u'max(test.metric{hostname=host}) gte 1',
                u'id': u'00000001-0001-0001-0001-000000000001',
                'links': [{
                    'href': link,
                    'rel': 'self'}],
                u'severity': u'LOW'}]
        }

        response = self.simulate_request(
            path='/v2.0/alarm-definitions',
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID
            },
            query_string='name=Test Alarm')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_data))

        response = self.simulate_request(
            path='/v2.0/alarm-definitions',
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID
            },
            query_string='sort_by=name')

        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_data))

        response = self.simulate_request(
            path='/v2.0/alarm-definitions',
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID
            },
            query_string='severity=LOW')
        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_data))

        response = self.simulate_request(
            path='/v2.0/alarm-definitions',
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID
            },
            query_string='offset=1')
        self.assertEqual(response.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_data))

    def test_alarm_definition_get_alarm_definition_list_incorrect(self):
        response = self.simulate_request(
            path='/v2.0/alarm-definitions',
            headers={
                'X-Roles': CONF.security.default_authorized_roles[0],
                'X-Tenant-Id': TENANT_ID
            },
            query_string='offset=definitelyNotINT')
        self.assertEqual(response.status, falcon.HTTP_422)

    def test_alarm_definition_get_query_alarm_definition_name(self):
        alarm_def = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW',
        }
        name = alarm_definitions.get_query_alarm_definition_name(alarm_def)
        self.assertEqual(alarm_def['name'], name)

        alarm_def.pop('name')

        self.assertRaises(HTTPUnprocessableEntityError,
                          alarm_definitions.get_query_alarm_definition_name,
                          alarm_def)

        name = alarm_definitions.get_query_alarm_definition_name(alarm_def, return_none=True)
        self.assertIsNone(name)

    def test_alarm_definition_get_query_alarm_definition_expression(self):
        alarm_def = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW',
        }
        expression = alarm_definitions.get_query_alarm_definition_expression(alarm_def)
        self.assertEqual(alarm_def['expression'], expression)

        alarm_def.pop('expression')

        self.assertRaises(HTTPUnprocessableEntityError,
                          alarm_definitions.get_query_alarm_definition_expression,
                          alarm_def)

        expression = alarm_definitions.get_query_alarm_definition_expression(alarm_def,
                                                                             return_none=True)
        self.assertIsNone(expression)

    def test_alarm_definition_get_query_alarm_definition_description(self):
        alarm_def = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'Short description',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW',
        }
        description = alarm_definitions.get_query_alarm_definition_description(alarm_def)
        self.assertEqual(alarm_def['description'], description)

        alarm_def.pop('description')

        description = alarm_definitions.get_query_alarm_definition_description(alarm_def)
        self.assertEqual('', description)

        description = alarm_definitions.get_query_alarm_definition_description(alarm_def,
                                                                               return_none=True)
        self.assertIsNone(description)

    def test_alarm_definition_get_query_alarm_definition_severity(self):
        alarm_def = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'CRITICAL',
        }
        severity = alarm_definitions.get_query_alarm_definition_severity(alarm_def)
        self.assertEqual(alarm_def['severity'], severity)

        alarm_def['severity'] = u'Why so serious'
        self.assertRaises(HTTPUnprocessableEntityError,
                          alarm_definitions.get_query_alarm_definition_severity,
                          alarm_def)

        alarm_def.pop('severity')
        severity = alarm_definitions.get_query_alarm_definition_severity(alarm_def)
        self.assertEqual('LOW', severity)

        severity = alarm_definitions.get_query_alarm_definition_severity(alarm_def,
                                                                         return_none=True)
        self.assertIsNone(severity)

    def test_alarm_definition_get_query_alarm_definition_match_by(self):
        alarm_def = {
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW',
        }
        match_by = alarm_definitions.get_query_alarm_definition_match_by(alarm_def)
        self.assertEqual(alarm_def['match_by'], match_by)

        alarm_def.pop('match_by')

        match_by = alarm_definitions.get_query_alarm_definition_match_by(alarm_def)
        self.assertEqual([], match_by)

        expression = alarm_definitions.get_query_alarm_definition_match_by(alarm_def,
                                                                           return_none=True)
        self.assertIsNone(expression)

    def test_alarm_definition_get_query_alarm_definition_alarm_actions(self):
        alarm_def = {
            u'alarm_actions': 'c60ec47e-5038-4bf1-9f95-4046c6e9a759',
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW',
        }
        alarm_actions = alarm_definitions.get_query_alarm_definition_alarm_actions(alarm_def)
        self.assertEqual(alarm_def['alarm_actions'], alarm_actions)

        alarm_def.pop('alarm_actions')

        alarm_actions = alarm_definitions.get_query_alarm_definition_alarm_actions(alarm_def)
        self.assertEqual([], alarm_actions)

        alarm_actions = alarm_definitions.get_query_alarm_definition_alarm_actions(alarm_def,
                                                                                   return_none=True)
        self.assertIsNone(alarm_actions)

    def test_alarm_definition_get_query_alarm_definition_undetermined_actions(self):
        alarm_def = {
            u'alarm_actions': 'c60ec47e-5038-4bf1-9f95-4046c6e9a759',
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': 'c60ec47e-5038-4bf1-9f95-4046c6e9a759',
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW',
        }
        undetermined_actions = \
            alarm_definitions.get_query_alarm_definition_undetermined_actions(alarm_def)
        self.assertEqual(alarm_def['undetermined_actions'], undetermined_actions)

        alarm_def.pop('undetermined_actions')

        undetermined_actions = \
            alarm_definitions.get_query_alarm_definition_undetermined_actions(alarm_def)
        self.assertEqual([], undetermined_actions)

        undetermined_actions = \
            alarm_definitions.get_query_alarm_definition_undetermined_actions(alarm_def,
                                                                              return_none=True)
        self.assertIsNone(undetermined_actions)

    def test_alarm_definition_get_query_alarm_definition_ok_actions(self):
        alarm_def = {
            u'ok_actions': 'c60ec47e-5038-4bf1-9f95-4046c6e9a759',
            u'description': u'',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW',
        }
        ok_actions = alarm_definitions.get_query_ok_actions(alarm_def)
        self.assertEqual(alarm_def['ok_actions'], ok_actions)

        alarm_def.pop('ok_actions')

        ok_actions = alarm_definitions.get_query_ok_actions(alarm_def)
        self.assertEqual([], ok_actions)

        ok_actions = alarm_definitions.get_query_ok_actions(alarm_def, return_none=True)
        self.assertIsNone(ok_actions)

    def test_alarm_definition_get_query_alarm_definition_actions_enabled(self):
        alarm_def = {
            u'alarm_actions': 'c60ec47e-5038-4bf1-9f95-4046c6e9a759',
            u'ok_actions': [],
            u'description': u'',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'deterministic': False,
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'severity': u'LOW',
        }
        actions_enabled = alarm_definitions.get_query_alarm_definition_actions_enabled(alarm_def)
        self.assertEqual(alarm_def['actions_enabled'], actions_enabled)

        alarm_def.pop('actions_enabled')

        actions_enabled = alarm_definitions.get_query_alarm_definition_actions_enabled(alarm_def)
        self.assertEqual('', actions_enabled)

        actions_enabled = alarm_definitions.\
            get_query_alarm_definition_actions_enabled(alarm_def,
                                                       return_none=True)
        self.assertIsNone(actions_enabled)

        actions_enabled = alarm_definitions. \
            get_query_alarm_definition_actions_enabled(alarm_def,
                                                       required=True,
                                                       return_none=True)
        self.assertIsNone(actions_enabled)

        self.assertRaises(HTTPUnprocessableEntityError,
                          alarm_definitions.get_query_alarm_definition_actions_enabled,
                          alarm_def,
                          required=True,
                          return_none=False)

    def test_alarm_definition_get_query_alarm_definition_is_definition_deterministic(self):
        expression = u'max(test.metric{hostname=host}) gte 1'
        is_deterministic = alarm_definitions.is_definition_deterministic(expression)
        self.assertEqual(False, is_deterministic)

        expression = u'max(test.metric{hostname=host}, deterministic) gte 1'
        is_deterministic = alarm_definitions.is_definition_deterministic(expression)
        self.assertEqual(True, is_deterministic)
