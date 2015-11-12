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
from monasca_tempest_tests.tests.api import constants
from monasca_tempest_tests.tests.api import helpers
from tempest.common.utils import data_utils
from tempest import test
from tempest_lib import exceptions

NUM_ALARM_DEFINITIONS = 2


class TestAlarmDefinitions(base.BaseMonascaTest):
    @classmethod
    def resource_setup(cls):
        super(TestAlarmDefinitions, cls).resource_setup()

    @classmethod
    def resource_cleanup(cls):
        super(TestAlarmDefinitions, cls).resource_cleanup()

    # Create

    @test.attr(type="gate")
    def test_create_alarm_definition(self):
        # Create an alarm definition
        name = data_utils.rand_name('alarm_definition')
        expression = "max(cpu.system_perc) > 0"
        alarm_definition = helpers.create_alarm_definition(
            name=name, description="description", expression=expression)
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self.assertEqual(201, resp.status)
        self.assertEqual(name, response_body['name'])
        self.assertEqual(expression, response_body['expression'])

        helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    def test_create_alarm_definition_with_notification(self):
        notification_name = data_utils.rand_name('notification-')
        notification_type = 'EMAIL'
        u_address = 'root@localhost'

        resp, response_body = self.monasca_client.create_notification_method(
            notification_name, type=notification_type, address=u_address)
        self.assertEqual(201, resp.status)
        self.assertEqual(notification_name, response_body['name'])
        notification_id = response_body['id']

        # Create an alarm definition
        alarm_def_name = data_utils.rand_name('monitoring_alarm_definition')
        expression = "mem_total_mb > 0"
        alarm_definition = helpers.create_alarm_definition(
            name=alarm_def_name,
            expression=expression,
            alarm_actions=[notification_id],
            ok_actions=[notification_id],
            undetermined_actions=[notification_id],
            severity="LOW")
        resp, body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self.assertEqual(201, resp.status)
        self.assertEqual(alarm_def_name, body['name'])
        self.assertEqual(expression, body['expression'])
        self.assertEqual(notification_id, body['ok_actions'][0])
        self.assertEqual(notification_id, body['alarm_actions'][0])
        self.assertEqual(notification_id, body['undetermined_actions'][0])

        helpers.delete_alarm_definitions(self)

        # Delete notification
        resp, body = self.monasca_client.delete_notification_method(
            notification_id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    def test_create_alarm_definition_with_multiple_notifications(self):
        notification_name1 = data_utils.rand_name('notification-')
        notification_type1 = 'EMAIL'
        address1 = 'root@localhost'

        notification_name2 = data_utils.rand_name('notification-')
        notification_type2 = 'PAGERDUTY'
        address2 = 'http://localhost.com'

        resp, body = self.monasca_client.create_notification_method(
            notification_name1, type=notification_type1, address=address1)
        self.assertEqual(201, resp.status)
        self.assertEqual(notification_name1, body['name'])
        notification_id1 = body['id']

        resp, body = self.monasca_client.create_notification_method(
            notification_name2, type=notification_type2, address=address2)
        self.assertEqual(201, resp.status)
        self.assertEqual(notification_name2, body['name'])
        notification_id2 = body['id']

        # Create an alarm definition
        alarm_def_name = data_utils.rand_name('monitoring_alarm_definition')
        alarm_definition = helpers.create_alarm_definition(
            name=alarm_def_name,
            expression="mem_total_mb > 0",
            alarm_actions=[notification_id1, notification_id2],
            ok_actions=[notification_id1, notification_id2],
            severity="LOW")
        resp, body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self.assertEqual(201, resp.status)
        self.assertEqual(alarm_def_name, body['name'])
        self.assertEqual("mem_total_mb > 0", body['expression'])

        helpers.delete_alarm_definitions(self)

        # Delete notification 1
        resp, body = self.monasca_client.delete_notification_method(
            notification_id1)
        self.assertEqual(204, resp.status)

        # Delete notification 2
        resp, body = self.monasca_client.delete_notification_method(
            notification_id2)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    def test_create_alarm_definition_with_url_in_expression(self):
        notification_name = data_utils.rand_name('notification-')
        notification_type = 'EMAIL'
        u_address = 'root@localhost'

        resp, body = self.monasca_client.create_notification_method(
            notification_name, type=notification_type, address=u_address)
        self.assertEqual(201, resp.status)
        self.assertEqual(notification_name, body['name'])
        notification_id = body['id']

        # Create an alarm definition
        alarm_def_name = data_utils.rand_name('monitoring_alarm_definition')
        alarm_definition = helpers.create_alarm_definition(
            name=alarm_def_name,
            expression="avg(mem_total_mb{url=https://www.google.com}) gt 0",
            alarm_actions=[notification_id],
            ok_actions=[notification_id],
            severity="LOW")
        resp, body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self.assertEqual(201, resp.status)
        self.assertEqual(alarm_def_name, body['name'])
        self.assertEqual("avg(mem_total_mb{url=https://www.google.com}) gt 0",
                         body['expression'])

        helpers.delete_alarm_definitions(self)

        # Delete notification
        resp, body = self.monasca_client.delete_notification_method(
            notification_id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_alarm_definition_with_special_chars_in_expression(self):
        notification_name = data_utils.rand_name('notification-')
        notification_type = 'EMAIL'
        u_address = 'root@localhost'

        resp, body = self.monasca_client.create_notification_method(
            notification_name, type=notification_type, address=u_address)
        self.assertEqual(201, resp.status)
        self.assertEqual(notification_name, body['name'])
        notification_id = body['id']

        # Create an alarm definition
        alarm_def_name = data_utils.rand_name('monitoring_alarm')
        alarm_definition = helpers.create_alarm_definition(
            name=alarm_def_name,
            expression="avg(mem_total_mb{dev=\usr\local\bin}) "
                       "gt 0",
            alarm_actions=[notification_id],
            ok_actions=[notification_id],
            severity="LOW")
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_alarm_definitions,
                          alarm_definition)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_alarm_definition_with_name_exceeds_max_length(self):
        long_name = "x" * (constants.MAX_ALARM_DEFINITION_NAME_LENGTH + 1)
        expression = "max(cpu.system_perc) > 0"
        alarm_definition = helpers.create_alarm_definition(
            name=long_name, description="description", expression=expression)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_alarm_definitions,
                          alarm_definition)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_alarm_definition_with_description_exceeds_max_length(self):
        name = data_utils.rand_name('alarm_definition')
        long_description = "x" * (constants.
                                  MAX_ALARM_DEFINITION_DESCRIPTION_LENGTH + 1)
        expression = "max(cpu.system_perc) > 0"
        alarm_definition = helpers.create_alarm_definition(
            name=name, description=long_description, expression=expression)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_alarm_definitions,
                          alarm_definition)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_alarm_definition_with_invalid_severity(self):
        invalid_severity = "INVALID"
        name = data_utils.rand_name('alarm_definition')
        expression = "max(cpu.system_perc) > 0"
        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            severity=invalid_severity)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_alarm_definitions,
                          alarm_definition)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_alarm_definition_with_alarm_actions_exceeds_max_length(
            self):
        name = data_utils.rand_name('alarm_definition')
        expression = "max(cpu.system_perc) > 0"
        alarm_actions = ["x" * (
            constants.MAX_ALARM_DEFINITION_ACTIONS_LENGTH + 1)]
        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            alarm_actions=alarm_actions)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_alarm_definitions,
                          alarm_definition)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_alarm_definition_with_ok_actions_exceeds_max_length(self):
        name = data_utils.rand_name('alarm_definition')
        expression = "max(cpu.system_perc) > 0"
        ok_actions = ["x" * (constants.MAX_ALARM_DEFINITION_ACTIONS_LENGTH +
                             1)]
        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            ok_actions=ok_actions)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_alarm_definitions,
                          alarm_definition)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_alarm_definition_with_undeterm_actions_exceeds_max_length(
            self):
        name = data_utils.rand_name('alarm_definition')
        expression = "max(cpu.system_perc) > 0"
        undetermined_actions = ["x" * (constants.
                                       MAX_ALARM_DEFINITION_ACTIONS_LENGTH +
                                       1)]
        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            undetermined_actions=undetermined_actions)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_alarm_definitions,
                          alarm_definition)

    # List

    @test.attr(type="gate")
    def test_list_alarm_definitions(self):
        expression = "avg(cpu_utilization{service=compute}) >= 1234"
        helpers.create_alarm_definitions_with_num(self, expression)
        resp, response_body = self.monasca_client.list_alarm_definitions()
        self.assertEqual(200, resp.status)

        # Test list alarm definition response body
        elements = response_body['elements']
        links = response_body['links']
        link = links[0]
        self.assertEqual(len(elements), NUM_ALARM_DEFINITIONS)
        self.assertTrue(isinstance(response_body, dict))
        self.assertTrue(set(['links', 'elements']) == set(response_body))
        self.assertTrue(isinstance(links, list))
        self.assertTrue(set(['rel', 'href']) == set(link))
        self.assertEqual(link['rel'], u'self')
        for definition in elements:
            self.assertTrue(set(['id',
                                 'links',
                                 'name',
                                 'description',
                                 'expression',
                                 'match_by',
                                 'severity',
                                 'actions_enabled',
                                 'ok_actions',
                                 'alarm_actions',
                                 'undetermined_actions']) ==
                            set(definition))
        helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    def test_list_alarm_definitions_with_name(self):
        name = data_utils.rand_name('alarm_definition')
        description = data_utils.rand_name('description')
        expression = "max(cpu.system_perc) > 0"
        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description=description,
            expression=expression)
        resp, body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self.assertEqual(201, resp.status)

        query_parms = "?name=" + str(name)
        resp, response_body = self.monasca_client.list_alarm_definitions(
            query_parms)
        self.assertEqual(200, resp.status)
        description_new = response_body['elements'][0]['description']
        self.assertEqual(description, description_new)
        expression_new = response_body['elements'][0]['expression']
        self.assertEqual(expression, expression_new)

        helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    def test_list_alarm_definitions_with_dimensions(self):
        # Create an alarm definition with random dimensions
        name = data_utils.rand_name('alarm_definition')
        key = data_utils.rand_name('key')
        value = data_utils.rand_name('value')
        expression = 'avg(cpu_utilization{' + str(key) + '=' + str(value) + \
                     '}) >= 1000'
        alarm_definition = helpers.create_alarm_definition(
            name=name, description="description", expression=expression)
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self.assertEqual(201, resp.status)

        # List alarms
        query_parms = '?dimensions=' + str(key) + ':' + str(value)
        resp, response_body = self.monasca_client.\
            list_alarm_definitions(query_parms)
        elements = response_body['elements']
        name_new = elements[0]['name']
        self.assertEqual(1, len(elements))
        self.assertEqual(name, str(name_new))

        helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    def test_list_alarm_definitions_with_offset_limit(self):
        expression = "max(cpu.system_perc) > 0"
        helpers.create_alarm_definitions_with_num(self, expression)
        resp, response_body = self.monasca_client.list_alarm_definitions()
        self.assertEqual(200, resp.status)
        first_element = response_body['elements'][0]
        last_element = response_body['elements'][1]

        query_parms = '?limit=2'
        resp, response_body = self.monasca_client.list_alarm_definitions(
            query_parms)
        self.assertEqual(200, resp.status)

        elements = response_body['elements']
        self.assertEqual(2, len(elements))
        self.assertEqual(first_element, elements[0])
        self.assertEqual(last_element, elements[1])

        timeout = time.time() + 60 * 1   # 1 minute timeout
        for limit in xrange(1, 3):
            next_element = elements[limit - 1]
            while True:
                if time.time() < timeout:
                    query_parms = '?offset=' + str(next_element['id']) + \
                                  '&limit=' + str(limit)
                    resp, response_body = self.monasca_client.\
                        list_alarm_definitions(query_parms)
                    self.assertEqual(200, resp.status)
                    new_elements = response_body['elements']
                    if len(new_elements) > limit - 1:
                        self.assertEqual(limit, len(new_elements))
                        next_element = new_elements[limit - 1]
                    elif 0 < len(new_elements) <= limit - 1:
                        self.assertEqual(last_element, new_elements[0])
                        break
                    else:
                        self.assertEqual(last_element, next_element)
                        break
                else:
                    msg = "Failed " \
                          "test_list_alarm_definitions_with_offset_limit: " \
                          "one minute timeout"
                    raise exceptions.TimeoutException(msg)

        helpers.delete_alarm_definitions(self)

    # Get

    @test.attr(type="gate")
    def test_get_alarm_definition(self):
        alarm_definition = helpers.\
            create_alarm_definition_for_test_alarm_definition()
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        alarm_def_id = response_body['id']
        resp, response_body = self.monasca_client.get_alarm_definition(
            alarm_def_id)
        self.assertEqual(200, resp.status)

        # Test Get Alarm Definition Response Body
        self.assertTrue(isinstance(response_body, dict))
        self.assertTrue(set(['id', 'links', 'name', 'description',
                             'expression', 'match_by', 'severity',
                             'actions_enabled', 'alarm_actions',
                             'ok_actions', 'undetermined_actions'
                             ]) ==
                        set(response_body))
        links = response_body['links']
        self.assertTrue(isinstance(links, list))
        link = links[0]
        self.assertTrue(set(['rel', 'href']) ==
                        set(link))
        self.assertEqual(link['rel'], u'self')

        helpers.delete_alarm_definitions(self)

    # Update

    @test.attr(type="gate")
    def test_update_alarm_definition(self):
        alarm_definition = helpers.\
            create_alarm_definition_for_test_alarm_definition()
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        id = response_body['id']

        # Update alarm definition
        updated_name = data_utils.rand_name('updated_name')
        updated_description = 'updated description'
        updated_expression = response_body['expression']
        resp, response_body = self.monasca_client.update_alarm_definition(
            id=id,
            name=updated_name,
            expression=updated_expression,
            description=updated_description,
            actions_enabled='true'
        )
        self.assertEqual(200, resp.status)

        # Validate fields updated
        self.assertEqual(updated_name, response_body['name'])
        self.assertEqual(updated_expression, response_body['expression'])
        self.assertEqual(updated_description, response_body['description'])

        # Get and validate details of alarm definition after update
        resp, response_body = self.monasca_client.get_alarm_definition(id)
        self.assertEqual(200, resp.status)
        self.assertEqual(updated_name, response_body['name'])
        self.assertEqual(updated_description, response_body['description'])
        self.assertEqual(updated_expression, response_body['expression'])

        # Test Updated alarm definition Response Body
        self.assertTrue(isinstance(response_body, dict))
        self.assertTrue(set(['id', 'links', 'name', 'description',
                             'expression', 'match_by', 'severity',
                             'actions_enabled', 'alarm_actions',
                             'ok_actions', 'undetermined_actions'
                             ]) ==
                        set(response_body))

        links = response_body['links']
        self.assertTrue(isinstance(links, list))
        link = links[0]
        self.assertTrue(set(['rel', 'href']) ==
                        set(link))
        self.assertEqual(link['rel'], u'self')

        helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_update_alarm_definition_with_a_different_match_by(self):
        alarm_definition = helpers.\
            create_alarm_definition_for_test_alarm_definition()
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        id = response_body['id']
        name = response_body['name']
        expression = response_body['expression']
        description = response_body['description']
        updated_match_by = ['hostname']
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.update_alarm_definition,
                          id=id, name=name, expression=expression,
                          description=description, actions_enabled='true',
                          match_by=updated_match_by)
        helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    def test_update_notification_in_alarm_definition(self):
        notification_name = data_utils.rand_name('notification-')
        notification_type = 'EMAIL'
        u_address = 'root@localhost'

        resp, body = self.monasca_client.create_notification_method(
            notification_name, type=notification_type, address=u_address)
        self.assertEqual(201, resp.status)
        self.assertEqual(notification_name, body['name'])
        notification_id = body['id']

        # Create an alarm definition
        alarm_definition = helpers.\
            create_alarm_definition_for_test_alarm_definition()
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self.assertEqual(201, resp.status)
        alarm_def_id = response_body['id']
        expression = response_body['expression']

        # Update alarm definition
        alarm_def_name = data_utils.rand_name('monitoring_alarm_update')
        resp, body = self.monasca_client.update_alarm_definition(
            alarm_def_id,
            name=alarm_def_name,
            expression=expression,
            actions_enabled='true',
            alarm_actions=[notification_id],
            ok_actions=[notification_id]
        )
        self.assertEqual(200, resp.status)
        self.assertEqual(alarm_def_name, body['name'])
        self.assertEqual(expression, body['expression'])
        self.assertEqual(notification_id, body['alarm_actions'][0])
        self.assertEqual(notification_id, body['ok_actions'][0])

        # Get and verify details of an alarm after update
        resp, body = self.monasca_client.get_alarm_definition(alarm_def_id)
        self.assertEqual(200, resp.status)
        self.assertEqual(alarm_def_name, body['name'])
        self.assertEqual(expression, body['expression'])

        helpers.delete_alarm_definitions(self)

        # Delete notification
        resp, body = self.monasca_client.delete_notification_method(
            notification_id)
        self.assertEqual(204, resp.status)

    # Patch

    @test.attr(type="gate")
    def test_patch_alarm_definition(self):
        alarm_definition = helpers.\
            create_alarm_definition_for_test_alarm_definition()
        resp, response_body = self.monasca_client.\
            create_alarm_definitions(alarm_definition)
        id = response_body['id']

        # Patch alarm definition
        patched_name = data_utils.rand_name('patched_name')
        resp, response_body = self.monasca_client.patch_alarm_definition(
            id=id,
            name=patched_name
        )
        self.assertEqual(200, resp.status)

        # Validate fields updated
        self.assertEqual(patched_name, response_body['name'])

        helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_patch_alarm_definition_with_a_different_match_by(self):
        alarm_definition = helpers.\
            create_alarm_definition_for_test_alarm_definition()
        resp, response_body = self.monasca_client.\
            create_alarm_definitions(alarm_definition)
        id = response_body['id']

        # Patch alarm definition
        patched_match_by = ['hostname']
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.patch_alarm_definition,
                          id=id, match_by=patched_match_by)
        helpers.delete_alarm_definitions(self)

    @test.attr(type="gate")
    def test_patch_actions_in_alarm_definition(self):
        notification_name = data_utils.rand_name('notification-')
        notification_type = 'EMAIL'
        u_address = 'root@localhost'

        resp, body = self.monasca_client.create_notification_method(
            notification_name, type=notification_type, address=u_address)
        self.assertEqual(201, resp.status)
        self.assertEqual(notification_name, body['name'])
        notification_id = body['id']

        # Create an alarm definition
        alarm_definition = helpers.\
            create_alarm_definition_for_test_alarm_definition()
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self.assertEqual(201, resp.status)
        alarm_def_id = response_body['id']
        expression = response_body['expression']

        # Patch alarm definition
        alarm_def_name = data_utils.rand_name('monitoring_alarm_update')
        resp, body = self.monasca_client.patch_alarm_definition(
            alarm_def_id,
            name=alarm_def_name,
            expression=expression,
            actions_enabled='true',
            alarm_actions=[notification_id],
            ok_actions=[notification_id],
            undetermined_actions=[notification_id]
        )
        self.assertEqual(200, resp.status)
        self.assertEqual(alarm_def_name, body['name'])
        self.assertEqual(expression, body['expression'])
        self.assertEqual(notification_id, body['alarm_actions'][0])
        self.assertEqual(notification_id, body['ok_actions'][0])
        self.assertEqual(notification_id, body['undetermined_actions'][0])

        # Get and verify details of an alarm after update
        resp, body = self.monasca_client.get_alarm_definition(alarm_def_id)
        self.assertEqual(200, resp.status)
        self.assertEqual(alarm_def_name, body['name'])
        self.assertEqual(expression, body['expression'])
        self.assertEqual(notification_id, body['alarm_actions'][0])
        self.assertEqual(notification_id, body['ok_actions'][0])
        self.assertEqual(notification_id, body['undetermined_actions'][0])

        helpers.delete_alarm_definitions(self)

        # Delete notification
        resp, body = self.monasca_client.delete_notification_method(
            notification_id)
        self.assertEqual(204, resp.status)

    # Delete

    @test.attr(type="gate")
    def test_create_and_delete_alarm_definition(self):
        alarm_definition = helpers.\
            create_alarm_definition_for_test_alarm_definition()
        resp, response_body = self.monasca_client.\
            create_alarm_definitions(alarm_definition)
        alarm_def_id = response_body['id']

        # Delete alarm definitions
        resp, response_body = self.monasca_client.list_alarm_definitions()
        if 'elements' in response_body:
            elements = response_body['elements']
            if elements:
                for element in elements:
                    current_id = element['id']
                    resp, body = self.monasca_client.delete_alarm_definition(
                        current_id)
                    self.assertEqual(204, resp.status)
                    self.assertRaises(exceptions.NotFound,
                                      self.monasca_client.get_alarm_definition,
                                      alarm_def_id)
