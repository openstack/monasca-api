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

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import constants
from monasca_tempest_tests.tests.api import helpers
from tempest.common.utils import data_utils
from tempest import test

MIN_HISTORY = 2


class TestAlarmStateHistoryMultipleTransitions(base.BaseMonascaTest):
    # For testing list alarm state history with the same alarm ID, two alarm
    # transitions are needed. One transit from ALARM state to UNDETERMINED
    # state and the other one from UNDETERMINED state to ALARM state.

    @classmethod
    def resource_setup(cls):
        super(TestAlarmStateHistoryMultipleTransitions, cls).resource_setup()
        alarm_definition = helpers.create_alarm_definition(
            name=data_utils.rand_name('alarm_state_history'),
            expression="min(name-1) < 1.0")
        cls.monasca_client.create_alarm_definitions(alarm_definition)
        for timer in xrange(constants.MAX_RETRIES):
            # create some metrics to prime the system and create
            # MIN_HISTORY alarms
            metric = helpers.create_metric(
                name="name-1", dimensions={'key1': 'value1'}, value=0.0)
            cls.monasca_client.create_metrics(metric)
            # sleep 1 second between metrics to make sure timestamps
            # are different in the second field. Influxdb has a bug
            # where it does not sort properly by milliseconds. .014
            # is sorted as greater than .138
            time.sleep(1.0)
            resp, response_body = cls.monasca_client.\
                list_alarms_state_history()
            elements = response_body['elements']
            if len(elements) >= 1:
                break
            time.sleep(constants.RETRY_WAIT_SECS)

        time.sleep(constants.MAX_RETRIES)

        for timer in xrange(constants.MAX_RETRIES * 2):
            metric = helpers.create_metric(
                name="name-1", dimensions={'key2': 'value2'}, value=2.0)
            cls.monasca_client.create_metrics(metric)
            # sleep 0.05 second between metrics to make sure timestamps
            # are different
            time.sleep(0.05)
            resp, response_body = \
                cls.monasca_client.list_alarms_state_history()
            elements = response_body['elements']
            if len(elements) >= 2:
                return
            else:
                num_transitions = len(elements)
            time.sleep(constants.RETRY_WAIT_SECS)
        assert False, "Required {} alarm state transitions, but found {}".\
            format(MIN_HISTORY, num_transitions)

    @classmethod
    def resource_cleanup(cls):
        super(TestAlarmStateHistoryMultipleTransitions, cls).\
            resource_cleanup()

    @test.attr(type="gate")
    def test_list_alarm_state_history(self):
        # Get the alarm state history for a specific alarm by ID
        resp, response_body = self.monasca_client.list_alarms_state_history()
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
        if elements:
            element = elements[0]
            alarm_id = element['alarm_id']
            resp, response_body = self.monasca_client.list_alarm_state_history(
                alarm_id)
            self.assertEqual(200, resp.status)

            # Test Response Body
            self.assertTrue(set(['links', 'elements']) ==
                            set(response_body))
            elements = response_body['elements']
            links = response_body['links']
            self.assertIsInstance(links, list)
            link = links[0]
            self.assertTrue(set(['rel', 'href']) ==
                            set(link))
            self.assertEqual(link['rel'], u'self')
            definition = elements[0]
            self.assertTrue(set(['id', 'alarm_id', 'metrics', 'new_state',
                                 'old_state', 'reason', 'reason_data',
                                 'sub_alarms', 'timestamp']) ==
                            set(definition))
        else:
            error_msg = "Failed test_list_alarm_state_history: at least one " \
                        "alarm state history is needed."
            self.fail(error_msg)

    @test.attr(type="gate")
    def test_list_alarm_state_history_with_offset_limit(self):
        # Get the alarm state history for a specific alarm by ID
        resp, response_body = self.monasca_client.list_alarms_state_history()
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
        if len(elements) >= MIN_HISTORY:
            element = elements[0]
            second_element = elements[1]
            alarm_id = element['alarm_id']
            query_parms = '?limit=1'
            resp, response_body = self.monasca_client.\
                list_alarm_state_history(alarm_id, query_parms)
            elements = response_body['elements']
            self.assertEqual(200, resp.status)
            self.assertEqual(1, len(elements))

            query_parms = '?offset=' + str(element['timestamp'])
            resp, response_body = self.monasca_client.\
                list_alarm_state_history(alarm_id, query_parms)
            elements_new = response_body['elements']
            self.assertEqual(200, resp.status)
            self.assertEqual(1, len(elements_new))
            self.assertEqual(second_element, elements_new[0])
        else:
            error_msg = "Failed test_list_alarm_state_history_with_offset" \
                        "_limit: two alarms state history are needed."
            self.fail(error_msg)
