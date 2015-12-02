# Copyright 2015 Hewlett-Packard
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

import datetime
import json

import falcon
import falcon.testing as testing

from monasca_api.v2.reference import versions


class TestVersions(testing.TestBase):

    def before(self):
        self.versions_resource = versions.Versions()
        self.api.add_route('/versions', self.versions_resource)
        self.api.add_route('/versions/{version_id}', self.versions_resource)

    def test_list_versions(self):
        result = self.simulate_request('/versions')
        self.assertEqual(self.srmock.status, falcon.HTTP_200)
        response = json.loads(result[0])
        self.assertIsInstance(response, dict)
        self.assertTrue(set(['links', 'elements']) ==
                        set(response))
        links = response['links']
        self.assertIsInstance(links, list)
        link = links[0]
        self.assertTrue(set(['rel', 'href']) ==
                        set(link))
        self.assertEqual(link['rel'], u'self')
        self.assertTrue(link['href'].endswith('versions'))

    def test_valid_version_id(self):
        result = self.simulate_request('/versions/v2.0')
        self.assertEqual(self.srmock.status, falcon.HTTP_200)
        response = json.loads(result[0])
        self.assertIsInstance(response, dict)
        version = response
        self.assertTrue(set(['id', 'links', 'status', 'updated']) ==
                        set(version))
        self.assertEqual(version['id'], u'v2.0')
        self.assertEqual(version['status'], u'CURRENT')
        date_object = datetime.datetime.strptime(version['updated'],
                                                 "%Y-%m-%dT%H:%M:%S.%fZ")
        self.assertIsInstance(date_object, datetime.datetime)
        links = response['links']
        self.assertIsInstance(links, list)
        link = links[0]
        self.assertTrue(set(['rel', 'href']) ==
                        set(link))
        self.assertEqual(link['rel'], u'self')
        self.assertTrue(link['href'].endswith('/versions/v2.0'))

    def test_invalid_version_id(self):
        self.simulate_request('/versions/v1.0')
        self.assertEqual(self.srmock.status, '422 Unprocessable Entity')
