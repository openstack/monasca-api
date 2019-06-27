# Copyright 2015 Hewlett-Packard
# Copyright 2017 Fujitsu LIMITED
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

import falcon

from monasca_api.tests import base
from monasca_api.v2.reference import versions


class TestVersions(base.BaseApiTestCase):

    def setUp(self):
        super(TestVersions, self).setUp()
        self.app.add_route('/', versions.Versions())
        self.app.add_route('/{version_id}', versions.Versions())

    def test_list_versions(self):
        result = self.simulate_request(path='/')
        self.assertEqual(result.status, falcon.HTTP_200)
        response = result.json
        self.assertIsInstance(response, dict)
        self.assertEqual(set(['links', 'elements']),
                         set(response))
        links = response['links']
        self.assertIsInstance(links, list)
        link = links[0]
        self.assertEqual(set(['rel', 'href']),
                         set(link))
        self.assertEqual(link['rel'], u'self')
        self.assertTrue(link['href'].endswith('/'))

    def test_valid_version_id(self):
        result = self.simulate_request(path='/v2.0')
        self.assertEqual(result.status, falcon.HTTP_200)
        response = result.json
        self.assertIsInstance(response, dict)
        version = response
        self.assertEqual(set(['id', 'links', 'status', 'updated']),
                         set(version))
        self.assertEqual(version['id'], u'v2.0')
        self.assertEqual(version['status'], u'CURRENT')
        date_object = datetime.datetime.strptime(version['updated'],
                                                 "%Y-%m-%dT%H:%M:%S.%fZ")
        self.assertIsInstance(date_object, datetime.datetime)
        links = response['links']
        self.assertIsInstance(links, list)
        link = links[0]
        self.assertEqual(set(['rel', 'href']),
                         set(link))
        self.assertEqual(link['rel'], u'self')
        self.assertTrue(link['href'].endswith('/v2.0'))

    def test_invalid_version_id(self):
        result = self.simulate_request(path='/v1.0')
        self.assertEqual(result.status, '422 Unprocessable Entity')
