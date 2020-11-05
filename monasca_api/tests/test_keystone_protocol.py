# Copyright 2017 FUJITSU LIMITED
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

from unittest import mock

from monasca_api.healthcheck import keystone_protocol
from monasca_api.tests import base

_CONF = {}


class TestKeystoneProtocol(base.BaseTestCase):

    def test_should_return_none_if_healthcheck(self):
        """
        Checks if the health request was sent.

        Args:
            self: (todo): write your description
        """
        mocked_api = mock.Mock()
        instance = keystone_protocol.SkippingAuthProtocol(mocked_api, _CONF)
        request = mock.Mock()
        request.path = '/healthcheck'

        ret_val = instance.process_request(request)

        self.assertIsNone(ret_val)

    @mock.patch('keystonemiddleware.auth_token.AuthProtocol.process_request')
    def test_should_enter_keystone_auth_if_not_healthcheck(self, proc_request):
        """
        Checks if the request should be sent.

        Args:
            self: (todo): write your description
            proc_request: (todo): write your description
        """
        mocked_api = mock.Mock()
        instance = keystone_protocol.SkippingAuthProtocol(mocked_api, _CONF)
        request = mock.Mock()
        request.path = '/v2.0/logs/single'

        instance.process_request(request)

        self.assertTrue(proc_request.called)
