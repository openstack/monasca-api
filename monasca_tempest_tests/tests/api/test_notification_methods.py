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

# TODO(RMH): Validate whether a 200 or 201 should be returned and resolve.
# TODO(RMH): Documentation says 200, but a 201 is being returned.

import time

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import constants
from monasca_tempest_tests.tests.api import helpers
from tempest.common.utils import data_utils
from tempest import test
from tempest_lib import exceptions

DEFAULT_EMAIL_ADDRESS = 'john.doe@domain.com'


class TestNotificationMethods(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestNotificationMethods, cls).resource_setup()

    @classmethod
    def resource_cleanup(cls):
        super(TestNotificationMethods, cls).resource_cleanup()

    @test.attr(type="gate")
    def test_create_notification_method(self):
        notification = helpers.create_notification()
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        self.assertEqual(201, resp.status)
        id = response_body['id']

        resp, response_body = self.monasca_client.\
            delete_notification_method(id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_notification_method_with_no_name(self):
        notification = helpers.create_notification(name=None)
        self.assertRaises((exceptions.BadRequest,exceptions.UnprocessableEntity),
                          self.monasca_client.create_notifications,
                          notification)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_notification_method_with_no_type(self):
        notification = helpers.create_notification(type=None)
        self.assertRaises((exceptions.BadRequest,exceptions.UnprocessableEntity),
                          self.monasca_client.create_notifications,
                          notification)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_notification_method_with_no_address(self):
        notification = helpers.create_notification(address=None)
        self.assertRaises((exceptions.BadRequest,exceptions.UnprocessableEntity),
                          self.monasca_client.create_notifications,
                          notification)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_notification_method_with_name_exceeds_max_length(self):
        long_name = "x" * (constants.MAX_NOTIFICATION_METHOD_NAME_LENGTH + 1)
        notification = helpers.create_notification(name=long_name)
        self.assertRaises((exceptions.BadRequest, exceptions.UnprocessableEntity),
                          self.monasca_client.create_notifications,
                          notification)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_notification_method_with_address_exceeds_max_length(self):
        long_address = "x" * (
            constants.MAX_NOTIFICATION_METHOD_ADDRESS_LENGTH + 1)
        notification = helpers.create_notification(address=long_address)
        self.assertRaises((exceptions.BadRequest, exceptions.UnprocessableEntity),
                          self.monasca_client.create_notifications,
                          notification)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_notification_method_with_invalid_type(self):
        notification = helpers.create_notification(type='random')
        self.assertRaises((exceptions.BadRequest, exceptions.UnprocessableEntity),
                          self.monasca_client.create_notifications,
                          notification)

    @test.attr(type="gate")
    def test_list_notification_methods(self):
        notification = helpers.create_notification()
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        self.assertEqual(201, resp.status)
        id = response_body['id']
        resp, response_body = self.monasca_client.list_notification_methods()
        self.assertEqual(200, resp.status)
        # Test response body
        self.assertTrue(set(['links', 'elements']) == set(response_body))
        elements = response_body['elements']
        element = elements[0]
        self.assertTrue(set(['id', 'links', 'name', 'type', 'address']) ==
                        set(element))
        self.assertTrue(type(element['id']) is unicode)
        self.assertTrue(type(element['links']) is list)
        self.assertTrue(type(element['name']) is unicode)
        self.assertTrue(type(element['type']) is unicode)
        self.assertTrue(type(element['address']) is unicode)

        resp, response_body = self.monasca_client.\
            delete_notification_method(id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    def test_list_notification_methods_with_offset_limit(self):
        name1 = data_utils.rand_name('notification')
        name2 = data_utils.rand_name('notification')
        name3 = data_utils.rand_name('notification')
        name4 = data_utils.rand_name('notification')
        notification1 = helpers.create_notification(name=name1)
        notification2 = helpers.create_notification(name=name2)
        notification3 = helpers.create_notification(name=name3)
        notification4 = helpers.create_notification(name=name4)

        resp, response_body = self.monasca_client.create_notifications(
            notification1)
        id1 = response_body['id']
        self.assertEqual(201, resp.status)
        resp, response_body = self.monasca_client.create_notifications(
            notification2)
        id2 = response_body['id']
        self.assertEqual(201, resp.status)
        resp, response_body = self.monasca_client.create_notifications(
            notification3)
        id3 = response_body['id']
        self.assertEqual(201, resp.status)
        resp, response_body = self.monasca_client.create_notifications(
            notification4)
        id4 = response_body['id']
        self.assertEqual(201, resp.status)

        resp, response_body = self.monasca_client.list_notification_methods()
        elements = response_body['elements']

        first_element = elements[0]
        last_element = elements[3]

        query_parms = '?limit=4'
        resp, response_body = self.monasca_client.\
            list_notification_methods(query_parms)
        self.assertEqual(200, resp.status)
        self.assertEqual(4, len(elements))
        self.assertEqual(first_element, elements[0])

        timeout = time.time() + 60 * 1   # 1 minute timeout
        for limit in xrange(1, 5):
            next_element = elements[limit - 1]
            while True:
                if time.time() < timeout:
                    query_parms = '?offset=' + str(next_element['id']) + \
                                  '&limit=' + str(limit)
                    resp, response_body = self.monasca_client.\
                        list_notification_methods(query_parms)
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
                          "test_list_notification_methods_with_offset_limit:" \
                          " one minute timeout on offset limit test loop."
                    raise exceptions.TimeoutException(msg)

        resp, response_body = self.monasca_client.\
            delete_notification_method(id1)
        self.assertEqual(204, resp.status)

        resp, response_body = self.monasca_client.\
            delete_notification_method(id2)
        self.assertEqual(204, resp.status)

        resp, response_body = self.monasca_client.\
            delete_notification_method(id3)
        self.assertEqual(204, resp.status)
        resp, response_body = self.monasca_client.\
            delete_notification_method(id4)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    def test_get_notification_method(self):
        notification = helpers.create_notification()
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        self.assertEqual(201, resp.status)
        id = response_body['id']
        resp, response_body = self.monasca_client.get_notification_method(id)
        self.assertEqual(200, resp.status)
        resp, response_body = self.monasca_client.\
            delete_notification_method(id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_get_notification_method_with_invalid_id(self):
        notification = helpers.create_notification()
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        self.assertEqual(201, resp.status)
        id = data_utils.rand_name()
        self.assertRaises(exceptions.NotFound,
                          self.monasca_client.get_notification_method,
                          id)
        resp, response_body = self.monasca_client.\
            delete_notification_method(response_body['id'])
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    def test_update_notification_method_name(self):
        name = data_utils.rand_name('notification-')
        notification = helpers.create_notification(name=name)
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        self.assertEqual(201, resp.status)
        self.assertEqual(name, response_body['name'])
        id = response_body['id']
        new_name = name + 'update'
        resp, response_body = self.monasca_client.\
            update_notification_method(id, new_name,
                                       type=response_body['type'],
                                       address=response_body['address'])
        self.assertEqual(200, resp.status)
        self.assertEqual(new_name, response_body['name'])
        resp, response_body = self.monasca_client.\
            delete_notification_method(id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    def test_update_notification_method_type(self):
        type = 'EMAIL'
        notification = helpers.create_notification(type=type)
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        self.assertEqual(201, resp.status)
        self.assertEqual(type, response_body['type'])
        id = response_body['id']
        new_type = 'PAGERDUTY'
        resp, response_body = \
            self.monasca_client.\
            update_notification_method(id, name=response_body['name'],
                                       type=new_type,
                                       address=response_body['address'])
        self.assertEqual(200, resp.status)
        self.assertEqual(new_type, response_body['type'])
        resp, response_body = self.monasca_client.\
            delete_notification_method(id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    def test_update_notification_method_address(self):
        address = DEFAULT_EMAIL_ADDRESS
        notification = helpers.create_notification(address=address)
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        self.assertEqual(201, resp.status)
        self.assertEqual(address, response_body['address'])
        id = response_body['id']
        new_address = 'jane.doe@domain.com'
        resp, response_body = self.monasca_client.\
            update_notification_method(id,
                                       name=response_body['name'],
                                       type=response_body['type'],
                                       address=new_address)
        self.assertEqual(200, resp.status)
        self.assertEqual(new_address, response_body['address'])
        resp, response_body = \
            self.monasca_client.delete_notification_method(id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_update_notification_method_name_exceeds_max_length(self):
        name = data_utils.rand_name('notification-')
        notification = helpers.create_notification(name=name)
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        id = response_body['id']
        self.assertEqual(201, resp.status)
        new_name_long = "x" * (constants.MAX_NOTIFICATION_METHOD_NAME_LENGTH
                               + 1)
        self.assertRaises((exceptions.BadRequest, exceptions.UnprocessableEntity),
                          self.monasca_client.update_notification_method, id,
                          name=new_name_long, type=response_body['type'],
                          address=response_body['address'])
        resp, response_body = \
            self.monasca_client.delete_notification_method(id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_update_notification_method_invalid_type(self):
        name = data_utils.rand_name('notification-')
        notification = helpers.create_notification(name=name)
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        id = response_body['id']
        self.assertEqual(201, resp.status)
        self.assertRaises((exceptions.BadRequest, exceptions.UnprocessableEntity),
                          self.monasca_client.update_notification_method, id,
                          name=response_body['name'], type='random',
                          address=response_body['address'])
        resp, response_body = \
            self.monasca_client.delete_notification_method(id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_update_notification_method_address_exceeds_max_length(self):
        name = data_utils.rand_name('notification-')
        notification = helpers.create_notification(name=name)
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        id = response_body['id']
        self.assertEqual(201, resp.status)
        new_address_long = "x" * (
            constants.MAX_NOTIFICATION_METHOD_ADDRESS_LENGTH + 1)
        self.assertRaises((exceptions.BadRequest, exceptions.UnprocessableEntity),
                          self.monasca_client.update_notification_method, id,
                          name=response_body['name'], type=new_address_long,
                          address=response_body['address'])
        resp, response_body = \
            self.monasca_client.delete_notification_method(id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    def test_create_and_delete_notification_method(self):
        notification = helpers.create_notification()
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        self.assertEqual(201, resp.status)
        id = response_body['id']
        resp, response_body = self.monasca_client.\
            delete_notification_method(id)
        self.assertEqual(204, resp.status)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_delete_notification_method_with_invalid_id(self):
        name = data_utils.rand_name('notification-')
        notification = helpers.create_notification(name=name)
        resp, response_body = self.monasca_client.create_notifications(
            notification)
        self.assertEqual(201, resp.status)
        id = data_utils.rand_name()
        self.assertRaises(exceptions.NotFound,
                          self.monasca_client.delete_notification_method,
                          id)
        resp, response_body = self.monasca_client.\
            delete_notification_method(response_body['id'])
        self.assertEqual(204, resp.status)
