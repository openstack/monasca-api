# (C) Copyright 2015,2016 Hewlett Packard Enterprise Development Company LP
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

import six.moves.urllib.parse as urlparse

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import constants
from monasca_tempest_tests.tests.api import helpers
from tempest.common.utils import data_utils
from tempest import test
from tempest.lib import exceptions

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
            name=name, description="description", expression=expression,
            match_by=['hostname'], severity="MEDIUM")
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self._verify_create_alarm_definitions(resp, response_body,
                                              alarm_definition)

    @test.attr(type="gate")
    def test_create_alarm_definition_with_notification(self):
        notification_name = data_utils.rand_name('notification-')
        notification_type = 'EMAIL'
        notification_address = 'root@localhost'
        resp, response_body = self.monasca_client.create_notification_method(
            name=notification_name, type=notification_type,
            address=notification_address)
        notification_id = self._verify_create_notification_method(
            resp, response_body, notification_name, notification_type,
            notification_address)

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
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self._verify_create_alarm_definitions(resp, response_body,
                                              alarm_definition)
        self.assertEqual(notification_id, response_body['ok_actions'][0])
        self.assertEqual(notification_id, response_body['alarm_actions'][0])
        self.assertEqual(notification_id,
                         response_body['undetermined_actions'][0])

        self._delete_notification(notification_id)

    @test.attr(type="gate")
    def test_create_alarm_definition_with_multiple_notifications(self):
        notification_name1 = data_utils.rand_name('notification-')
        notification_type1 = 'EMAIL'
        address1 = 'root@localhost'

        notification_name2 = data_utils.rand_name('notification-')
        notification_type2 = 'PAGERDUTY'
        address2 = 'http://localhost.com'

        resp, response_body = self.monasca_client.create_notification_method(
            notification_name1, type=notification_type1, address=address1)
        notification_id1 = self._verify_create_notification_method(
            resp, response_body, notification_name1, notification_type1,
            address1)

        resp, response_body = self.monasca_client.create_notification_method(
            notification_name2, type=notification_type2, address=address2)
        notification_id2 = self._verify_create_notification_method(
            resp, response_body, notification_name2, notification_type2,
            address2)

        # Create an alarm definition
        alarm_def_name = data_utils.rand_name('monitoring_alarm_definition')
        alarm_definition = helpers.create_alarm_definition(
            name=alarm_def_name,
            expression="mem_total_mb > 0",
            alarm_actions=[notification_id1, notification_id2],
            ok_actions=[notification_id1, notification_id2],
            undetermined_actions=[notification_id1, notification_id2],
            severity="LOW")
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self._verify_create_alarm_definitions(resp, response_body,
                                              alarm_definition)

        self._delete_notification(notification_id1)
        self._delete_notification(notification_id2)

    @test.attr(type="gate")
    def test_create_alarm_definition_with_url_in_expression(self):
        notification_name = data_utils.rand_name('notification-')
        notification_type = 'EMAIL'
        address = 'root@localhost'
        resp, response_body = self.monasca_client.create_notification_method(
            notification_name, type=notification_type, address=address)
        notification_id = self._verify_create_notification_method(
            resp, response_body, notification_name, notification_type, address)

        # Create an alarm definition
        alarm_def_name = data_utils.rand_name('monitoring_alarm_definition')
        alarm_definition = helpers.create_alarm_definition(
            name=alarm_def_name,
            expression="avg(mem_total_mb{url=https://www.google.com}) gt 0",
            alarm_actions=[notification_id],
            ok_actions=[notification_id],
            undetermined_actions=[notification_id],
            severity="LOW")
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition)
        self._verify_create_alarm_definitions(resp, response_body,
                                              alarm_definition)
        self._delete_notification(notification_id)

    @test.attr(type='gate')
    def test_create_deterministic_alarm_definition(self):
        name = data_utils.rand_name('log.error')
        expression = "count(log.error{},deterministic) > 0"

        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            match_by=['hostname'],
            severity="MEDIUM"
        )
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition
        )
        self._verify_create_alarm_definitions(resp,
                                              response_body,
                                              alarm_definition,
                                              deterministic=True)

    @test.attr(type='gate')
    def test_create_non_deterministic_alarm_definition_compound_mixed_expr(self):
        name = data_utils.rand_name('log.error.and.disk.used_perc')
        expression = ('max(disk.used_perc{hostname=node_1}) > 99.0 AND '
                      'count(log.error{hostname=node_1},deterministic) > 0')

        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            match_by=['hostname'],
            severity="MEDIUM"
        )
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition
        )
        self._verify_create_alarm_definitions(resp,
                                              response_body,
                                              alarm_definition,
                                              deterministic=False)

    @test.attr(type='gate')
    def test_create_deterministic_alarm_definition_compound_expr(self):
        name = data_utils.rand_name('log.error.nodes_1_2')
        expression = ('count(log.error{hostname=node_2},deterministic) > 0 '
                      'AND '
                      'count(log.error{hostname=node_1},deterministic) > 0')

        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            match_by=['hostname'],
            severity="MEDIUM"
        )
        resp, response_body = self.monasca_client.create_alarm_definitions(
            alarm_definition
        )
        self._verify_create_alarm_definitions(resp,
                                              response_body,
                                              alarm_definition,
                                              deterministic=True)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_alarm_definition_with_special_chars_in_expression(self):
        notification_name = data_utils.rand_name('notification-')
        notification_type = 'EMAIL'
        address = 'root@localhost'

        resp, response_body = self.monasca_client.create_notification_method(
            notification_name, type=notification_type, address=address)
        notification_id = self._verify_create_notification_method(
            resp, response_body, notification_name, notification_type, address)

        # Create an alarm definition
        alarm_def_name = data_utils.rand_name('monitoring_alarm')
        alarm_definition = helpers.create_alarm_definition(
            name=alarm_def_name,
            expression="avg(mem_total_mb{dev=\usr\local\bin}) "
                       "gt 0",
            alarm_actions=[notification_id],
            ok_actions=[notification_id],
            undetermined_actions=[notification_id],
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
        response_body_list = self._create_alarm_definitions(
            expression=expression, number_of_definitions=1)
        query_param = '?name=' + str(response_body_list[0]['name'])
        resp, response_body = self.monasca_client.list_alarm_definitions(
            query_param)
        self._verify_list_alarm_definitions_response_body(resp, response_body)

        # Test list alarm definition response body
        elements = response_body['elements']
        self._verify_alarm_definitions_list(elements, response_body_list)
        links = response_body['links']
        self._verify_list_alarm_definitions_links(links)

    @test.attr(type="gate")
    def test_list_alarm_definitions_with_name(self):
        name = data_utils.rand_name('alarm_definition')
        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description=data_utils.rand_name('description'),
            expression="max(cpu.system_perc) > 0")
        resp, res_body_create_alarm_def = self.monasca_client.\
            create_alarm_definitions(alarm_definition)
        self.assertEqual(201, resp.status)

        query_parms = "?name=" + str(name)
        resp, response_body = self.monasca_client.list_alarm_definitions(
            query_parms)
        self._verify_list_alarm_definitions_response_body(resp, response_body)
        elements = response_body['elements']
        self._verify_alarm_definitions_list(
            elements, [res_body_create_alarm_def])
        links = response_body['links']
        self._verify_list_alarm_definitions_links(links)

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
        resp, res_body_create_alarm_def = self.monasca_client.\
            create_alarm_definitions(alarm_definition)
        self.assertEqual(201, resp.status)

        # List alarms
        query_parms = '?dimensions=' + str(key) + ':' + str(value)
        resp, response_body = self.monasca_client.\
            list_alarm_definitions(query_parms)
        self._verify_list_alarm_definitions_response_body(resp, response_body)
        elements = response_body['elements']
        self._verify_alarm_definitions_list(elements, [res_body_create_alarm_def])
        links = response_body['links']
        self._verify_list_alarm_definitions_links(links)

    @test.attr(type="gate")
    def test_list_alarm_definitions_with_multiple_dimensions(self):
        # Create an alarm definition with random dimensions
        name = data_utils.rand_name('alarm_definition')
        dimensions = {data_utils.rand_name('key-1'): data_utils.rand_name('value-1'),
                      data_utils.rand_name('key-2'): data_utils.rand_name('value-2')}
        dimension_strings = [key + '=' + value for key, value in dimensions.items()]
        expression = 'avg(cpu_utilization{' + ','.join(dimension_strings) + '}) >= 1000'

        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression)
        resp, res_body_create_alarm_def = self.monasca_client. \
            create_alarm_definitions(alarm_definition)
        self.assertEqual(201, resp.status)

        # List alarms
        query_dimensions = [key + ':' + value for key, value in dimensions.items()]
        query_parms = '?dimensions=' + ','.join(query_dimensions)
        resp, response_body = self.monasca_client.list_alarm_definitions(query_parms)
        self._verify_list_alarm_definitions_response_body(resp, response_body)

        elements = response_body['elements']
        self._verify_alarm_definitions_list(elements, [res_body_create_alarm_def])

        links = response_body['links']
        self._verify_list_alarm_definitions_links(links)

    @test.attr(type="gate")
    def test_list_alarm_definitions_with_severity(self):
        name = data_utils.rand_name('alarm_definition')
        expression = 'avg(cpu_utilization) >= 1000'
        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            severity="LOW")
        resp, res_body_create_alarm_def = self.monasca_client.\
            create_alarm_definitions(alarm_definition)
        self.assertEqual(201, resp.status)

        name = data_utils.rand_name('alarm_definition')
        expression = 'avg(cpu_utilization) >= 1000'
        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            severity="MEDIUM")
        resp, res_body_create_alarm_def = self.monasca_client.\
            create_alarm_definitions(alarm_definition)
        self.assertEqual(201, resp.status)

        query_param = '?severity=MEDIUM'
        resp, response_body = self.monasca_client.\
            list_alarm_definitions(query_param)
        self._verify_list_alarm_definitions_response_body(resp, response_body)
        elements = response_body['elements']
        self._verify_alarm_definitions_list(elements, [res_body_create_alarm_def])
        links = response_body['links']
        self._verify_list_alarm_definitions_links(links)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_alarm_definitions_by_severity_invalid_severity(self):
        query_parms = '?severity=false_severity'
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_alarm_definitions, query_parms)

    @test.attr(type="gate")
    def test_list_alarm_definitions_with_multiple_severity(self):
        name = data_utils.rand_name('alarm_definition')
        expression = 'avg(cpu_utilization{alarm=severity}) >= 1000'
        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            severity="LOW")
        resp, res_body_create_alarm_def_low = self.monasca_client.\
            create_alarm_definitions(alarm_definition)
        self.assertEqual(201, resp.status)

        name = data_utils.rand_name('alarm_definition')
        expression = 'avg(cpu_utilization{alarm=severity}) >= 1000'
        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            severity="MEDIUM")
        resp, res_body_create_alarm_def_medium = self.monasca_client.\
            create_alarm_definitions(alarm_definition)
        self.assertEqual(201, resp.status)

        name = data_utils.rand_name('alarm_definition')
        expression = 'avg(cpu_utilization{alarm=severity}) >= 1000'
        alarm_definition = helpers.create_alarm_definition(
            name=name,
            description="description",
            expression=expression,
            severity="HIGH")
        resp, res_body_create_alarm_def = self.monasca_client.\
            create_alarm_definitions(alarm_definition)
        self.assertEqual(201, resp.status)


        query_param = '?severity=MEDIUM|LOW&dimensions=alarm:severity&sort_by=severity'
        resp, response_body = self.monasca_client.\
            list_alarm_definitions(query_param)
        self._verify_list_alarm_definitions_response_body(resp, response_body)
        elements = response_body['elements']
        self._verify_alarm_definitions_list(elements, [res_body_create_alarm_def_low,
                                                       res_body_create_alarm_def_medium])
        links = response_body['links']
        self._verify_list_alarm_definitions_links(links)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_alarm_definitions_by_severity_multiple_values_invalid_severity(self):
        query_parms = '?severity=false_severity|MEDIUM'
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_alarm_definitions, query_parms)

        query_parms = '?severity=MEDIUM|false_severity'
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_alarm_definitions, query_parms)

        query_parms = '?severity=LOW|false_severity|HIGH'
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_alarm_definitions, query_parms)

    @test.attr(type='gate')
    def test_list_alarm_definitions_sort_by(self):
        key = data_utils.rand_name('key')
        value = data_utils.rand_name('value')
        expression = 'avg(cpu_utilization{' + str(key) + '=' + str(value) + \
                     '}) >= 1000'

        alarm_definitions = [helpers.create_alarm_definition(
            name='alarm def sort by 01',
            expression=expression,
            severity='HIGH'
        ), helpers.create_alarm_definition(
            name='alarm def sort by 04',
            expression=expression,
            severity='LOW'
        ), helpers.create_alarm_definition(
            name='alarm def sort by 02',
            expression=expression,
            severity='CRITICAL'
        ), helpers.create_alarm_definition(
            name='alarm def sort by 03',
            expression=expression,
            severity='MEDIUM'
        )]
        for definition in alarm_definitions:
            resp, response_body = self.monasca_client.create_alarm_definitions(definition)
            definition['id'] = response_body['id']
            time.sleep(1)

        sort_params1 = ['id', 'name', 'severity']
        for sort_by in sort_params1:
            alarmdefs_sort_by = sorted(alarm_definitions, key=lambda
                definition: definition[sort_by])

            resp, response_body = self.monasca_client.list_alarm_definitions(
                '?dimensions=' + str(key) + ':' + str(value) +
                '&sort_by=' + sort_by)
            self.assertEqual(200, resp.status)
            for i, element in enumerate(response_body['elements']):
                self.assertEqual(alarmdefs_sort_by[i][sort_by], element[sort_by])

            resp, response_body = self.monasca_client.list_alarm_definitions(
                '?dimensions=' + str(key) + ':' + str(value) +
                '&sort_by=' + sort_by + urlparse.quote(' asc'))
            self.assertEqual(200, resp.status)
            for i, element in enumerate(response_body['elements']):
                self.assertEqual(alarmdefs_sort_by[i][sort_by], element[sort_by])

            alarmdefs_sort_by_reverse = sorted(alarm_definitions, key=lambda
                definition: definition[sort_by], reverse=True)

            resp, response_body = self.monasca_client.list_alarm_definitions(
                '?dimensions=' + str(key) + ':' + str(value) +
                '&sort_by=' + sort_by + urlparse.quote(' desc'))
            self.assertEqual(200, resp.status)
            for i, element in enumerate(response_body['elements']):
                self.assertEqual(alarmdefs_sort_by_reverse[i][sort_by], element[sort_by])

        sort_params2 = ['created_at', 'updated_at']
        for sort_by in sort_params2:
            resp, response_body = self.monasca_client.list_alarm_definitions(
                '?dimensions=' + str(key) + ':' + str(value) +
                '&sort_by=' + sort_by)
            self.assertEqual(200, resp.status)
            for i, element in enumerate(response_body['elements']):
                self.assertEqual(alarm_definitions[i]['id'], element['id'])

            resp, response_body = self.monasca_client.list_alarm_definitions(
                '?dimensions=' + str(key) + ':' + str(value) +
                '&sort_by=' + sort_by + urlparse.quote(' asc'))
            self.assertEqual(200, resp.status)
            for i, element in enumerate(response_body['elements']):
                self.assertEqual(alarm_definitions[i]['id'], element['id'])

            resp, response_body = self.monasca_client.list_alarm_definitions(
                '?dimensions=' + str(key) + ':' + str(value) +
                '&sort_by=' + sort_by + urlparse.quote(' desc'))
            self.assertEqual(200, resp.status)
            for i, element in enumerate(response_body['elements']):
                self.assertEqual(alarm_definitions[-i-1]['id'], element['id'])

    @test.attr(type='gate')
    def test_list_alarm_definitions_multiple_sort_by(self):
        key = data_utils.rand_name('key')
        value = data_utils.rand_name('value')
        expression = 'avg(cpu_utilization{' + str(key) + '=' + str(value) + \
                     '}) >= 1000'

        alarm_definitions = [helpers.create_alarm_definition(
            name='alarm def sort by 11',
            expression=expression,
            severity='MEDIUM'
        ), helpers.create_alarm_definition(
            name='alarm def sort by 14',
            expression=expression,
            severity='MEDIUM'
        ), helpers.create_alarm_definition(
            name='alarm def sort by 12',
            expression=expression,
            severity='LOW'
        ), helpers.create_alarm_definition(
            name='alarm def sort by 13',
            expression=expression,
            severity='MEDIUM'
        )]
        for definition in alarm_definitions:
            resp, response_body = self.monasca_client.create_alarm_definitions(definition)
            definition['id'] = response_body['id']

        resp, response_body = self.monasca_client.list_alarm_definitions(
            '?dimensions=' + str(key) + ':' + str(value) +
            '&sort_by=' + urlparse.quote('severity asc,name desc,id'))
        self.assertEqual(200, resp.status)

        expected_order = [2, 1, 3, 0]

        for i, element in enumerate(response_body['elements']):
            self.assertEqual(alarm_definitions[expected_order[i]]['id'], element['id'])

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_list_alarm_definitions_invalid_sort_by(self):
        query_parms = '?sort_by=random'
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_alarm_definitions, query_parms)

    @test.attr(type="gate")
    def test_list_alarm_definitions_with_offset_limit(self):
        helpers.delete_alarm_definitions(self.monasca_client)
        expression = "max(cpu.system_perc) > 0"
        self._create_alarm_definitions(
            expression=expression, number_of_definitions=NUM_ALARM_DEFINITIONS)
        resp, response_body = self.monasca_client.list_alarm_definitions()
        self._verify_list_alarm_definitions_response_body(resp, response_body)
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

        for offset in xrange(0, 2):
            for limit in xrange(1, 3 - offset):
                query_parms = '?offset=' + str(offset) + '&limit=' + str(limit)
                resp, response_body = self.monasca_client.list_alarm_definitions(query_parms)
                self.assertEqual(200, resp.status)
                new_elements = response_body['elements']
                self.assertEqual(limit, len(new_elements))
                self.assertEqual(elements[offset], new_elements[0])
                self.assertEqual(elements[offset+limit-1], new_elements[-1])
                links = response_body['links']
                for link in links:
                    if link['rel'] == 'next':
                        next_offset = helpers.get_query_param(link['href'], 'offset')
                        next_limit = helpers.get_query_param(link['href'], 'limit')
                        self.assertEqual(str(offset + limit), next_offset)
                        self.assertEqual(str(limit), next_limit)

    # Get

    @test.attr(type="gate")
    def test_get_alarm_definition(self):
        response_body_list = self._create_alarm_definitions(
            expression=None, number_of_definitions=1)
        resp, response_body = self.monasca_client.get_alarm_definition(
            response_body_list[0]['id'])
        self.assertEqual(200, resp.status)
        self._verify_element_set(response_body)
        self._verify_alarm_definitions_element(response_body,
                                               response_body_list[0])
        links = response_body['links']
        self._verify_list_alarm_definitions_links(links)

    # Update

    @test.attr(type="gate")
    def test_update_alarm_definition(self):
        notification_name = data_utils.rand_name('notification-')
        notification_type = 'EMAIL'
        address = 'root@localhost'

        resp, response_body = self.monasca_client.create_notification_method(
            notification_name, type=notification_type, address=address)
        notification_id = self._verify_create_notification_method(
            resp, response_body, notification_name, notification_type, address)

        response_body_list = self._create_alarm_definitions(
            expression=None, number_of_definitions=1)
        # Update alarm definition
        updated_name = data_utils.rand_name('updated_name')
        updated_description = 'updated description'
        updated_expression = "max(cpu.system_perc) < 0"

        resp, response_body = self.monasca_client.update_alarm_definition(
            str(response_body_list[0]['id']), updated_name, updated_expression,
            updated_description, True, response_body_list[0]['match_by'],
            'LOW', [notification_id], [notification_id], [notification_id])
        self.assertEqual(200, resp.status)
        self._verify_update_patch_alarm_definition(response_body, updated_name,
                                                   updated_expression,
                                                   updated_description, None)
        # Validate fields updated
        resp, response_body = self.monasca_client.get_alarm_definition(
            response_body_list[0]['id'])
        self._verify_update_patch_alarm_definition(response_body, updated_name,
                                                   updated_expression,
                                                   updated_description, None)
        links = response_body['links']
        self._verify_list_alarm_definitions_links(links)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_update_alarm_definition_with_a_different_match_by(self):
        response_body_list = self._create_alarm_definitions(
            expression=None, number_of_definitions=1)
        name = response_body_list[0]['name']
        expression = response_body_list[0]['expression']
        description = response_body_list[0]['description']
        updated_match_by = ['hostname']
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.update_alarm_definition,
                          response_body_list[0]['id'], name, expression,
                          description, 'true', updated_match_by, 'LOW', None,
                          None, None)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_update_alarm_definition_with_no_ok_actions(self):
        response_body_list = self._create_alarm_definitions(
            expression=None, number_of_definitions=1)
        name = response_body_list[0]['name']
        expression = response_body_list[0]['expression']
        description = response_body_list[0]['description']
        updated_match_by = ['hostname']
        self.assertRaises(
            exceptions.UnprocessableEntity,
            self.monasca_client.update_alarm_definition_with_no_ok_actions,
            response_body_list[0]['id'], name, expression, description,
            'true', updated_match_by, 'LOW', None, None)

    @test.attr(type="gate")
    def test_update_notification_in_alarm_definition(self):
        notification_name = data_utils.rand_name('notification-')
        notification_type = 'EMAIL'
        address = 'root@localhost'

        resp, response_body = self.monasca_client.create_notification_method(
            notification_name, type=notification_type, address=address)
        notification_id = self._verify_create_notification_method(
            resp, response_body, notification_name, notification_type, address)

        # Create an alarm definition
        response_body_list = self._create_alarm_definitions(
            expression=None, number_of_definitions=1)
        expression = response_body_list[0]['expression']

        # Update alarm definition
        update_alarm_def_name = data_utils.rand_name('monitoring_alarm_update')
        resp, response_body = self.monasca_client.update_alarm_definition(
            response_body_list[0]['id'], update_alarm_def_name, expression,
            'description', True, response_body_list[0]['match_by'], 'LOW',
            [notification_id], [notification_id], [notification_id])
        self.assertEqual(200, resp.status)
        self._verify_update_patch_alarm_definition(response_body,
                                                   update_alarm_def_name,
                                                   None, None, notification_id)
        # Get and verify details of an alarm after update
        resp, response_body = self.monasca_client.get_alarm_definition(
            response_body_list[0]['id'])
        self._verify_update_patch_alarm_definition(response_body,
                                                   update_alarm_def_name,
                                                   None, None, notification_id)
        self._delete_notification(notification_id)

    # Patch

    @test.attr(type="gate")
    def test_patch_alarm_definition(self):
        response_body_list = self._create_alarm_definitions(
            expression=None, number_of_definitions=1)
        # Patch alarm definition
        patched_name = data_utils.rand_name('patched_name')
        resp, response_body = self.monasca_client.patch_alarm_definition(
            id=response_body_list[0]['id'],
            name=patched_name
        )
        self.assertEqual(200, resp.status)
        self._verify_update_patch_alarm_definition(response_body, patched_name,
                                                   None, None, None)
        # Validate fields updated
        resp, response_body = self.monasca_client.get_alarm_definition(
            response_body_list[0]['id'])
        self._verify_update_patch_alarm_definition(response_body, patched_name,
                                                   None, None, None)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_patch_alarm_definition_with_a_different_match_by(self):
        response_body_list = self._create_alarm_definitions(
            expression=None, number_of_definitions=1)
        # Patch alarm definition
        patched_match_by = ['hostname']
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.patch_alarm_definition,
                          id=response_body_list[0]['id'],
                          match_by=patched_match_by)

    @test.attr(type="gate")
    def test_patch_actions_in_alarm_definition(self):
        notification_name = data_utils.rand_name('notification-')
        notification_type = 'EMAIL'
        address = 'root@localhost'

        resp, response_body = self.monasca_client.create_notification_method(
            notification_name, type=notification_type, address=address)
        notification_id = self._verify_create_notification_method(
            resp, response_body, notification_name, notification_type, address)

        # Create an alarm definition
        response_body_list = self._create_alarm_definitions(
            expression=None, number_of_definitions=1)
        # Patch alarm definition
        patch_alarm_def_name = data_utils.rand_name('monitoring_alarm_update')
        resp, body = self.monasca_client.patch_alarm_definition(
            response_body_list[0]['id'],
            name=patch_alarm_def_name,
            expression=response_body_list[0]['expression'],
            actions_enabled='true',
            alarm_actions=[notification_id],
            ok_actions=[notification_id],
            undetermined_actions=[notification_id]
        )
        self.assertEqual(200, resp.status)
        self._verify_update_patch_alarm_definition(body, patch_alarm_def_name,
                                                   None, None, notification_id)
        # Get and verify details of an alarm after update
        resp, response_body = self.monasca_client.get_alarm_definition(
            response_body_list[0]['id'])
        self._verify_update_patch_alarm_definition(response_body,
                                                   patch_alarm_def_name, None,
                                                   None, notification_id)
        self._delete_notification(notification_id)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_patch_alarm_definition_with_invalid_actions(self):
        response_body_list = self._create_alarm_definitions(
            expression=None, number_of_definitions=1)
        # Patch alarm definition
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.patch_alarm_definition,
                          id=response_body_list[0]['id'],
                          alarm_actions=['bad_notification_id'])

        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.patch_alarm_definition,
                          id=response_body_list[0]['id'],
                          ok_actions=['bad_notification_id'])

        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.patch_alarm_definition,
                          id=response_body_list[0]['id'],
                          undetermined_actions=['bad_notification_id'])

    # Delete

    @test.attr(type="gate")
    def test_create_and_delete_alarm_definition(self):
        response_body_list = self._create_alarm_definitions(
            expression=None, number_of_definitions=1)
        # Delete alarm definitions
        resp, response_body = self.monasca_client.list_alarm_definitions()
        self._verify_list_alarm_definitions_response_body(resp, response_body)
        elements = response_body['elements']
        for element in elements:
            if element['id'] == response_body_list[0]['id']:
                resp, body = self.monasca_client.delete_alarm_definition(
                    response_body_list[0]['id'])
                self.assertEqual(204, resp.status)
                self.assertRaises(exceptions.NotFound,
                                  self.monasca_client.get_alarm_definition,
                                  response_body_list[0]['id'])
                return
        self.fail("Failed test_create_and_delete_alarm_definition: "
                  "cannot find the alarm definition just created.")

    def _create_alarm_definitions(self, expression, number_of_definitions):
        self.rule = {'expression': 'mem_total_mb > 0'}
        if expression is None:
            expression = "max(cpu.system_perc) > 0"
        response_body_list = []
        for i in xrange(number_of_definitions):
            alarm_definition = helpers.create_alarm_definition(
                name=data_utils.rand_name('alarm_definition'),
                description=data_utils.rand_name('description'),
                expression=expression,
                match_by=['device'])
            resp, response_body = self.monasca_client.create_alarm_definitions(
                alarm_definition)
            self.assertEqual(201, resp.status)
            response_body_list.append(response_body)
        return response_body_list

    def _verify_list_alarm_definitions_response_body(self, resp,
                                                     response_body):
        self.assertEqual(200, resp.status)
        self.assertIsInstance(response_body, dict)
        self.assertTrue(set(['links', 'elements']) == set(response_body))

    def _verify_alarm_definitions_list(self, observed, reference):
        self.assertEqual(len(reference), len(observed))
        for i in xrange(len(reference)):
            self._verify_alarm_definitions_element(
                reference[i], observed[i])

    def _verify_alarm_definitions_element(self, response_body,
                                          res_body_create_alarm_def):
        self._verify_element_set(response_body)
        self.assertEqual(response_body['name'],
                         res_body_create_alarm_def['name'])
        self.assertEqual(response_body['expression'],
                         res_body_create_alarm_def['expression'])
        self.assertEqual(response_body['id'], res_body_create_alarm_def['id'])
        self.assertEqual(response_body['description'],
                         res_body_create_alarm_def['description'])
        self.assertEqual(response_body['match_by'],
                         res_body_create_alarm_def['match_by'])
        self.assertEqual(response_body['severity'],
                         res_body_create_alarm_def['severity'])
        self.assertEqual(response_body['deterministic'],
                         res_body_create_alarm_def['deterministic'])

    def _verify_element_set(self, element):
        self.assertTrue(set(['id',
                             'links',
                             'name',
                             'description',
                             'expression',
                             'deterministic',
                             'match_by',
                             'severity',
                             'actions_enabled',
                             'ok_actions',
                             'alarm_actions',
                             'undetermined_actions']) ==
                        set(element))

    def _verify_list_alarm_definitions_links(self, links):
        self.assertIsInstance(links, list)
        link = links[0]
        self.assertTrue(set(['rel', 'href']) == set(link))
        self.assertEqual(link['rel'], u'self')

    def _verify_create_alarm_definitions(self,
                                         resp,
                                         response_body,
                                         alarm_definition,
                                         deterministic=False):
        self.assertEqual(201, resp.status)
        self.assertEqual(alarm_definition['name'], response_body['name'])

        self.assertEqual(alarm_definition['expression'],
                         str(response_body['expression']))
        self.assertEqual(deterministic, bool(response_body['deterministic']))

        if 'description' in alarm_definition:
            self.assertEqual(alarm_definition['description'],
                             str(response_body['description']))
        else:
            self.assertEqual('', str(response_body['description']))
        if 'match_by' in alarm_definition:
            self.assertEqual(alarm_definition['match_by'],
                             response_body['match_by'])
        else:
            self.assertEqual([], response_body['match_by'])
        if 'severity' in alarm_definition:
            self.assertEqual(alarm_definition['severity'],
                             str(response_body['severity']))
        else:
            self.assertEqual('LOW', str(response_body['severity']))

    def _verify_update_patch_alarm_definition(self, response_body,
                                              updated_name, updated_expression,
                                              updated_description,
                                              notification_id):
        if updated_name is not None:
            self.assertEqual(updated_name, response_body['name'])
        if updated_expression is not None:
            self.assertEqual(updated_expression, response_body['expression'])
        if updated_description is not None:
            self.assertEqual(updated_description, response_body['description'])
        if notification_id is not None:
            self.assertEqual(notification_id,
                             response_body['alarm_actions'][0])
            self.assertEqual(notification_id, response_body['ok_actions'][0])
            self.assertEqual(notification_id,
                             response_body['undetermined_actions'][0])

    def _delete_notification(self, notification_id):
        resp, body = self.monasca_client.delete_notification_method(
            notification_id)
        self.assertEqual(204, resp.status)

    def _verify_create_notification_method(
            self, resp, response_body, test_name, test_type, test_address):
        self.assertEqual(201, resp.status)
        self.assertEqual(test_name, response_body['name'])
        self.assertEqual(test_type, response_body['type'])
        self.assertEqual(test_address, response_body['address'])
        notification_id = response_body['id']
        return notification_id
