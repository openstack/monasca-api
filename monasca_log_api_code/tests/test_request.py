# Copyright 2016-2017 FUJITSU LIMITED
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

from falcon import testing
from mock import mock

from monasca_log_api.app.base import request
from monasca_log_api.app.base import validation
from monasca_log_api.tests import base


class TestRequest(base.BaseTestCase):

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

    def test_validate_context_type(self):
        with mock.patch.object(validation,
                               'validate_content_type') as vc_type, \
                mock.patch.object(validation,
                                  'validate_payload_size') as vp_size, \
                mock.patch.object(validation,
                                  'validate_cross_tenant') as vc_tenant:
            req = request.Request(testing.create_environ())
            vc_type.side_effect = Exception()

            try:
                req.validate(['test'])
            except Exception as ex:
                self.assertEqual(1, vc_type.call_count)
                self.assertEqual(0, vp_size.call_count)
                self.assertEqual(0, vc_tenant.call_count)

                self.assertIsInstance(ex, Exception)

    def test_validate_payload_size(self):
        with mock.patch.object(validation,
                               'validate_content_type') as vc_type, \
                mock.patch.object(validation,
                                  'validate_payload_size') as vp_size, \
                mock.patch.object(validation,
                                  'validate_cross_tenant') as vc_tenant:

            req = request.Request(testing.create_environ())
            vp_size.side_effect = Exception()

            try:
                req.validate(['test'])
            except Exception as ex:
                self.assertEqual(1, vc_type.call_count)
                self.assertEqual(1, vp_size.call_count)
                self.assertEqual(0, vc_tenant.call_count)

                self.assertIsInstance(ex, Exception)

    def test_validate_cross_tenant(self):
        with mock.patch.object(validation,
                               'validate_content_type') as vc_type, \
                mock.patch.object(validation,
                                  'validate_payload_size') as vp_size, \
                mock.patch.object(validation,
                                  'validate_cross_tenant') as vc_tenant:

            req = request.Request(testing.create_environ())
            vc_tenant.side_effect = Exception()

            try:
                req.validate(['test'])
            except Exception as ex:
                self.assertEqual(1, vc_type.call_count)
                self.assertEqual(1, vp_size.call_count)
                self.assertEqual(1, vc_tenant.call_count)

                self.assertIsInstance(ex, Exception)
