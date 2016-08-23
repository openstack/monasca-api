# (C) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
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
from oslo_utils import timeutils
from tempest.common.utils import data_utils
from tempest import test

NUM_ALARM_DEFINITIONS = 3
MIN_HISTORY = 3


class TestAlarmsStateHistoryOneTransition(base.BaseMonascaTest):
    # Alarms state histories with one transition but different alarm
    # definitions are needed for this test class.

    @classmethod
    def resource_setup(cls):
        super(TestAlarmsStateHistoryOneTransition, cls).resource_setup()

        for i in xrange(MIN_HISTORY):
            alarm_definition = helpers.create_alarm_definition(
                name=data_utils.rand_name('alarm_state_history' + str(i + 1)),
                expression="min(name-" + str(i + 1) + ") < " + str(i + 1))
            cls.monasca_client.create_alarm_definitions(alarm_definition)

        num_transitions = 0
        for timer in xrange(constants.MAX_RETRIES):
            for i in xrange(MIN_HISTORY):
                # Create some metrics to prime the system and waiting for the
                # alarms to be created and then for them to change state.
                # MIN_HISTORY number of Alarms State History are needed.
                metric = helpers.create_metric(name="name-" + str(i + 1))
                cls.monasca_client.create_metrics(metric)
                # Ensure alarms transition at different times
                time.sleep(0.1)
            resp, response_body = cls.monasca_client.\
                list_alarms_state_history()
            elements = response_body['elements']
            if len(elements) >= MIN_HISTORY:
                return
            else:
                num_transitions = len(elements)
            time.sleep(constants.RETRY_WAIT_SECS)
        assert False, "Required {} alarm state transitions, but found {}".\
            format(MIN_HISTORY, num_transitions)

    @classmethod
    def resource_cleanup(cls):
        super(TestAlarmsStateHistoryOneTransition, cls).resource_cleanup()

    @test.attr(type="gate")
    def test_list_alarms_state_history(self):
        resp, response_body = self.monasca_client.list_alarms_state_history()
        self.assertEqual(200, resp.status)
        # Test response body
        self.assertTrue(set(['links', 'elements']) == set(response_body))
        elements = response_body['elements']
        number_of_alarms = len(elements)
        if number_of_alarms < 1:
            error_msg = "Failed test_list_alarms_state_history: need " \
                        "at least one alarms state history to test."
            self.fail(error_msg)
        else:
            element = elements[0]
            self.assertTrue(set(['id', 'alarm_id', 'metrics', 'old_state',
                                 'new_state', 'reason', 'reason_data',
                                 'timestamp', 'sub_alarms'])
                            == set(element))

    @test.attr(type="gate")
    def test_list_alarms_state_history_with_dimensions(self):
        resp, response_body = self.monasca_client.list_alarms_state_history()
        elements = response_body['elements']
        if elements:
            element = elements[0]
            dimension = element['metrics'][0]['dimensions']
            dimension_items = dimension.items()
            dimension_item = dimension_items[0]
            dimension_item_0 = dimension_item[0]
            dimension_item_1 = dimension_item[1]
            name = element['metrics'][0]['name']

            query_parms = '?dimensions=' + str(dimension_item_0) + ':' + str(
                dimension_item_1)
            resp, response_body = self.monasca_client.\
                list_alarms_state_history(query_parms)
            name_new = response_body['elements'][0]['metrics'][0]['name']
            self.assertEqual(200, resp.status)
            self.assertEqual(name, name_new)
        else:
            error_msg = "Failed test_list_alarms_state_history_with_" \
                        "dimensions: need at least one alarms state history " \
                        "to test."
            self.fail(error_msg)

    @test.attr(type="gate")
    def test_list_alarms_state_history_with_start_time(self):
        # 1, get all histories
        resp, all_response_body = self.monasca_client.\
            list_alarms_state_history()
        all_elements = all_response_body['elements']

        if len(all_elements) < 3:
            error_msg = "Failed test_list_alarms_state_history_with_" \
                        "start_time: need 3 or more alarms state history " \
                        "to test."
            self.fail(error_msg)

        # 2, query second(timestamp) <= x
        min_element, second_element, max_element = \
            self._get_elements_with_min_max_timestamp(all_elements)
        start_time = second_element['timestamp']
        query_params = '?start_time=' + str(start_time)
        resp, selected_response_body = self.monasca_client.\
            list_alarms_state_history(query_params)
        selected_elements = selected_response_body['elements']

        # 3. compare #1 and #2
        expected_elements = all_elements
        expected_elements.remove(min_element)
        self.assertEqual(expected_elements, selected_elements)

    @test.attr(type="gate")
    def test_list_alarms_state_history_with_end_time(self):
        # 1, get all histories
        resp, all_response_body = self.monasca_client.\
            list_alarms_state_history()
        all_elements = all_response_body['elements']

        if len(all_elements) < 3:
            error_msg = "Failed test_list_alarms_state_history_with_" \
                        "end_time: need 3 or more alarms state history " \
                        "to test."
            self.fail(error_msg)

        # 2, query x <= second(timestamp)
        min_element, second_element, max_element = \
            self._get_elements_with_min_max_timestamp(all_elements)
        end_time = second_element['timestamp']
        query_params = '?end_time=' + str(end_time)
        resp, selected_response_body = self.monasca_client.\
            list_alarms_state_history(query_params)
        selected_elements = selected_response_body['elements']

        # 3. compare #1 and #2
        expected_elements = all_elements
        expected_elements.remove(max_element)
        self.assertEqual(expected_elements, selected_elements)

    @test.attr(type="gate")
    def test_list_alarms_state_history_with_start_end_time(self):
        # 1, get all histories
        resp, all_response_body = self.monasca_client.\
            list_alarms_state_history()
        all_elements = all_response_body['elements']

        if len(all_elements) < 3:
            error_msg = "Failed test_list_alarms_state_history_with_" \
                        "start_end_time: need 3 or more alarms state history" \
                        "to test."
            self.fail(error_msg)

        # 2, query min(timestamp) <= x <= max(timestamp)
        min_element, second_element, max_element = \
            self._get_elements_with_min_max_timestamp(all_elements)
        start_time = min_element['timestamp']
        end_time = max_element['timestamp']
        query_params = '?start_time=' + str(start_time) + '&end_time=' + \
                       str(end_time)
        resp, selected_response_body = self.monasca_client.\
            list_alarms_state_history(query_params)
        selected_elements = selected_response_body['elements']

        # 3. compare #1 and #2
        self.assertEqual(all_elements, selected_elements)

    @test.attr(type="gate")
    def test_list_alarms_state_history_with_offset_limit(self):
        resp, response_body = self.monasca_client.list_alarms_state_history()
        elements_set1 = response_body['elements']
        number_of_alarms = len(elements_set1)
        if number_of_alarms >= MIN_HISTORY:
            query_parms = '?limit=' + str(number_of_alarms)
            resp, response_body = self.monasca_client.\
                list_alarms_state_history(query_parms)
            self.assertEqual(200, resp.status)
            elements_set2 = response_body['elements']
            self.assertEqual(number_of_alarms, len(elements_set2))
            for index in xrange(MIN_HISTORY - 1):
                self.assertEqual(elements_set1[index], elements_set2[index])
            for index in xrange(MIN_HISTORY - 1):
                alarm_history = elements_set2[index]
                max_limit = len(elements_set2) - index
                for limit in xrange(1, max_limit):
                    first_index = index + 1
                    last_index = first_index + limit
                    expected_elements = elements_set2[first_index:last_index]

                    query_parms = '?offset=' + str(alarm_history['timestamp'])\
                                  + '&limit=' + str(limit)
                    resp, response_body = self.\
                        monasca_client.list_alarms_state_history(query_parms)
                    self.assertEqual(200, resp.status)
                    new_elements = response_body['elements']
                    self.assertEqual(limit, len(new_elements))
                    for i in xrange(len(expected_elements)):
                        self.assertEqual(expected_elements[i], new_elements[i])
        else:
            error_msg = ("Failed test_list_alarms_state_history_with_offset "
                         "limit: need three alarms state history to test. "
                         "Current number of alarms = {}").format(
                number_of_alarms)
            self.fail(error_msg)

    def _get_elements_with_min_max_timestamp(self, elements):
        sorted_elements = sorted(elements, key=lambda element: timeutils.
                                 parse_isotime(element['timestamp']))
        min_element = sorted_elements[0]
        second_element = sorted_elements[1]
        max_element = sorted_elements[-1]
        return min_element, second_element, max_element
