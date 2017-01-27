# (C) Copyright 2016 Hewlett Packard Enterprise Development LP
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

import time

from tempest.common.utils import data_utils
from tempest import test

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import helpers

WAIT_SECS = 10


class TestAlarmTransitions(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestAlarmTransitions, cls).resource_setup()

    @classmethod
    def resource_cleanup(cls):
        super(TestAlarmTransitions, cls).resource_cleanup()

    def _wait_for_alarm_creation(self, definition_id):
        for x in xrange(WAIT_SECS):
            time.sleep(1)
            resp, resp_body = self.monasca_client.list_alarms(
                query_params="?alarm_definition_id=" + definition_id)

            self.assertEqual(200, resp.status)
            if len(resp_body['elements']) != 0:
                break
        self.assertEqual(1, len(resp_body['elements']))
        alarm_id = resp_body['elements'][0]['id']
        initial_state = resp_body['elements'][0]['state']
        return alarm_id, initial_state

    def _wait_for_alarm_transition(self, alarm_id, expected_state):
        for x in xrange(WAIT_SECS):
            time.sleep(1)
            resp, resp_body = self.monasca_client.get_alarm(alarm_id)
            self.assertEqual(200, resp.status)
            if resp_body['state'] == expected_state:
                break
        self.assertEqual(expected_state, resp_body['state'])

    def _send_measurement(self, metric_def, value):
        metric = helpers.create_metric(name=metric_def['name'],
                                       dimensions=metric_def['dimensions'],
                                       value=value)
        resp, resp_body = self.monasca_client.create_metrics([metric])
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    def test_alarm_max_function(self):
        metric_def = {
            'name': data_utils.rand_name("max_test"),
            'dimensions': {
                'dim_to_match': data_utils.rand_name("max_match")
            }
        }
        expression = "max(" + metric_def['name'] + ") > 14"
        definition = helpers.create_alarm_definition(name="Test Max Function",
                                                     description="",
                                                     expression=expression,
                                                     match_by=["dim_to_match"])
        resp, resp_body = (self.monasca_client
                           .create_alarm_definitions(definition))
        self.assertEqual(201, resp.status)
        definition_id = resp_body['id']
        time.sleep(1)

        self._send_measurement(metric_def, 1)

        alarm_id, initial_state = self._wait_for_alarm_creation(definition_id)
        self.assertEqual("UNDETERMINED", initial_state)

        self._send_measurement(metric_def, 20)

        self._wait_for_alarm_transition(alarm_id, "ALARM")

    @test.attr(type="gate")
    def test_alarm_max_with_deterministic(self):
        metric_def = {
            'name': data_utils.rand_name("max_deterministic_test"),
            'dimensions': {
                'dim_to_match': data_utils.rand_name("max_match")
            }
        }
        expression = "max(" + metric_def['name'] + ",deterministic) > 14"
        definition = helpers.create_alarm_definition(name="Test Max Deterministic Function",
                                                     description="",
                                                     expression=expression,
                                                     match_by=["dim_to_match"])
        resp, resp_body = self.monasca_client.create_alarm_definitions(definition)
        self.assertEqual(201, resp.status)
        definition_id = resp_body['id']
        time.sleep(1)

        self._send_measurement(metric_def, 1)

        alarm_id, initial_state = self._wait_for_alarm_creation(definition_id)
        self.assertEqual("OK", initial_state)

        self._send_measurement(metric_def, 20)

        self._wait_for_alarm_transition(alarm_id, "ALARM")

    @test.attr(type="gate")
    def test_alarm_last_function(self):
        metric_def = {
            'name': data_utils.rand_name("last_test"),
            'dimensions': {
                'dim_to_match': data_utils.rand_name("last_match")
            }
        }
        expression = "last(" + metric_def['name'] + ") > 14"
        definition = helpers.create_alarm_definition(name="Test Last Function",
                                                     description="",
                                                     expression=expression,
                                                     match_by=["dim_to_match"])
        resp, resp_body = self.monasca_client.create_alarm_definitions(definition)
        self.assertEqual(201, resp.status)
        definition_id = resp_body['id']
        time.sleep(1)

        self._send_measurement(metric_def, 1)

        alarm_id, initial_state = self._wait_for_alarm_creation(definition_id)
        self.assertEqual("OK", initial_state)

        self._send_measurement(metric_def, 20)

        self._wait_for_alarm_transition(alarm_id, "ALARM")

        self._send_measurement(metric_def, 3)

        self._wait_for_alarm_transition(alarm_id, "OK")

    @test.attr(type="gate")
    def test_alarm_last_with_deterministic(self):
        metric_def = {
            'name': data_utils.rand_name("last_deterministic_test"),
            'dimensions': {
                'dim_to_match': data_utils.rand_name("last_match")
            }
        }
        expression = "last(" + metric_def['name'] + ",deterministic) > 14"
        definition = helpers.create_alarm_definition(name="Test Last Deterministic Function",
                                                     description="",
                                                     expression=expression,
                                                     match_by=["dim_to_match"])
        resp, resp_body = self.monasca_client.create_alarm_definitions(definition)
        self.assertEqual(201, resp.status)
        definition_id = resp_body['id']
        time.sleep(1)

        self._send_measurement(metric_def, 1)

        alarm_id, initial_state = self._wait_for_alarm_creation(definition_id)
        self.assertEqual("OK", initial_state)

        self._send_measurement(metric_def, 20)

        self._wait_for_alarm_transition(alarm_id, "ALARM")

        self._send_measurement(metric_def, 3)

        self._wait_for_alarm_transition(alarm_id, "OK")
