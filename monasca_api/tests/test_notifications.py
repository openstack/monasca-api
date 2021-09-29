# Copyright 2019 FUJITSU LIMITED
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

import datetime
import json

import falcon.testing
import fixtures
from oslo_config import cfg

from monasca_api.tests import base
from monasca_api.v2.reference import notifications
from monasca_api.v2.reference import notificationstype


CONF = cfg.CONF

TENANT_ID = u"fedcba9876543210fedcba9876543210"


class TestNotifications(base.BaseApiTestCase):
    def setUp(self):
        super(TestNotifications, self).setUp()

        self.conf_override(
            notifications_driver='monasca_api.common.repositories.sqla.'
                                 'notifications_repository:NotificationsRepository',
            group='repositories')

        self.notifications_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.sqla.notifications_repository.NotificationsRepository'
        )).mock

        self.notifications_type_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.sqla.'
            'notification_method_type_repository.NotificationMethodTypeRepository'
        )).mock

        self.notification_resource = notifications.Notifications()
        self.app.add_route(
            '/v2.0/notification-methods', self.notification_resource)
        self.app.add_route(
            '/v2.0/notification-methods/{notification_method_id}', self.notification_resource)

    def test_create_notifications(self):
        request_body = \
            {
                "name": "Name",
                "type": "EMAIL",
                "address": "john@doe.com"
            }
        return_value = self.notifications_repo_mock.return_value
        return_value.create_notification.return_value = 'a9362cc5-c78e-4674-bd39-4639fc274a20'
        return_value.find_notification_by_name.return_value = {}

        return_value = self.notifications_type_repo_mock.return_value
        return_value.list_notification_method_types.return_value = \
            [u'EMAIL',
             u'PAGERDUTY',
             u'WEBHOOK']

        response = self.simulate_request(path='/v2.0/notification-methods',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         method='POST',
                                         body=json.dumps(request_body))
        self.assertEqual(falcon.HTTP_201, response.status)

    def test_create_notifications_with_incorrect_type(self):
        request_body = \
            {
                "name": "Name",
                "type": "MagicTYPE",
                "address": "john@doe.com"
            }
        return_value = self.notifications_repo_mock.return_value
        return_value.find_notification_by_name.return_value = {}

        return_value = self.notifications_type_repo_mock.return_value
        return_value.list_notification_method_types.return_value = \
            [u'EMAIL',
             u'PAGERDUTY',
             u'WEBHOOK']

        response = self.simulate_request(path='/v2.0/notification-methods',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         method='POST',
                                         body=json.dumps(request_body))
        self.assertEqual(falcon.HTTP_400, response.status)

    def test_create_notifications_when_name_is_taken(self):
        request_body = \
            {
                "name": "Name",
                "type": "EMAIL",
                "address": "john@doe.com"
            }
        return_value = self.notifications_repo_mock.return_value
        return_value.find_notification_by_name.return_value = \
            {'name': u'Name',
             'id': u'1',
             'tenant_id': u'444',
             'type': u'EMAIL',
             'period': 0,
             'address': u'a@b.com',
             'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
             'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}

        return_value = self.notifications_type_repo_mock.return_value
        return_value.list_notification_method_types.return_value = \
            [u'EMAIL',
             u'PAGERDUTY',
             u'WEBHOOK']

        response = self.simulate_request(path='/v2.0/notification-methods',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         method='POST',
                                         body=json.dumps(request_body))
        self.assertEqual(falcon.HTTP_409, response.status)

    def test_list_notifications(self):
        expected_elements = \
            {'elements': [
                {'name': u'notification',
                 'id': u'1',
                 'type': u'EMAIL',
                 'period': 0,
                 'address': u'a@b.com',
                 'links': [{
                     'href': 'http://falconframework.org/v2.0/notification-methods/1',
                     'rel': 'self'}]}]}

        return_value = self.notifications_repo_mock.return_value
        return_value.list_notifications.return_value = \
            [{'name': u'notification',
              'id': u'1',
              'tenant_id': u'4199b031d5fa401abf9afaf7e58890b7',
              'type': u'EMAIL',
              'period': 0,
              'address': u'a@b.com',
              'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
              'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}]
        response = self.simulate_request(path='/v2.0/notification-methods',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET')
        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_elements))

    def test_list_notifications_with_sort_by(self):
        expected_elements = \
            {'elements': [
                {'name': u'notification',
                 'id': u'1',
                 'type': u'EMAIL',
                 'period': 0,
                 'address': u'a@b.com',
                 'links': [{
                     'href': 'http://falconframework.org/v2.0/notification-methods/1',
                     'rel': 'self'}]}]}

        return_value = self.notifications_repo_mock.return_value
        return_value.list_notifications.return_value = \
            [{'name': u'notification',
              'id': u'1',
              'tenant_id': u'4199b031d5fa401abf9afaf7e58890b7',
              'type': u'EMAIL',
              'period': 0,
              'address': u'a@b.com',
              'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
              'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}]
        response = self.simulate_request(path='/v2.0/notification-methods',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         query_string='sort_by=name',
                                         method='GET')
        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_elements))

    def test_list_notifications_with_incorrect_sort_by(self):
        response = self.simulate_request(path='/v2.0/notification-methods',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         query_string='sort_by=random_string',
                                         method='GET')
        self.assertEqual(falcon.HTTP_422, response.status)

    def test_list_notifications_with_offset(self):
        expected_elements = \
            {'elements': [
                {'name': u'notification',
                 'id': u'1',
                 'type': u'EMAIL',
                 'period': 0,
                 'address': u'a@b.com',
                 'links': [{
                     'href': 'http://falconframework.org/v2.0/notification-methods/1',
                     'rel': 'self'}]}]}

        return_value = self.notifications_repo_mock.return_value
        return_value.list_notifications.return_value = \
            [{'name': u'notification',
              'id': u'1',
              'tenant_id': u'4199b031d5fa401abf9afaf7e58890b7',
              'type': u'EMAIL',
              'period': 0,
              'address': u'a@b.com',
              'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
              'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}]
        response = self.simulate_request(path='/v2.0/notification-methods',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         query_string='offset=10',
                                         method='GET')
        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_elements))

    def test_list_notifications_with_incorrect_offset(self):
        return_value = self.notifications_repo_mock.return_value
        return_value.list_notifications.return_value = \
            [{'name': u'notification',
              'id': u'1',
              'tenant_id': u'4199b031d5fa401abf9afaf7e58890b7',
              'type': u'EMAIL',
              'period': 0,
              'address': u'a@b.com',
              'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
              'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}]
        result = self.simulate_request(path='/v2.0/notification-methods',
                                       headers={'X-Roles':
                                                CONF.security.default_authorized_roles[0],
                                                'X-Tenant-Id': TENANT_ID},
                                       query_string='offset=ten',
                                       method='GET')
        self.assertEqual(falcon.HTTP_422, result.status)

    def test_get_notification_with_id(self):
        expected_elements = \
            {'name': u'notification',
             'id': u'1',
             'type': u'EMAIL',
             'period': 0,
             'address': u'a@b.com'}

        return_value = self.notifications_repo_mock.return_value
        return_value.list_notification.return_value = \
            {'name': u'notification',
             'id': u'1',
             'tenant_id': u'4199b031d5fa401abf9afaf7e58890b7',
             'type': u'EMAIL',
             'period': 0,
             'address': u'a@b.com',
             'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
             'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}
        response = self.simulate_request(path='/v2.0/notification-methods/1',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='GET')
        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_elements))

    def test_delete_notification(self):
        response = self.simulate_request(path='/v2.0/notification-methods/1',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID},
                                         method='DELETE')
        self.assertEqual(falcon.HTTP_204, response.status)

    def test_put_notification(self):
        expected_elements = \
            {"id": "1",
             "name": "shy_name",
             "type": "EMAIL",
             "address": "james@bond.com",
             "period": 0}

        return_value = self.notifications_type_repo_mock.return_value
        return_value.list_notification_method_types.return_value = \
            [u'EMAIL',
             u'PAGERDUTY',
             u'WEBHOOK']

        return_value = self.notifications_repo_mock.return_value
        return_value.find_notification_by_name.return_value = \
            {'name': u'notification',
             'id': u'1',
             'tenant_id': u'444',
             'type': u'EMAIL',
             'period': 0,
             'address': u'a@b.com',
             'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
             'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}

        request_body = \
            {"name": "shy_name",
             "type": "EMAIL",
             "address": "james@bond.com",
             "period": 0}

        response = self.simulate_request(path='/v2.0/notification-methods/1',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         method='PUT',
                                         body=json.dumps(request_body))

        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_elements))

    def test_patch_notification_all_fields(self):
        expected_elements = \
            {"id": "1",
             "name": "shy_name",
             "type": "EMAIL",
             "address": "james@bond.com",
             "period": 0}

        return_value = self.notifications_type_repo_mock.return_value
        return_value.list_notification_method_types.return_value = \
            [u'EMAIL',
             u'PAGERDUTY',
             u'WEBHOOK']

        return_value = self.notifications_repo_mock.return_value
        return_value.find_notification_by_name.return_value = \
            {'name': u'notification',
             'id': u'1',
             'tenant_id': u'444',
             'type': u'EMAIL',
             'period': 0,
             'address': u'a@b.com',
             'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
             'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}

        request_body = \
            {"name": "shy_name",
             "type": "EMAIL",
             "address": "james@bond.com",
             "period": 0}

        response = self.simulate_request(path='/v2.0/notification-methods/1',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         method='PATCH',
                                         body=json.dumps(request_body))

        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_elements))

    def test_patch_notification_name_fields(self):
        expected_elements = \
            {"id": "1",
             "name": "shy_name",
             "type": "EMAIL",
             "address": "james@bond.com",
             "period": 0}

        return_value = self.notifications_type_repo_mock.return_value
        return_value.list_notification_method_types.return_value = \
            [u'EMAIL',
             u'PAGERDUTY',
             u'WEBHOOK']

        return_value = self.notifications_repo_mock.return_value
        return_value.find_notification_by_name.return_value = \
            {'name': u'notification',
             'id': u'1',
             'tenant_id': u'444',
             'type': u'EMAIL',
             'period': 0,
             'address': u'james@bond.com',
             'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
             'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}

        return_value = self.notifications_repo_mock.return_value
        return_value.list_notification.return_value = \
            {'name': u'notification',
             'id': u'1',
             'tenant_id': u'444',
             'type': u'EMAIL',
             'period': 0,
             'address': u'james@bond.com'}

        request_body = {"name": "shy_name"}

        response = self.simulate_request(path='/v2.0/notification-methods/1',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         method='PATCH',
                                         body=json.dumps(request_body))

        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_elements))

    def test_patch_notification_type_fields(self):
        expected_elements = \
            {"id": "1",
             "name": "notification",
             "type": "PAGERDUTY",
             "address": "james@bond.com",
             "period": 0}

        return_value = self.notifications_type_repo_mock.return_value
        return_value.list_notification_method_types.return_value = \
            [u'EMAIL',
             u'PAGERDUTY',
             u'WEBHOOK']

        return_value = self.notifications_repo_mock.return_value
        return_value.find_notification_by_name.return_value = \
            {'name': u'notification',
             'id': u'1',
             'tenant_id': u'444',
             'type': u'EMAIL',
             'period': 0,
             'address': u'james@bond.com',
             'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
             'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}

        return_value = self.notifications_repo_mock.return_value
        return_value.list_notification.return_value = \
            {'name': u'notification',
             'id': u'1',
             'tenant_id': u'444',
             'type': u'EMAIL',
             'period': 0,
             'address': u'james@bond.com'}

        request_body = {"type": "PAGERDUTY"}

        response = self.simulate_request(path='/v2.0/notification-methods/1',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         method='PATCH',
                                         body=json.dumps(request_body))

        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_elements))

    def test_patch_notification_address_fields(self):
        expected_elements = \
            {"id": "1",
             "name": "notification",
             "type": "EMAIL",
             "address": "a@b.com",
             "period": 0}

        return_value = self.notifications_type_repo_mock.return_value
        return_value.list_notification_method_types.return_value = \
            [u'EMAIL',
             u'PAGERDUTY',
             u'WEBHOOK']

        return_value = self.notifications_repo_mock.return_value
        return_value.find_notification_by_name.return_value = \
            {'name': u'notification',
             'id': u'1',
             'tenant_id': u'444',
             'type': u'EMAIL',
             'period': 0,
             'address': u'james@bond.com',
             'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
             'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}

        return_value = self.notifications_repo_mock.return_value
        return_value.list_notification.return_value = \
            {'name': u'notification',
             'id': u'1',
             'tenant_id': u'444',
             'type': u'EMAIL',
             'period': 0,
             'address': u'james@bond.com'}

        request_body = {"address": "a@b.com"}

        response = self.simulate_request(path='/v2.0/notification-methods/1',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         method='PATCH',
                                         body=json.dumps(request_body))

        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_elements))

    def test_patch_notification_period_fields(self):
        expected_elements = \
            {"id": "1",
             "name": "notification",
             "type": "WEBHOOK",
             "address": "http://jamesbond.com",
             "period": 60}

        return_value = self.notifications_type_repo_mock.return_value
        return_value.list_notification_method_types.return_value = \
            [u'EMAIL',
             u'PAGERDUTY',
             u'WEBHOOK']

        return_value = self.notifications_repo_mock.return_value
        return_value.find_notification_by_name.return_value = \
            {'name': u'notification',
             'id': u'1',
             'tenant_id': u'444',
             'type': u'WEBHOOK',
             'period': 0,
             'address': u'http://jamesbond.com',
             'created_at': datetime.datetime(2019, 3, 22, 9, 35, 25),
             'updated_at': datetime.datetime(2019, 3, 22, 9, 35, 25)}

        return_value = self.notifications_repo_mock.return_value
        return_value.list_notification.return_value = \
            {'name': u'notification',
             'id': u'1',
             'tenant_id': u'444',
             'type': u'WEBHOOK',
             'period': 0,
             'address': u'http://jamesbond.com'}

        request_body = {"period": 60}

        response = self.simulate_request(path='/v2.0/notification-methods/1',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         method='PATCH',
                                         body=json.dumps(request_body))

        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_elements))


class TestNotificationsType(base.BaseApiTestCase):
    def setUp(self):
        super(TestNotificationsType, self).setUp()

        self.conf_override(
            notifications_driver='monasca_api.common.repositories.sqla.'
                                 'notifications_repository:NotificationsRepository',
            group='repositories')

        self.notifications_type_repo_mock = self.useFixture(fixtures.MockPatch(
            'monasca_api.common.repositories.sqla.'
            'notification_method_type_repository.NotificationMethodTypeRepository'
        )).mock

        self.notification_resource = notificationstype.NotificationsType()
        self.app.add_route(
            '/v2.0/notification-methods/types', self.notification_resource)

    def test_get_notification_types(self):
        expected_notification_types = \
            {'elements': [
                {'type': 'EMAIL'},
                {'type': 'PAGERDUTY'},
                {'type': 'WEBHOOK'}]}
        return_value = self.notifications_type_repo_mock.return_value
        return_value.list_notification_method_types.return_value = \
            [u'EMAIL',
             u'PAGERDUTY',
             u'WEBHOOK']

        response = self.simulate_request(path='/v2.0/notification-methods/types',
                                         headers={'X-Roles':
                                                  CONF.security.default_authorized_roles[0],
                                                  'X-Tenant-Id': TENANT_ID,
                                                  'Content-Type': 'application/json'},
                                         method='GET')
        self.assertEqual(falcon.HTTP_200, response.status)
        self.assertThat(response, base.RESTResponseEquals(expected_notification_types))
