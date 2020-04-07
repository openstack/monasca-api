# Copyright 2018 OP5 AB
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

from falcon import errors
from falcon import testing

from oslo_policy import policy as os_policy

from monasca_api.api.core import request
from monasca_api.common.policy import policy_engine as policy
from monasca_api.tests import base
import monasca_api.v2.reference.helpers as helpers

from monasca_api.common.rest import utils as rest_utils


class TestHelpersFunction(base.BaseTestCase):

    def test_from_json(self):
        body_json = {'test_body': 'test'}
        req = request.Request(
            testing.create_environ(
                body=rest_utils.as_json(body_json),
            )
        )
        response = helpers.from_json(req)
        self.assertEqual(body_json, response)

    def test_from_json_incorrect_message(self):
        req = request.Request(
            testing.create_environ(
                body='incorrect message',
            )
        )
        self.assertRaises(errors.HTTPBadRequest, helpers.from_json, req)

    def test_to_json(self):
        test_dict = {'test_body': 'test'}
        expected_json = '{"test_body": "test"}'
        response = helpers.to_json(test_dict)
        self.assertEqual(expected_json, response)

    def test_validate_json_content_type(self):
        req = request.Request(
            testing.create_environ(
                headers={'Content-Type': 'application/json'}
            )
        )
        helpers.validate_json_content_type(req)

    def test_validate_json_content_type_incorrect_content_type(self):
        req = request.Request(
            testing.create_environ(
                headers={'Content-Type': 'multipart/form-data'}
            )
        )
        self.assertRaises(errors.HTTPBadRequest, helpers.validate_json_content_type, req)

    def test_validate_json_content_type_missing_content_type(self):
        req = request.Request(testing.create_environ())
        self.assertRaises(errors.HTTPBadRequest, helpers.validate_json_content_type, req)


class TestGetXTenantOrTenantId(base.BaseApiTestCase):
    def setUp(self):
        super(TestGetXTenantOrTenantId, self).setUp()
        rules = [
            os_policy.RuleDefault("example:allowed", "@"),
            os_policy.RuleDefault("example:denied", "!"),
            os_policy.RuleDefault("example:authorized",
                                  "role:role_1 or role:role_2")
        ]
        policy.reset()
        policy.init()
        policy._ENFORCER.register_defaults(rules)

    def test_return_tenant_id_on_authorized_roles(self):

        for role in ['role_1', 'role_2']:
            req_context = self._get_request_context(role)
            self.assertEqual(
                'fake_tenant_id',
                helpers.get_x_tenant_or_tenant_id(
                    req_context, ['example:authorized']
                )
            )

    def test_return_tenant_id_on_allowed_rules(self):
        req_context = self._get_request_context()
        self.assertEqual(
            'fake_tenant_id',
            helpers.get_x_tenant_or_tenant_id(
                req_context,
                ['example:allowed']
            )
        )

    def test_return_project_id_on_unauthorized_role(self):
        req_context = self._get_request_context()
        self.assertEqual('fake_project_id',
                         helpers.get_x_tenant_or_tenant_id(
                             req_context,
                             ['example:authorized']))

    def test_return_project_id_on_denied_rules(self):
        req_context = self._get_request_context()
        self.assertEqual(
            'fake_project_id',
            helpers.get_x_tenant_or_tenant_id(
                req_context,
                ['example:denied']
            )
        )

    def test_return_project_id_on_unavailable_tenant_id(self):
        req_context = self._get_request_context()
        req_context.query_string = ''
        self.assertEqual(
            'fake_project_id',
            helpers.get_x_tenant_or_tenant_id(
                req_context,
                ['example:allowed']
            )
        )

    @staticmethod
    def _get_request_context(role='fake_role'):
        return request.Request(
            testing.create_environ(
                path="/",
                query_string="tenant_id=fake_tenant_id",
                headers={
                    "X_PROJECT_ID": "fake_project_id",
                    "X_ROLES": role
                }
            )
        )
