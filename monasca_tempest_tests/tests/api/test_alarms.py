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
import time

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

    @classmethod
    def create_alarms_for_test_alarms(cls):
        # create an alarm definition
        expression = "avg(name-1) > 0"
        name = data_utils.rand_name('name-1')
        alarm_definition = helpers.create_alarm_definition(
            name=name, expression=expression)
        resp, response_body = cls.monasca_client.create_alarm_definitions(
            alarm_definition)
        # create some metrics
        for i in xrange(30):
            metric = helpers.create_metric()
            resp, response_body = cls.monasca_client.create_metrics(metric)
            time.sleep(1)
            resp, response_body = cls.monasca_client.list_alarms()
            elements = response_body['elements']
            if len(elements) > 0:
                break

    @test.attr(type="gate")
    def test_list_alarms(self):
        self.create_alarms_for_test_alarms()
        resp, response_body = self.monasca_client.list_alarms()
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) ==
                        set(response_body))
        elements = response_body['elements']
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
            self.assertEqual(target_metric['dimensions'], metric['dimensions'])

    @test.attr(type="gate")
    def test_list_alarms_by_alarm_definition_id(self):
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        element = elements[0]
        alarm_definition_id = element['alarm_definition']['id']
        query_parms = '?alarm_definition_id=' + str(alarm_definition_id)
        resp, response_body = self.monasca_client.list_alarms(query_parms)
        self.assertEqual(200, resp.status)
        element_1 = response_body['elements'][0]
        self.assertEqual(element_1, element)

    @test.attr(type="gate")
    def test_list_alarms_by_metric_name(self):
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        element = elements[0]
        metric_name = element['metrics'][0]['name']
        query_parms = '?metric_name=' + str(metric_name)
        resp, response_body = self.monasca_client.list_alarms(query_parms)
        self.assertEqual(200, resp.status)
        element_1 = response_body['elements'][0]
        self.assertEqual(element_1, element)

    @test.attr(type="gate")
    def test_list_alarms_by_metric_dimensions(self):
        query_parms = '?metric_dimensions=key-2:value-2,key-1:value-1'
        resp, response_body_1 = self.monasca_client.list_alarms(query_parms)
        self.assertEqual(200, resp.status)

    @test.attr(type="gate")
    def test_list_alarms_by_state(self):
        resp, response_body = self.monasca_client.list_alarms()
        len0 = len(response_body['elements'])
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

    @test.attr(type="gate")
    def test_list_alarms_by_lifecycle_state(self):
        query_parms = '?lifecycle_state=None'
        resp, response_body = self.monasca_client.list_alarms(query_parms)
        self.assertEqual(200, resp.status)

    @test.attr(type="gate")
    def test_list_alarms_by_link(self):
        query_parms = '?link=None'
        resp, response_body = self.monasca_client.list_alarms(query_parms)
        self.assertEqual(200, resp.status)

    @test.attr(type="gate")
    def test_list_alarms_by_state_updated_start_time(self):
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

    @test.attr(type="gate")
    def test_list_alarms_by_offset_limit(self):
        resp, response_body = self.monasca_client.list_alarms()
        elements = response_body['elements']
        first_element = elements[0]
        next_element = elements[1]
        id = first_element['id']
        query_parms = '?offset=' + str(id) + '&limit=1'
        resp, response_body1 = self.monasca_client.list_alarms(query_parms)
        elements = response_body1['elements']
        self.assertEqual(1, len(elements))
        self.assertEqual(elements[0]['id'], next_element['id'])
        self.assertEqual(elements[0], next_element)

    @test.attr(type="gate")
    def test_get_alarm(self):
        self.create_alarms_for_test_alarms()
        resp, response_body = self.monasca_client.list_alarms()
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
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
            self.assertEqual(target_metric['dimensions'], metric['dimensions'])

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_get_alarm_with_invalid_id(self):
        id = data_utils.rand_name()
        self.assertRaises(exceptions.NotFound, self.monasca_client.get_alarm,
                          id)

    @test.attr(type="gate")
    def test_update_alarm(self):
        self.create_alarms_for_test_alarms()
        resp, response_body = self.monasca_client.list_alarms()
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
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

    @test.attr(type="gate")
    def test_patch_alarm(self):
        self.create_alarms_for_test_alarms()
        resp, response_body = self.monasca_client.list_alarms()
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
        element = elements[0]
        id = element['id']
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

    @test.attr(type="gate")
    def test_delete_alarm(self):
        self.create_alarms_for_test_alarms()
        resp, response_body = self.monasca_client.list_alarms()
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
        element = elements[0]
        id = element['id']
        resp, response_body = self.monasca_client.delete_alarm(id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_delete_alarm_with_invalid_id(self):
        id = data_utils.rand_name()
        self.assertRaises(exceptions.NotFound,
                          self.monasca_client.delete_alarm, id)
