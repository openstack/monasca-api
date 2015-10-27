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

import datetime

from oslo_serialization import jsonutils as json

from monasca_tempest_tests.tests.api import base
from tempest import test


class TestPlaceholder(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestPlaceholder, cls).resource_setup()

    @test.attr(type='gate')
    def test_placeholder(self):
        # Placeholder Test
        self.assertTrue(1 == 1)
        return
