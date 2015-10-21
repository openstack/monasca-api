# Copyright 2015 Cray Inc.
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
import json

import falcon.testing
import fixtures
import testtools.matchers as matchers

from monasca_api.v2.reference import alarm_definitions
from monasca_api.v2.reference import alarms

import oslo_config
import oslo_config.fixture
import oslotest.base as oslotest

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
    (u"tenant_id", TENANT_ID),
))


class MonascaApiConfigFixture(oslo_config.fixture.Config):

    def setUp(self):
        super(MonascaApiConfigFixture, self).setUp()

        # [messaging]
        self.conf.set_override(
            'driver',
            'monasca_api.common.messaging.kafka_publisher:KafkaPublisher',
            group='messaging')

        # [repositories]
        self.conf.set_override(
            'alarms_driver',
            'monasca_api.common.repositories.mysql.alarms_repository:AlarmsRepository',
            group='repositories')
        self.conf.set_override(
            'alarm_definitions_driver',
            'monasca_api.common.repositories.alarm_definitions_repository:AlarmDefinitionsRepository',
            group='repositories')
        self.conf.set_override(
            'metrics_driver',
            'monasca_api.common.repositories.influxdb.metrics_repository:MetricsRepository',
            group='repositories')


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
        if len(actual) != 1:
            return matchers.Mismatch("Response contains <> 1 item: %r" % actual)

        response_data = json.loads(actual[0])

        if u"links" in response_data:
            del response_data[u"links"]

        return matchers.Equals(self.expected_data).match(response_data)


class AlarmTestBase(falcon.testing.TestBase, oslotest.BaseTestCase):

    def setUp(self):
        super(AlarmTestBase, self).setUp()

        self.useFixture(fixtures.MockPatch(
            'monasca_api.common.messaging.kafka_publisher.KafkaPublisher'))

        self.CONF = self.useFixture(MonascaApiConfigFixture(CONF)).conf


class TestAlarmsStateHistory(AlarmTestBase):

    def setUp(self):
        super(TestAlarmsStateHistory, self).setUp()

        self.useFixture(InfluxClientAlarmHistoryResponseFixture(
            'monasca_api.common.repositories.influxdb.metrics_repository.client.InfluxDBClient'))
        self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.mysql.alarms_repository.AlarmsRepository'))

        self.alarms_resource = alarms.AlarmsStateHistory()
        self.api.add_route(
            '/v2.0/alarms/{alarm_id}/state-history/', self.alarms_resource)

    def test_alarm_state_history(self):
        expected_elements = {u"elements": [dict(ALARM_HISTORY)]}
        del expected_elements[u"elements"][0][u"time"]
        del expected_elements[u"elements"][0][u"sub_alarms"][0][u"sub_alarm_expression"][u"metric_definition"]

        response = self.simulate_request(
            u'/v2.0/alarms/%s/state-history/' % ALARM_HISTORY[u"alarm_id"],
            headers={
                'X-Roles': 'admin',
                'X-Tenant-Id': ALARM_HISTORY[u"tenant_id"],
            })

        self.assertEqual(self.srmock.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))


class TestAlarmDefinitionList(AlarmTestBase):
    def setUp(self):
        super(TestAlarmDefinitionList, self).setUp()

        self.alarm_def_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.alarm_definitions_repository.AlarmDefinitionsRepository'
        )).mock

        self.alarm_definition_resource = alarm_definitions.AlarmDefinitions()

        self.api.add_route("/v2.0/alarm-definitions/{alarm_definition_id}",
                           self.alarm_definition_resource)

    def test_alarm_definition_specific_alarm(self):

        self.alarm_def_repo_mock.return_value.get_alarm_definition.return_value = {
            'alarm_actions': None,
            'ok_actions': None,
            'description': b'Non-ASCII character: \xe2\x98\x83',
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
            u'description': u'Non-ASCII character: \u2603',
            u'match_by': [u'hostname'],
            u'name': u'Test Alarm',
            u'actions_enabled': True,
            u'undetermined_actions': [],
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'severity': u'LOW',
        }

        response = self.simulate_request(
            '/v2.0/alarm-definitions/%s' % (expected_data[u'id']),
            headers={
                'X-Roles': 'admin',
                'X-Tenant-Id': TENANT_ID,
            })

        self.assertEqual(self.srmock.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_data))

    def test_alarm_definition_specific_alarm_description_none(self):

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
            u'expression': u'max(test.metric{hostname=host}) gte 1',
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'severity': u'LOW',
        }

        response = self.simulate_request(
            '/v2.0/alarm-definitions/%s' % (expected_data[u'id']),
            headers={
                'X-Roles': 'admin',
                'X-Tenant-Id': TENANT_ID,
            })

        self.assertEqual(self.srmock.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_data))
