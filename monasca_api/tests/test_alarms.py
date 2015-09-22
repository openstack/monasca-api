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

from monasca_api.v2.reference import alarms

import oslo_config
import oslo_config.fixture
import oslotest.base as oslotest

CONF = oslo_config.cfg.CONF

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
    (u"tenant_id", u"fedcba9876543210fedcba9876543210"),
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


class TestAlarmsStateHistory(falcon.testing.TestBase, oslotest.BaseTestCase):

    def __init__(self, *args, **kwargs):
        super(TestAlarmsStateHistory, self).__init__(*args, **kwargs)

    def setUp(self):
        super(TestAlarmsStateHistory, self).setUp()

        self.CONF = self.useFixture(MonascaApiConfigFixture(CONF)).conf

        self.useFixture(fixtures.MockPatch(
            'monasca_api.common.messaging.kafka_publisher.KafkaPublisher'))
        self.useFixture(InfluxClientAlarmHistoryResponseFixture(
            'monasca_api.common.repositories.influxdb.metrics_repository.client.InfluxDBClient'))
        self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.mysql.alarms_repository.AlarmsRepository'))

        self.alarms_resource = alarms.AlarmsStateHistory()
        self.api.add_route(
            '/v2.0/alarms/{alarm_id}/state-history/', self.alarms_resource)

    def test_alarm_state_history(self):
        expected_elements = dict(ALARM_HISTORY)
        del expected_elements[u"time"]
        del expected_elements[u"sub_alarms"][0][u"sub_alarm_expression"][u"metric_definition"]

        response = self.simulate_request(
            u'/v2.0/alarms/%s/state-history/' % ALARM_HISTORY[u"alarm_id"],
            headers={
                'X-Roles': 'admin',
                'X-Tenant-Id': ALARM_HISTORY[u"tenant_id"],
            })

        self.assertEqual(self.srmock.status, falcon.HTTP_200)

        self.assertEqual(len(response), 1)

        response_data = json.loads(response[0])
        response_elements = response_data["elements"]
        self.assertEqual(response_elements, [expected_elements])
