# Copyright 2016-2017 FUJITSU LIMITED
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
from oslo_policy import policy as os_policy

from monasca_api.api.core import request
from monasca_api.common.policy import policy_engine as policy
import monasca_api.common.repositories.constants as const
from monasca_api.tests import base
from monasca_api.v2.common import exceptions


class TestRequest(base.BaseApiTestCase):
    def setUp(self):
        super(TestRequest, self).setUp()
        rules = [
            os_policy.RuleDefault("example:allowed", "user_id:222"),
        ]
        policy.reset()
        policy.init()
        policy._ENFORCER.register_defaults(rules)

    def test_use_context_from_request(self):
        req = request.Request(
            self.create_environ(
                path='/',
                headers={
                    'X_AUTH_TOKEN': '111',
                    'X_USER_ID': '222',
                    'X_PROJECT_ID': '333',
                    'X_ROLES': 'terminator,predator',
                },
                query_string='project_id=444'

            )
        )

        self.assertEqual('111', req.context.auth_token)
        self.assertEqual('222', req.user_id)
        self.assertEqual('333', req.project_id)
        self.assertEqual(['terminator', 'predator'], req.roles)
        self.assertEqual('444', req.cross_project_id)

    def test_policy_validation_with_target(self):
        req = request.Request(
            self.create_environ(
                path='/',
                headers={
                    'X_AUTH_TOKEN': '111',
                    'X_USER_ID': '222',
                    'X_PROJECT_ID': '333',
                }
            )
        )
        target = {'project_id': req.project_id,
                  'user_id': req.user_id}
        self.assertEqual(True, req.can('example:allowed', target))

    def test_policy_validation_without_target(self):
        req = request.Request(
            self.create_environ(
                path='/',
                headers={
                    'X_AUTH_TOKEN': '111',
                    'X_USER_ID': '222',
                    'X_PROJECT_ID': '333',
                }
            )
        )
        self.assertEqual(True, req.can('example:allowed'))


class TestRequestLimit(base.BaseApiTestCase):

    def test_valid_limit(self):
        expected_limit = 10
        req = request.Request(
            self.create_environ(
                path='/',
                query_string='limit=%d' % expected_limit,
                headers={
                    'X_AUTH_TOKEN': '111',
                    'X_USER_ID': '222',
                    'X_PROJECT_ID': '333',
                    'X_ROLES': 'terminator,predator'
                }
            )
        )
        self.assertEqual(expected_limit, req.limit)

    def test_invalid_limit(self):
        req = request.Request(
            self.create_environ(
                path='/',
                query_string='limit=abc',
                headers={
                    'X_AUTH_TOKEN': '111',
                    'X_USER_ID': '222',
                    'X_PROJECT_ID': '333',
                    'X_ROLES': 'terminator,predator'
                }
            )
        )

        # note(trebskit) assertRaises fails to call property
        # so we need the actual function
        def property_wrapper():
            return req.limit

        self.assertRaises(
            exceptions.HTTPUnprocessableEntityError,
            property_wrapper
        )

    def test_default_limit(self):
        req = request.Request(
            self.create_environ(
                path='/',
                headers={
                    'X_AUTH_TOKEN': '111',
                    'X_USER_ID': '222',
                    'X_PROJECT_ID': '333',
                    'X_ROLES': 'terminator,predator'
                }
            )
        )
        self.assertEqual(const.PAGE_LIMIT, req.limit)

    def test_to_big_limit(self):
        req = request.Request(
            self.create_environ(
                path='/',
                headers={
                    'X_AUTH_TOKEN': '111',
                    'X_USER_ID': '222',
                    'X_PROJECT_ID': '333',
                    'X_ROLES': 'terminator,predator'
                },
                query_string='limit={}'.format(const.PAGE_LIMIT + 1),
            )
        )
        self.assertEqual(const.PAGE_LIMIT, req.limit)
