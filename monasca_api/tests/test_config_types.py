# (C) Copyright 2016-2017 Hewlett Packard Enterprise Development LP
# Copyright 2017 FUJITSU LIMITED
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.
# See the License for the specific language governing permissions and
# limitations under the License.


from monasca_api.conf import types
from monasca_api.tests import base


class TestHostAddressPortType(base.BaseTestCase):
    def setUp(self):
        super(TestHostAddressPortType, self).setUp()
        self.types = types.HostAddressPortType()

    def test_ip_address(self):
        self.assertEqual('127.0.0.1:2121', self.types('127.0.0.1:2121'))

    def test_hostname(self):
        self.assertEqual('localhost:2121', self.types('localhost:2121'))

    def test_ipv6_address(self):
        self.assertEqual('2001:db8:85a3::8a2e:370:2121',
                         self.types('[2001:db8:85a3::8a2e:370]:2121'))

    def test_ipv6_hostname(self):
        self.assertEqual('::1:2121', self.types('[::1]:2121'))

    # failure scenario
    def test_missing_port(self):
        self.assertRaises(ValueError, self.types, '127.0.0.1')

    def test_missing_address(self):
        self.assertRaises(ValueError, self.types, ':123')

    def test_incorrect_ip(self):
        self.assertRaises(ValueError, self.types, '127.surprise.0.1:2121')

    def test_incorrect_ipv6(self):
        self.assertRaises(ValueError, self.types, '[2001:db8:8a2e:370]:2121')

    def test_incorrect_port(self):
        self.assertRaises(ValueError, self.types, '127.0.0.1:65536')
        self.assertRaises(ValueError, self.types, '127.0.0.1:sample')
