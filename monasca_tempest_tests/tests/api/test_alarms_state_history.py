# (C) Copyright 2015 Hewlett Packard Enterprise Development Company LP
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
from monasca_tempest_tests.tests.api import helpers
from oslo_utils import timeutils
from tempest.common.utils import data_utils
from tempest import test

NUM_ALARM_DEFINITIONS = 9
MIN_HISTORY = 3

class TestAlarmsStateHistory(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestAlarmsStateHistory, cls).resource_setup()

        for i in xrange(1, NUM_ALARM_DEFINITIONS + 1):
            alarm_definition = helpers.create_alarm_definition(
                name=data_utils.rand_name('alarm_state_history' + str(i)),
                expression="min(name-1) < " + str(i))
            cls.monasca_client.create_alarm_definitions(alarm_definition)

        # create some metrics to prime the system and create three alarms
        for i in xrange(60):
            metric = helpers.create_metric()
            cls.monasca_client.create_metrics(metric)
            resp, response_body = cls.monasca_client.\
                list_alarms_state_history()
            elements = response_body['elements']
            if len(elements) >= MIN_HISTORY:
                break
            time.sleep(5)

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

        if len(all_elements) < 2:
            error_msg = "Failed test_list_alarms_state_history_with_" \
                        "start_time: need 2 or more alarms state history " \
                        "to test."
            self.fail(error_msg)

        # 2, query min(timestamp) < x
        min_element, max_element = self._get_elements_with_min_max_timestamp(
            all_elements)
        start_time = min_element['timestamp']
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

        if len(all_elements) < 2:
            error_msg = "Failed test_list_alarms_state_history_with_" \
                        "end_time: need 2 or more alarms state history " \
                        "to test."
            self.fail(error_msg)

        # 2, query x < max(timestamp)
        min_element, max_element = self._get_elements_with_min_max_timestamp(
            all_elements)
        end_time = max_element['timestamp']
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
                        "start_end_time: need 3 or more alarms state history " \
                        "to test."
            self.fail(error_msg)

        # 2, query min(timestamp) < x < max(timestamp)
        min_element, max_element = self._get_elements_with_min_max_timestamp(
            all_elements)
        start_time = min_element['timestamp']
        end_time = max_element['timestamp']
        query_params = '?start_time=' + str(start_time) + '&end_time=' + \
                       str(end_time)
        resp, selected_response_body = self.monasca_client.\
            list_alarms_state_history(query_params)
        selected_elements = selected_response_body['elements']

        # 3. compare #1 and #2
        expected_elements = all_elements
        expected_elements.remove(min_element)
        expected_elements.remove(max_element)
        self.assertEqual(expected_elements, selected_elements)

    @test.attr(type="gate")
    def test_list_alarms_state_history_with_offset_limit(self):
        resp, response_body = self.monasca_client.list_alarms_state_history()
        elements = response_body['elements']
        number_of_alarms = len(elements)
        if number_of_alarms >= MIN_HISTORY:
            first_element = elements[0]
            first_element_id = first_element['id']

            for limit in xrange(1, MIN_HISTORY):
                query_parms = '?limit=' + str(limit) + \
                              '&offset=' + str(first_element_id)
                resp, response_body = self.monasca_client.\
                    list_alarms_state_history(query_parms)
                elements = response_body['elements']
                element_new = elements[0]
                self.assertEqual(200, resp.status)
                self.assertEqual(element_new, first_element)
                self.assertEqual(limit, len(elements))
                id_new = element_new['id']
                self.assertEqual(id_new, first_element_id)
        else:
                error_msg = ("Failed "
                             "test_list_alarms_state_history_with_offset "
                             "limit: need three alarms state history to "
                             "test. Current number of alarms = {}").\
                    format(number_of_alarms)
                self.fail(error_msg)

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
            self.assertTrue(isinstance(links, list))
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
        if elements:
            element = elements[0]
            alarm_id = element['alarm_id']
            query_parms = '?limit=1'
            resp, response_body = self.monasca_client.\
                list_alarm_state_history(alarm_id, query_parms)
            elements = response_body['elements']
            self.assertEqual(200, resp.status)
            self.assertEqual(1, len(elements))

            id = element['id']
            query_parms = '?limit=1&offset=' + str(id)
            resp, response_body = self.monasca_client.\
                list_alarm_state_history(alarm_id, query_parms)
            elements_new = response_body['elements']
            self.assertEqual(200, resp.status)
            self.assertEqual(1, len(elements_new))
            self.assertEqual(element, elements_new[0])
        else:
            error_msg = "Failed test_list_alarm_state_history_with_offset" \
                        "_limit: at least one alarms state history is needed."
            self.fail(error_msg)

    def _get_elements_with_min_max_timestamp(self, elements):
        sorted_elements = sorted(elements, key=lambda element: timeutils.
                                 parse_isotime(element['timestamp']))
        min_element = sorted_elements[0]
        max_element = sorted_elements[-1]
        return min_element, max_element
