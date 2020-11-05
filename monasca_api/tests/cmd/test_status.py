# Copyright (c) 2018 NEC, Corp.
# Copyright (c) 2018 SUSE LLC
#
#    Licensed under the Apache License, Version 2.0 (the "License"); you may
#    not use this file except in compliance with the License. You may obtain
#    a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#    License for the specific language governing permissions and limitations
#    under the License.

import unittest

from oslo_upgradecheck.upgradecheck import Code

from monasca_api.cmd import status


class TestUpgradeChecks(unittest.TestCase):

    def setUp(self):
        """
        Sets the daemon.

        Args:
            self: (todo): write your description
        """
        super(TestUpgradeChecks, self).setUp()
        self.cmd = status.Checks()

    def test__check_placeholder(self):
        """
        Run placeholder.

        Args:
            self: (todo): write your description
        """
        check_result = self.cmd._check_placeholder()
        self.assertEqual(
            Code.SUCCESS, check_result.code,
            "Placeholder should always succeed.")
