# Copyright 2016 FUJITSU LIMITED
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

from mock import mock
from oslo_config import fixture as oo_cfg
from oslo_context import fixture as oo_ctx

from falcon import testing

from monasca_api.api.core import request
from monasca_api.v2.common import exceptions


class TestRequest(testing.TestBase):

    def test_use_context_from_request(self):
        req = request.Request(
            testing.create_environ(
                path='/',
                headers={
                    'X_AUTH_TOKEN': '111',
                    'X_USER_ID': '222',
                    'X_PROJECT_ID': '333',
                    'X_ROLES': 'terminator,predator'
                }
            )
        )

        self.assertEqual('111', req.context.auth_token)
        self.assertEqual('222', req.user_id)
        self.assertEqual('333', req.project_id)
        self.assertEqual(['terminator', 'predator'], req.roles)


class TestRequestLimit(testing.TestBase):
    def setUp(self):
        super(TestRequestLimit, self).setUp()
        self.useFixture(oo_cfg.Config())
        self.useFixture(oo_ctx.ClearRequestContext())

    def test_valid_limit(self):
        expected_limit = 10
        req = request.Request(
            testing.create_environ(
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
            testing.create_environ(
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

    @mock.patch('monasca_api.common.repositories.constants.PAGE_LIMIT')
    def test_default_limit(self, page_limit):
        req = request.Request(
            testing.create_environ(
                path='/',
                headers={
                    'X_AUTH_TOKEN': '111',
                    'X_USER_ID': '222',
                    'X_PROJECT_ID': '333',
                    'X_ROLES': 'terminator,predator'
                }
            )
        )
        self.assertEqual(page_limit, req.limit)
