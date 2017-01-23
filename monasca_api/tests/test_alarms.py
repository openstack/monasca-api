# -*- coding: utf-8 -*-
# Copyright 2015 Cray Inc.
# (C) Copyright 2015,2017 Hewlett Packard Enterprise Development LP
# Copyright 2016 FUJITSU LIMITED
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

from mock import Mock

from monasca_api.common.repositories.model import sub_alarm_definition
from monasca_api.tests import base
from monasca_api.v2.reference import alarm_definitions
from monasca_api.v2.reference import alarms

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
    # Only present in data returned from InfluxDB:
    (u"tenant_id", TENANT_ID),
    # Only present in data returned from API:
    (u"id", u"1420070400000"),
))


class MonascaApiConfigFixture(oslo_config.fixture.Config):

    def setUp(self):
        super(MonascaApiConfigFixture, self).setUp()

        # [messaging]
        self.conf.set_override(
            'driver',
            'monasca_api.common.messaging.kafka_publisher:KafkaPublisher',
            group='messaging', enforce_type=True)

        # [repositories]
        self.conf.set_override(
            'alarms_driver',
            'monasca_api.common.repositories.sqla.alarms_repository:AlarmsRepository',
            group='repositories', enforce_type=True)
        self.conf.set_override(
            'alarm_definitions_driver',
            'monasca_api.common.repositories.alarm_definitions_repository:AlarmDefinitionsRepository',
            group='repositories', enforce_type=True)
        self.conf.set_override(
            'metrics_driver',
            'monasca_api.common.repositories.influxdb.metrics_repository:MetricsRepository',
            group='repositories', enforce_type=True)


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
        if len(actual) != 1:
            return matchers.Mismatch("Response contains <> 1 item: %r" % actual)

        response_data = json.loads(actual[0])

        if u"links" in response_data:
            del response_data[u"links"]

        return matchers.Equals(self.expected_data).match(response_data)


class AlarmTestBase(falcon.testing.TestBase, oslotest.BaseTestCase):

    api_class = base.MockedAPI

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
            'monasca_api.common.repositories.sqla.alarms_repository.AlarmsRepository'))

        self.alarms_resource = alarms.AlarmsStateHistory()
        self.api.add_route(
            '/v2.0/alarms/{alarm_id}/state-history/', self.alarms_resource)

    def test_alarm_state_history(self):
        expected_elements = {u"elements": [dict(ALARM_HISTORY)]}
        del expected_elements[u"elements"][0][u"time"]
        del expected_elements[u"elements"][0][u"sub_alarms"][0][u"sub_alarm_expression"][u"metric_definition"]
        del expected_elements[u"elements"][0][u"tenant_id"]

        response = self.simulate_request(
            u'/v2.0/alarms/%s/state-history/' % ALARM_HISTORY[u"alarm_id"],
            headers={
                'X-Roles': 'admin',
                'X-Tenant-Id': TENANT_ID,
            })

        self.assertEqual(self.srmock.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_elements))


class TestAlarmDefinition(AlarmTestBase):
    def setUp(self):
        super(TestAlarmDefinition, self).setUp()

        self.alarm_def_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.alarm_definitions_repository.AlarmDefinitionsRepository'
        )).mock

        self.alarm_definition_resource = alarm_definitions.AlarmDefinitions()
        self.alarm_definition_resource.send_event = Mock()
        self._send_event = self.alarm_definition_resource.send_event

        self.api.add_route("/v2.0/alarm-definitions/",
                           self.alarm_definition_resource)
        self.api.add_route("/v2.0/alarm-definitions/{alarm_definition_id}",
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
            u'actions_enabled': u'true',
            u'undetermined_actions': [],
            u'expression': u'test.metric > 10',
            u'deterministic': False,
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'severity': u'LOW',
        }

        response = self.simulate_request("/v2.0/alarm-definitions/",
                                         headers={'X-Roles': 'admin', 'X-Tenant-Id': TENANT_ID},
                                         method="POST",
                                         body=json.dumps(alarm_def))

        self.assertEqual(self.srmock.status, falcon.HTTP_201)
        self.assertThat(response, RESTResponseEquals(expected_data))

    def test_alarm_definition_create_with_valid_expressions(self):
        return_value = self.alarm_def_repo_mock.return_value
        return_value.get_alarm_definitions.return_value = []
        return_value.create_alarm_definition.return_value = u"00000001-0001-0001-0001-000000000001"

        valid_expressions = [
            "max(-_.千幸福的笑脸{घोड़ा=馬,  "
            "dn2=dv2,千幸福的笑脸घ=千幸福的笑脸घ}) gte 100 "
            "times 3 && "
            "(min(ເຮືອນ{dn3=dv3,家=дом}) < 10 or sum(biz{dn5=dv5}) >99 and "
            "count(fizzle) lt 0or count(baz) > 1)".decode('utf8'),

            "max(foo{hostname=mini-mon,千=千}, 120) > 100 and (max(bar)>100 "
            " or max(biz)>100)".decode('utf8'),

            "max(foo)>=100",

            "test_metric{this=that, that =  this} < 1",

            "max  (  3test_metric5  {  this  =  that  })  lt  5 times    3",

            "3test_metric5 lt 3",

            "ntp.offset > 1 or ntp.offset < -5",
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
            u'actions_enabled': u'true',
            u'undetermined_actions': [],
            u'expression': u'test.metric > 10',
            u'deterministic': False,
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'severity': u'LOW',
        }

        for expression in valid_expressions:
            alarm_def[u'expression'] = expression
            expected_data[u'expression'] = expression
            response = self.simulate_request("/v2.0/alarm-definitions/",
                                             headers={'X-Roles': 'admin', 'X-Tenant-Id': TENANT_ID},
                                             method="POST",
                                             body=json.dumps(alarm_def))

            self.assertEqual(self.srmock.status, falcon.HTTP_201,
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
            self.simulate_request("/v2.0/alarm-definitions/",
                                  headers={'X-Roles': 'admin', 'X-Tenant-Id': TENANT_ID},
                                  method="POST",
                                  body=json.dumps(alarm_def))

            self.assertEqual(self.srmock.status, '422 Unprocessable Entity',
                             u'Expression {} should have failed'.format(expression))

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
                      'periods': 1})}
             }
        )

        expected_def = {
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'Non-ASCII character: \u2603',
            u'links': [{u'href': u'http://falconframework.org/v2.0/alarm-definitions/'
                                 u'00000001-0001-0001-0001-000000000001/00000001-0001-0001-0001-000000000001',
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

        result = self.simulate_request("/v2.0/alarm-definitions/%s" % expected_def[u'id'],
                                       headers={'X-Roles': 'admin', 'X-Tenant-Id': TENANT_ID},
                                       method="PUT",
                                       body=json.dumps(alarm_def))

        self.assertEqual(self.srmock.status, falcon.HTTP_200)
        result_def = json.loads(result[0])
        self.assertEqual(result_def, expected_def)

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
                      'periods': 1})}
             }
        )

        expected_def = {
            u'id': alarm_def_id,
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': description,
            u'links': [{u'href': u'http://falconframework.org/v2.0/alarm-definitions/'
                                 u'00000001-0001-0001-0001-000000000001/00000001-0001-0001-0001-000000000001',
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

        result = self.simulate_request("/v2.0/alarm-definitions/%s" % expected_def[u'id'],
                                       headers={'X-Roles': 'admin', 'X-Tenant-Id': TENANT_ID},
                                       method="PATCH",
                                       body=json.dumps(alarm_def))

        self.assertEqual(self.srmock.status, falcon.HTTP_200)
        result_def = json.loads(result[0])
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
                                        u'dimensions': {u'uname': 'host'},
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
                      'is_deterministic': False})}
             }
        )

        expected_def = {
            u'id': u'00000001-0001-0001-0001-000000000001',
            u'alarm_actions': [],
            u'ok_actions': [],
            u'description': u'Non-ASCII character: \u2603',
            u'links': [{u'href': u'http://falconframework.org/v2.0/alarm-definitions/'
                                 u'00000001-0001-0001-0001-000000000001/00000001-0001-0001-0001-000000000001',
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

        result = self.simulate_request("/v2.0/alarm-definitions/%s" % expected_def[u'id'],
                                       headers={'X-Roles': 'admin', 'X-Tenant-Id': TENANT_ID},
                                       method="PUT",
                                       body=json.dumps(alarm_def))

        self.assertEqual(self.srmock.status, falcon.HTTP_200)
        result_def = json.loads(result[0])
        self.assertEqual(result_def, expected_def)

        for key, value in alarm_def.iteritems():
            del alarm_def[key]

            self.simulate_request("/v2.0/alarm-definitions/%s" % expected_def[u'id'],
                                  headers={'X-Roles': 'admin', 'X-Tenant-Id': TENANT_ID},
                                  method="PUT",
                                  body=json.dumps(alarm_def))
            self.assertEqual(self.srmock.status, "422 Unprocessable Entity",
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
            '/v2.0/alarm-definitions/%s' % (expected_data[u'id']),
            headers={
                'X-Roles': 'admin',
                'X-Tenant-Id': TENANT_ID,
            })

        self.assertEqual(self.srmock.status, falcon.HTTP_200)
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
            '/v2.0/alarm-definitions/%s' % (expected_data[u'id']),
            headers={
                'X-Roles': 'admin',
                'X-Tenant-Id': TENANT_ID,
            })

        self.assertEqual(self.srmock.status, falcon.HTTP_200)
        self.assertThat(response, RESTResponseEquals(expected_data))
