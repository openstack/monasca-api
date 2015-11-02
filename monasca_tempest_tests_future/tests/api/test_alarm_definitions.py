# # (C) Copyright 2015 Hewlett Packard Enterprise Development Company LP
# #
# # Licensed under the Apache License, Version 2.0 (the "License"); you may
# # not use this file except in compliance with the License. You may obtain
# # a copy of the License at
# #
# #      http://www.apache.org/licenses/LICENSE-2.0
# #
# # Unless required by applicable law or agreed to in writing, software
# # distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# # WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# # License for the specific language governing permissions and limitations
# # under the License.
#
# from monasca_tempest_tests.tests.api import base
# from monasca_tempest_tests.tests.api import helpers
# from tempest.common.utils import data_utils
# from tempest import test
# from tempest_lib import exceptions
#
# NUM_ALARM_DEFINITIONS = 2
#
#
# class TestAlarmDefinitions(base.BaseMonascaTest):
#     @classmethod
#     def resource_setup(cls):
#         super(TestAlarmDefinitions, cls).resource_setup()
#         cls.rule = {'expression': 'mem_total_mb > 0'}
#         for i in range(NUM_ALARM_DEFINITIONS):
#             alarm_definition = helpers.create_alarm_definition(
#                 name='alarm-definition-' + str(i),
#                 description='alarm definition description',
#                 expression='avg(cpu_utilization{service=compute}) >= 1000')
#             cls.monasca_client.create_alarm_definitions(alarm_definition)
#
#     @classmethod
#     def create_alarm_definition(cls):
#         # Create an alarm definition
#         name = data_utils.rand_name('alarm_definition')
#         expression = "max(cpu.system_perc) > 0"
#         alarm_definition = helpers.create_alarm_definition(
#             name=name,
#             description="description",
#             expression=expression)
#         return alarm_definition
#
#     # Create
#     @test.attr(type="gate")
#     def test_create_alarm_definition(self):
#         # Create an alarm definition
#         name = data_utils.rand_name('alarm_definition')
#         expression = "max(cpu.system_perc) > 0"
#         alarm_definition = helpers.create_alarm_definition(
#             name=name, description="description", expression=expression)
#         resp, response_body = self.monasca_client.create_alarm_definitions(
#             alarm_definition)
#         self.assertEqual(201, resp.status)
#         self.assertEqual(name, response_body['name'])
#         alarm_def_id = response_body['id']
#         self.assertEqual(expression, response_body['expression'])
#
#         # Delete alarm and verify if deleted
#         resp, response_body = self.monasca_client.delete_alarm_definition(
#             alarm_def_id)
#         self.assertEqual(204, resp.status)
#         self.assertRaises(exceptions.NotFound,
#                           self.monasca_client.get_alarm_definition,
#                           alarm_def_id)
#
#     @test.attr(type="gate")
#     def test_create_alarm_definition_with_notification(self):
#         notification_name = data_utils.rand_name('notification-')
#         notification_type = 'EMAIL'
#         u_address = 'root@localhost'
#
#         resp, response_body = self.monasca_client.create_notification_method(
#             notification_name, type=notification_type, address=u_address)
#         self.assertEqual(201, resp.status)
#         self.assertEqual(notification_name, response_body['name'])
#         notification_id = response_body['id']
#
#         # Create an alarm definition
#         alarm_def_name = data_utils.rand_name('monitoring_alarm_definition')
#         expression = "mem_total_mb > 0"
#         alarm_definition = helpers.create_alarm_definition(
#             name=alarm_def_name,
#             expression=expression,
#             alarm_actions=notification_id,
#             ok_actions=notification_id,
#             undetermined_actions=notification_id,
#             severity="LOW")
#         resp, body = self.monasca_client.create_alarm_definitions(
#             alarm_definition)
#         self.assertEqual(201, resp.status)
#         self.assertEqual(alarm_def_name, body['name'])
#         alarm_def_id = body['id']
#         self.assertEqual(expression, body['expression'])
#         self.assertEqual(notification_id, body['ok_actions'][0])
#         self.assertEqual(notification_id, body['alarm_actions'][0])
#         self.assertEqual(notification_id, body['undetermined_actions'][0])
#
#         # Delete alarm definition and verify if deleted
#         resp, body = self.monasca_client.delete_alarm_definition(
#             alarm_def_id)
#         self.assertEqual(204, resp.status)
#         self.assertRaises(exceptions.NotFound,
#                           self.monasca_client.get_alarm_definition,
#                           alarm_def_id)
#
#         # Delete notification
#         resp, body = self.monasca_client.delete_notification_method(
#             notification_id)
#         self.assertEqual(204, resp.status)
#
#     @test.attr(type="gate")
#     def test_create_alarm_definition_with_multiple_notifications(self):
#         notification_name1 = data_utils.rand_name('notification-')
#         notification_type1 = 'EMAIL'
#         address1 = 'root@localhost'
#
#         notification_name2 = data_utils.rand_name('notification-')
#         notification_type2 = 'PAGERDUTY'
#         address2 = 'http://localhost.com'
#
#         resp, body = self.monasca_client.create_notification_method(
#             notification_name1, type=notification_type1, address=address1)
#         self.assertEqual(201, resp.status)
#         self.assertEqual(notification_name1, body['name'])
#         notification_id1 = body['id']
#
#         resp, body = self.monasca_client.create_notification_method(
#             notification_name2, type=notification_type2, address=address2)
#         self.assertEqual(201, resp.status)
#         self.assertEqual(notification_name2, body['name'])
#         notification_id2 = body['id']
#
#         # Create an alarm definition
#         alarm_def_name = data_utils.rand_name('monitoring_alarm_definition')
#         alarm_definition = helpers.create_alarm_definition(
#             name=alarm_def_name,
#             expression="mem_total_mb > 0",
#             alarm_actions=[notification_id1, notification_id2],
#             ok_actions=[notification_id1, notification_id2],
#             severity="LOW")
#         resp, body = self.monasca_client.create_alarm_definitions(
#             alarm_definition)
#         self.assertEqual(201, resp.status)
#         self.assertEqual(alarm_def_name, body['name'])
#         alarm_def_id = body['id']
#         self.assertEqual("mem_total_mb > 0", body['expression'])
#
#         # Delete alarm definition and validate if deleted
#         resp, body = self.monasca_client.delete_alarm_definition(
#             alarm_def_id)
#         self.assertEqual(204, resp.status)
#         self.assertRaises(exceptions.NotFound,
#                           self.monasca_client.get_alarm_definition,
#                           alarm_def_id)
#
#         # Delete notification 1
#         resp, body = self.monasca_client.delete_notification_method(
#             notification_id1)
#         self.assertEqual(204, resp.status)
#
#         # Delete notification 2
#         resp, body = self.monasca_client.delete_notification_method(
#             notification_id2)
#         self.assertEqual(204, resp.status)
#
#     @test.attr(type="gate")
#     def test_create_alarm_definition_with_url_in_expression(self):
#         notification_name = data_utils.rand_name('notification-')
#         notification_type = 'EMAIL'
#         u_address = 'root@localhost'
#
#         resp, body = self.monasca_client.create_notification_method(
#             notification_name, type=notification_type, address=u_address)
#         self.assertEqual(201, resp.status)
#         self.assertEqual(notification_name, body['name'])
#         notification_id = body['id']
#
#         # Create an alarm definition
#         alarm_def_name = data_utils.rand_name('monitoring_alarm_definition')
#         alarm_definition = helpers.create_alarm_definition(
#             name=alarm_def_name,
#             expression="avg(mem_total_mb{url=https://www.google.com}) gt 0",
#             alarm_actions=notification_id,
#             ok_actions=notification_id,
#             severity="LOW")
#         resp, body = self.monasca_client.create_alarm_definitions(
#             alarm_definition)
#         self.assertEqual(201, resp.status)
#         self.assertEqual(alarm_def_name, body['name'])
#         alarm_def_id = body['id']
#         self.assertEqual("avg(mem_total_mb{url=https://www.google.com}) gt 0",
#                          body['expression'])
#
#         # Delete alarm and verify if deleted
#         resp, body = self.monasca_client.delete_alarm_definition(
#             alarm_def_id)
#         self.assertEqual(204, resp.status)
#         self.assertRaises(exceptions.NotFound,
#                           self.monasca_client.get_alarm_definition,
#                           alarm_def_id)
#
#         # Delete notification
#         resp, body = self.monasca_client.delete_notification_method(
#             notification_id)
#         self.assertEqual(204, resp.status)
#
#     @test.attr(type="gate")
#     @test.attr(type=['negative'])
#     def test_create_alarm_definition_with_special_chars_in_expression(self):
#         notification_name = data_utils.rand_name('notification-')
#         notification_type = 'EMAIL'
#         u_address = 'root@localhost'
#
#         resp, body = self.monasca_client.create_notification_method(
#             notification_name, type=notification_type, address=u_address)
#         self.assertEqual(201, resp.status)
#         self.assertEqual(notification_name, body['name'])
#         notification_id = body['id']
#
#         # Create an alarm definition
#         alarm_def_name = data_utils.rand_name('monitoring_alarm')
#         alarm_definition = helpers.create_alarm_definition(
#             name=alarm_def_name,
#             expression="avg(mem_total_mb{dev=\usr\local\bin}) "
#                        "gt 0",
#             alarm_actions=notification_id,
#             ok_actions=notification_id,
#             severity="LOW")
#         self.assertRaises(exceptions.UnprocessableEntity,
#                           self.monasca_client.create_alarm_definitions,
#                           alarm_definition)
#
#     # List
#
#     @test.attr(type="gate")
#     def test_list_alarm_definitions(self):
#         resp, response_body = self.monasca_client.list_alarm_definitions()
#         self.assertEqual(200, resp.status)
#
#         # Test list alarm definition response body
#         self.assertTrue(isinstance(response_body, dict))
#         self.assertTrue(set(['links', 'elements']) ==
#                         set(response_body))
#         elements = response_body['elements']
#         links = response_body['links']
#         self.assertTrue(isinstance(links, list))
#         link = links[0]
#         self.assertTrue(set(['rel', 'href']) ==
#                         set(link))
#         self.assertEqual(link['rel'], u'self')
#         self.assertEqual(len(elements), NUM_ALARM_DEFINITIONS)
#         for definition in elements:
#             self.assertTrue(set(['id',
#                                  'links',
#                                  'name',
#                                  'description',
#                                  'expression',
#                                  'match_by',
#                                  'severity',
#                                  'actions_enabled',
#                                  'ok_actions',
#                                  'alarm_actions',
#                                  'undetermined_actions']) ==
#                             set(definition))
#
#     @test.attr(type="gate")
#     def test_list_alarm_definitions_with_name(self):
#         query_parms = '?name=name-1'
#         resp, response_body = self.monasca_client.list_alarm_definitions(
#             query_parms)
#         self.assertEqual(200, resp.status)
#
#     @test.attr(type="gate")
#     def test_list_alarm_definitions_with_dimensions(self):
#         query_parms = '?dimensions=key1:value1'
#         resp, response_body = self.monasca_client.list_alarm_definitions(
#             query_parms)
#         self.assertEqual(200, resp.status)
#
#     @test.attr(type="gate")
#     def test_list_alarm_definitions_with_offset_limit(self):
#         query_parms = '?offset=1&limit=2'
#         resp, response_body = self.monasca_client.list_alarm_definitions(
#             query_parms)
#         self.assertEqual(200, resp.status)
#
#     # Get
#
#     @test.attr(type="gate")
#     def test_get_alarm_definition(self):
#         alarm_definition = self.create_alarm_definition()
#         resp, response_body = self.monasca_client.create_alarm_definitions(
#             alarm_definition)
#         alarm_def_id = response_body['id']
#         resp, response_body = self.monasca_client.get_alarm_definition(
#             alarm_def_id)
#         self.assertEqual(200, resp.status)
#
#         # Test Get Alarm Definition Response Body
#         self.assertTrue(isinstance(response_body, dict))
#         self.assertTrue(set(['id', 'links', 'name', 'description',
#                              'expression', 'match_by', 'severity',
#                              'actions_enabled', 'alarm_actions',
#                              'ok_actions', 'undetermined_actions'
#                              ]) ==
#                         set(response_body))
#         links = response_body['links']
#         self.assertTrue(isinstance(links, list))
#         link = links[0]
#         self.assertTrue(set(['rel', 'href']) ==
#                         set(link))
#         self.assertEqual(link['rel'], u'self')
#
#         # Delete alarm and verify if deleted
#         resp, response_body = self.monasca_client.delete_alarm_definition(
#             alarm_def_id)
#         self.assertEqual(204, resp.status)
#         self.assertRaises(exceptions.NotFound,
#                           self.monasca_client.get_alarm_definition,
#                           alarm_def_id)
#
#     # Update
#
#     @test.attr(type="gate")
#     def test_update_alarm_definition(self):
#         alarm_definition = self.create_alarm_definition()
#         resp, response_body = self.monasca_client.create_alarm_definitions(
#             alarm_definition)
#         id = response_body['id']
#
#         # Update alarm definition
#         updated_name = data_utils.rand_name('updated_name')
#         updated_description = 'updated description'
#         updated_expression = response_body['expression']
#         resp, response_body = self.monasca_client.update_alarm_definition(
#             id=id,
#             name=updated_name,
#             expression=updated_expression,
#             description=updated_description,
#             actions_enabled='true'
#         )
#         self.assertEqual(200, resp.status)
#
#         # Validate fields updated
#         self.assertEqual(updated_name, response_body['name'])
#         self.assertEqual(updated_expression, response_body['expression'])
#         self.assertEqual(updated_description, response_body['description'])
#
#         # Get and validate details of alarm definition after update
#         resp, response_body = self.monasca_client.get_alarm_definition(id)
#         self.assertEqual(200, resp.status)
#         self.assertEqual(updated_name, response_body['name'])
#         self.assertEqual(updated_description, response_body['description'])
#         self.assertEqual(updated_expression, response_body['expression'])
#
#         # Test Updated alarm definition Response Body
#         self.assertTrue(isinstance(response_body, dict))
#         self.assertTrue(set(['id', 'links', 'name', 'description',
#                              'expression', 'match_by', 'severity',
#                              'actions_enabled', 'alarm_actions',
#                              'ok_actions', 'undetermined_actions'
#                              ]) ==
#                         set(response_body))
#
#         links = response_body['links']
#         self.assertTrue(isinstance(links, list))
#         link = links[0]
#         self.assertTrue(set(['rel', 'href']) ==
#                         set(link))
#         self.assertEqual(link['rel'], u'self')
#
#         # Delete alarm definition
#         resp, response_body = self.monasca_client.delete_alarm_definition(
#             id)
#         self.assertEqual(204, resp.status)
#
#         # Validate alarm ID is not found
#         self.assertRaises(exceptions.NotFound,
#                           self.monasca_client.get_alarm_definition, id)
#
#     @test.attr(type="gate")
#     def test_update_notification_in_alarm_definition(self):
#         notification_name = data_utils.rand_name('notification-')
#         notification_type = 'EMAIL'
#         u_address = 'root@localhost'
#
#         resp, body = self.monasca_client.create_notification_method(
#             notification_name, type=notification_type, address=u_address)
#         self.assertEqual(201, resp.status)
#         self.assertEqual(notification_name, body['name'])
#         notification_id = body['id']
#
#         # Create an alarm definition
#         alarm_definition = self.create_alarm_definition()
#         resp, response_body = self.monasca_client.create_alarm_definitions(
#             alarm_definition)
#         self.assertEqual(201, resp.status)
#         alarm_def_id = response_body['id']
#         expression = response_body['expression']
#
#         # Update alarm definition
#         alarm_def_name = data_utils.rand_name('monitoring_alarm_update')
#         resp, body = self.monasca_client.update_alarm_definition(
#             alarm_def_id,
#             name=alarm_def_name,
#             expression=expression,
#             actions_enabled='true',
#             alarm_actions=notification_id,
#             ok_actions=notification_id
#         )
#         self.assertEqual(200, resp.status)
#         self.assertEqual(alarm_def_name, body['name'])
#         self.assertEqual(expression, body['expression'])
#         self.assertEqual(notification_id, body['alarm_actions'][0])
#         self.assertEqual(notification_id, body['ok_actions'][0])
#
#         # Get and verify details of an alarm after update
#         resp, body = self.monasca_client.get_alarm_definition(alarm_def_id)
#         self.assertEqual(200, resp.status)
#         self.assertEqual(alarm_def_name, body['name'])
#         self.assertEqual(expression, body['expression'])
#
#         # Delete alarm and verify if deleted
#         resp, _ = self.monasca_client.delete_alarm_definition(alarm_def_id)
#         self.assertEqual(204, resp.status)
#         self.assertRaises(exceptions.NotFound,
#                           self.monasca_client.get_alarm_definition,
#                           alarm_def_id)
#
#         # Delete notification
#         resp, body = self.monasca_client.delete_notification_method(
#             notification_id)
#         self.assertEqual(204, resp.status)
#
#     # Patch
#
#     @test.attr(type="gate")
#     def test_patch_alarm_definition(self):
#         alarm_definition = self.create_alarm_definition()
#         resp, response_body = self.monasca_client.create_alarm_definitions(
#             alarm_definition)
#         id = response_body['id']
#
#         # Patch alarm definition
#         patched_name = data_utils.rand_name('patched_name')
#         resp, response_body = self.monasca_client.patch_alarm_definition(
#             id=id,
#             name=patched_name
#         )
#         self.assertEqual(200, resp.status)
#
#         # Validate fields updated
#         self.assertEqual(patched_name, response_body['name'])
#
#         # Delete alarm definition
#         resp, response_body = self.monasca_client.delete_alarm_definition(
#             id)
#         self.assertEqual(204, resp.status)
#
#         # Validate alarm ID is not found
#         self.assertRaises(exceptions.NotFound,
#                           self.monasca_client.get_alarm_definition,
#                           id)
#
#     # Delete
#     @test.attr(type="gate")
#     def test_create_and_delete_alarm_definition(self):
#         alarm_definition = self.create_alarm_definition()
#         resp, response_body = self.monasca_client.create_alarm_definitions(
#             alarm_definition)
#         alarm_def_id = response_body['id']
#
#         # Delete alarm and verify if deleted
#         resp, response_body = self.monasca_client.delete_alarm_definition(
#             alarm_def_id)
#         self.assertEqual(204, resp.status)
#         self.assertRaises(exceptions.NotFound,
#                           self.monasca_client.get_alarm_definition,
#                           alarm_def_id)
