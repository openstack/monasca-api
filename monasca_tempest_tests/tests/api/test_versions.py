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


class TestVersions(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestVersions, cls).resource_setup()

    @test.attr(type='gate')
    def test_get_version(self):
        resp, response_body = self.monasca_client.get_version()
        self.assertEqual(resp.status, 200)
        response_body = json.loads(response_body)

        self.assertIsInstance(response_body, dict)
        version = response_body
        self.assertTrue(set(['id', 'links', 'status', 'updated']) ==
                        set(version))
        self.assertEqual(version['id'], u'v2.0')
        self.assertEqual(version['status'], u'CURRENT')
        date_object = datetime.datetime.strptime(version['updated'],
                                                 "%Y-%m-%dT%H:%M:%S.%fZ")
        self.assertIsInstance(date_object, datetime.datetime)
        links = response_body['links']
        self.assertIsInstance(links, list)
        link = links[0]
        self.assertTrue(set(['rel', 'href']) ==
                        set(link))
        self.assertEqual(link['rel'], u'self')
        self.assertTrue(link['href'].endswith('/v2.0'))
