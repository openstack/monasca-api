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

# TODO(RMH): Update documentation. Get alarms returns alarm_definition, not
# TODO(RMH): alarm_definition_id in response body

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import helpers
from tempest.common.utils import data_utils
from tempest import test
from tempest_lib import exceptions


class TestAlarms(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestAlarms, cls).resource_setup()

    @classmethod
    def resource_cleanup(cls):
        super(TestAlarms, cls).resource_cleanup()

    @test.attr(type="gate")
    def test_list_alarms(self):
        helpers.create_alarms_for_test_alarms(self, num=1)
        resp, response_body = self.monasca_client.list_alarms()
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) ==
                        set(response_body))
        elements = response_body['elements']
        if elements:
            element = elements[0]
            self.assertTrue(set(['id',
                                 'links',
                                 'alarm_definition',
                                 'metrics',
                                 'state',
                                 'lifecycle_state',
                                 'link',
                                 'state_updated_timestamp',
                                 'updated_timestamp',
                                 'created_timestamp']) ==
                            set(element))
            for metric in element['metrics']:
                target_metric = helpers.create_metric()
                self.assertEqual(target_metric['name'], metric['name'])
                self.assertEqual(target_metric['dimensions'],
                                 metric['dimensions'])
            helpers.delete_alarm_definitions(self)
        else:
            skip_msg = "Skipped test_list_alarms: not the correct number of " \
                       "alarms to test"
            raise self.skipException(skip_msg)

    @test.attr(type="gate")
    def test_list_alarms_by_alarm_definition_id(self):
        helpers.create_alarms_for_test_alarms(self, num=1)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        if elements:
            element = elements[0]
            alarm_definition_id = element['alarm_definition']['id']
            query_parms = '?alarm_definition_id=' + str(alarm_definition_id)
            resp, response_body = self.monasca_client.list_alarms(query_parms)
            self.assertEqual(200, resp.status)
            element_1 = response_body['elements'][0]
            self.assertEqual(element_1, element)
            helpers.delete_alarm_definitions(self)
        else:
            skip_msg = "Skipped test_list_alarms_by_alarm_definition_id: " \
                       "not the correct number of alarms to test"
            raise self.skipException(skip_msg)

    @test.attr(type="gate")
    def test_list_alarms_by_metric_name(self):
        helpers.create_alarms_for_test_alarms(self, num=1)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        if elements:
            element = elements[0]
            metric_name = element['metrics'][0]['name']
            query_parms = '?metric_name=' + str(metric_name)
            resp, response_body = self.monasca_client.list_alarms(query_parms)
            self.assertEqual(200, resp.status)
            element_1 = response_body['elements'][0]
            self.assertEqual(element_1, element)
            helpers.delete_alarm_definitions(self)
        else:
            skip_msg = "Skipped test_list_alarms_by_metric_name: " \
                       "not the correct number of alarms to test"
            raise self.skipException(skip_msg)

    @test.attr(type="gate")
    def test_list_alarms_by_metric_dimensions(self):
        helpers.create_alarms_for_test_alarms(self, num=1)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        if elements:
            query_parms = '?metric_dimensions=key-2:value-2,key-1:value-1'
            resp, response_body = self.monasca_client.\
                list_alarms(query_parms)
            self.assertEqual(200, resp.status)
            helpers.delete_alarm_definitions(self)
        else:
            skip_msg = "Skipped test_list_alarms_by_metric_dimensions: " \
                       "not the correct number of alarms to test"
            raise self.skipException(skip_msg)

    @test.attr(type="gate")
    def test_list_alarms_by_state(self):
        helpers.create_alarms_for_test_alarms(self, num=3)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        number_of_alarms = len(elements)
        if number_of_alarms < 3:
            helpers.delete_alarm_definitions(self)
            skip_msg = "Skipped test_list_alarms_by_state: " \
                       "not the correct number of alarms to test"
            raise self.skipException(skip_msg)
        else:
            len0 = len(elements)
            query_parms = '?state=UNDETERMINED'
            resp, response_body1 = self.monasca_client.list_alarms(query_parms)
            len1 = len(response_body1['elements'])
            self.assertEqual(200, resp.status)
            query_parms = '?state=OK'
            resp, response_body2 = self.monasca_client.list_alarms(query_parms)
            len2 = len(response_body2['elements'])
            self.assertEqual(200, resp.status)
            query_parms = '?state=ALARM'
            resp, response_body3 = self.monasca_client.list_alarms(query_parms)
            len3 = len(response_body3['elements'])
            self.assertEqual(200, resp.status)
            self.assertEqual(len0, len1 + len2 + len3)
            helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    def test_list_alarms_by_lifecycle_state(self):
        helpers.create_alarms_for_test_alarms(self, num=1)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        if elements:
            query_parms = '?lifecycle_state=None'
            resp, response_body = self.monasca_client.list_alarms(query_parms)
            self.assertEqual(200, resp.status)
            helpers.delete_alarm_definitions(self)
        else:
            skip_msg = "Skipped test_list_alarms_by_lifecycle_state: " \
                       "not the correct number of alarms to test"
            raise self.skipException(skip_msg)

    @test.attr(type="gate")
    def test_list_alarms_by_link(self):
        helpers.create_alarms_for_test_alarms(self, num=1)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        if elements:
            query_parms = '?link=None'
            resp, response_body = self.monasca_client.list_alarms(query_parms)
            self.assertEqual(200, resp.status)
            helpers.delete_alarm_definitions(self)
        else:
            skip_msg = "Skipped test_list_alarms_by_link: " \
                       "not the correct number of alarms to test"
            raise self.skipException(skip_msg)

    @test.attr(type="gate")
    def test_list_alarms_by_state_updated_start_time(self):
        helpers.create_alarms_for_test_alarms(self, num=1)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        if elements:
            resp, response_body = self.monasca_client.list_alarms()
            elements = response_body['elements']
            element = elements[0]
            state_updated_start_time = element['state_updated_timestamp']
            query_parms = '?state_updated_timestamp=' + \
                          str(state_updated_start_time)
            resp, response_body = self.monasca_client.list_alarms(query_parms)
            self.assertEqual(200, resp.status)
            element_1 = response_body['elements'][0]
            self.assertEqual(element, element_1)
            helpers.delete_alarm_definitions(self)
        else:
            skip_msg = "Skipped " \
                       "test_list_alarms_by_state_updated_start_time: " \
                       "not the correct number of alarms to test"
            raise self.skipException(skip_msg)

    @test.attr(type="gate")
    def test_list_alarms_by_offset_limit(self):
        helpers.create_alarms_for_test_alarms(self, num=2)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        number_of_alarms = len(elements)
        if number_of_alarms < 2:
            helpers.delete_alarm_definitions(self)
            skip_msg = "Skipped test_list_alarms_by_offset_limit: " \
                       "not the correct number of alarms to test"
            raise self.skipException(skip_msg)
        else:
            first_element = elements[0]
            next_element = elements[1]
            id_first_element = first_element['id']
            query_parms = '?offset=' + str(id_first_element) + '&limit=1'
            resp, response_body1 = self.monasca_client.list_alarms(query_parms)
            elements = response_body1['elements']
            self.assertEqual(1, len(elements))
            self.assertEqual(elements[0]['id'], next_element['id'])
            self.assertEqual(elements[0], next_element)
            helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    def test_get_alarm(self):
        helpers.create_alarms_for_test_alarms(self, num=1)
        resp, response_body = self.monasca_client.list_alarms()
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
        if elements:
            element = elements[0]
            id = element['id']
            resp, response_body = self.monasca_client.get_alarm(id)
            self.assertEqual(200, resp.status)
            self.assertTrue(set(['id',
                                 'links',
                                 'alarm_definition',
                                 'metrics',
                                 'state',
                                 'lifecycle_state',
                                 'link',
                                 'state_updated_timestamp',
                                 'updated_timestamp',
                                 'created_timestamp']) ==
                            set(response_body))
            for metric in element['metrics']:
                target_metric = helpers.create_metric()
                self.assertEqual(target_metric['name'], metric['name'])
                self.assertEqual(target_metric['dimensions'],
                                 metric['dimensions'])
            helpers.delete_alarm_definitions(self)
        else:
            skip_msg = "Skipped test_get_alarm: " \
                       "not the correct number of alarms to test"
            raise self.skipException(skip_msg)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_get_alarm_with_invalid_id(self):
        id = data_utils.rand_name()
        self.assertRaises(exceptions.NotFound, self.monasca_client.get_alarm,
                          id)

    @test.attr(type="gate")
    def test_update_alarm(self):
        helpers.create_alarms_for_test_alarms(self, num=1)
        resp, response_body = self.monasca_client.list_alarms()
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
        if elements:
            element = elements[0]
            id = element['id']
            updated_state = "ALARM"
            updated_lifecycle_state = "OPEN"
            updated_link = "http://somesite.com"
            resp, response_body = self.monasca_client.update_alarm(
                id=id, state=updated_state,
                lifecycle_state=updated_lifecycle_state, link=updated_link)
            self.assertEqual(200, resp.status)
            self.assertTrue(set(['id',
                                 'links',
                                 'alarm_definition',
                                 'metrics',
                                 'state',
                                 'lifecycle_state',
                                 'link',
                                 'state_updated_timestamp',
                                 'updated_timestamp',
                                 'created_timestamp']) ==
                            set(response_body))
            # Validate fields updated
            self.assertEqual(updated_state, response_body['state'])
            self.assertEqual(updated_lifecycle_state, response_body[
                'lifecycle_state'])
            self.assertEqual(updated_link, response_body[
                'link'])
            helpers.delete_alarm_definitions(self)
        else:
            skip_msg = "Skipped test_list_alarms_by_offset_limit: " \
                       "not the correct number of alarms to test"
            raise self.skipException(skip_msg)

    @test.attr(type="gate")
    def test_patch_alarm(self):
        helpers.create_alarms_for_test_alarms(self, num=1)
        resp, response_body = self.monasca_client.list_alarms()
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
        if elements:
            id = elements[0]['id']
            updated_state = "UNDETERMINED"
            resp, response_body = self.monasca_client.patch_alarm(
                id=id, state=updated_state)
            self.assertEqual(200, resp.status)
            self.assertTrue(set(['id',
                                 'links',
                                 'alarm_definition',
                                 'metrics',
                                 'state',
                                 'lifecycle_state',
                                 'link',
                                 'state_updated_timestamp',
                                 'updated_timestamp',
                                 'created_timestamp']) ==
                            set(response_body))
            # Validate the field patched
            self.assertEqual(updated_state, response_body['state'])
            helpers.delete_alarm_definitions(self)
        else:
            skip_msg = "Skipped test_patch_alarm: " \
                       "not the correct number of alarms to test"
            raise self.skipException(skip_msg)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_delete_alarm_with_invalid_id(self):
        id = data_utils.rand_name()
        self.assertRaises(exceptions.NotFound,
                          self.monasca_client.delete_alarm, id)

    @test.attr(type="gate")
    def test_create_alarms_with_match_by(self):
        # Create an alarm definition with no match_by
        name = data_utils.rand_name('alarm_definition_1')
        expression = "avg(cpu.idle_perc{service=monitoring}) < 20"
        alarm_definition = helpers.create_alarm_definition(
            name=name, description="description", expression=expression)
        self.monasca_client.create_alarm_definitions(alarm_definition)
        helpers.create_metrics_for_test_alarms_match_by(self, num=1,
                                                        sub_expressions=False,
                                                        list=False)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        metrics = elements[0]['metrics']
        self.assertEqual(len(metrics), 2)
        self.assertNotEqual(metrics[0], metrics[1])
        helpers.delete_alarm_definitions(self)

        # Create an alarm definition with match_by
        name = data_utils.rand_name('alarm_definition_1')
        expression = "avg(cpu.idle_perc{service=monitoring}) < 20"
        match_by = ['hostname']
        alarm_definition = helpers.create_alarm_definition(
            name=name, description="description", expression=expression,
            match_by=match_by)
        self.monasca_client.create_alarm_definitions(alarm_definition)
        # create some metrics
        helpers.create_metrics_for_test_alarms_match_by(self, num=2,
                                                        sub_expressions=False,
                                                        list=False)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        self.assertEqual(len(elements), 2)
        self.assertEqual(len(elements[0]['metrics']), 1)
        self.assertEqual(len(elements[1]['metrics']), 1)
        self.assertNotEqual(elements[0]['metrics'], elements[1]['metrics'])
        helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    def test_create_alarms_with_sub_expressions_and_match_by(self):
        # Create an alarm definition with sub-expressions and match_by
        name = data_utils.rand_name('alarm_definition_1')
        expression = "avg(cpu.idle_perc{service=monitoring}) < 10 or " \
                     "avg(cpu.user_perc{service=monitoring}) > 60"
        match_by = ['hostname']
        alarm_definition = helpers.create_alarm_definition(
            name=name, description="description", expression=expression,
            match_by=match_by)
        self.monasca_client.create_alarm_definitions(alarm_definition)

        helpers.create_metrics_for_test_alarms_match_by(self, num=2,
                                                        sub_expressions=True,
                                                        list=False)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        self.assertEqual(len(elements), 2)
        self.assertEqual(len(elements[0]['metrics']), 2)
        self.assertEqual(len(elements[1]['metrics']), 2)
        hostname_1 = elements[0]['metrics'][0]['dimensions']['hostname']
        hostname_2 = elements[0]['metrics'][1]['dimensions']['hostname']
        hostname_3 = elements[1]['metrics'][0]['dimensions']['hostname']
        hostname_4 = elements[1]['metrics'][1]['dimensions']['hostname']
        self.assertEqual(hostname_1, hostname_2)
        self.assertEqual(hostname_3, hostname_4)
        self.assertNotEqual(hostname_1, hostname_3)
        helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    def test_create_alarms_with_match_by_list(self):
        # Create an alarm definition with match_by as a list
        name = data_utils.rand_name('alarm_definition_1')
        expression = "avg(cpu.idle_perc{service=monitoring}) < 10"
        match_by = ['hostname', 'device']
        alarm_definition = helpers.create_alarm_definition(
            name=name, description="description", expression=expression,
            match_by=match_by)
        self.monasca_client.create_alarm_definitions(alarm_definition)
        # create some metrics
        helpers.create_metrics_for_test_alarms_match_by(self, num=4,
                                                        sub_expressions=True,
                                                        list=True)
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        self.assertEqual(len(elements), 4)
        self.assertEqual(len(elements[0]['metrics']), 1)
        self.assertEqual(len(elements[1]['metrics']), 1)
        self.assertEqual(len(elements[2]['metrics']), 1)
        self.assertEqual(len(elements[3]['metrics']), 1)
        dimensions_1 = elements[0]['metrics'][0]['dimensions']
        dimensions_2 = elements[1]['metrics'][0]['dimensions']
        dimensions_3 = elements[2]['metrics'][0]['dimensions']
        dimensions_4 = elements[3]['metrics'][0]['dimensions']
        self.assertNotEqual(dimensions_1, dimensions_2)
        self.assertNotEqual(dimensions_1, dimensions_3)
        self.assertNotEqual(dimensions_1, dimensions_4)
        self.assertNotEqual(dimensions_2, dimensions_3)
        self.assertNotEqual(dimensions_2, dimensions_4)
        self.assertNotEqual(dimensions_3, dimensions_4)
        helpers.delete_alarm_definitions(self)
