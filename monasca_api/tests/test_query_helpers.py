# Copyright 2015 Cray Inc. All Rights Reserved.
# Copyright 2016 Hewlett Packard Enterprise Development Company, L.P.
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

import unittest

from mock import Mock

import monasca_api.v2.reference.helpers as helpers


class TestGetQueryDimension(unittest.TestCase):

    def test_no_dimensions(self):
        req = Mock()

        req.query_string = "foo=bar"

        result = helpers.get_query_dimensions(req)

        self.assertEqual(result, {})

    def test_one_dimensions(self):
        req = Mock()

        req.query_string = "foo=bar&dimensions=Dimension:Value"

        result = helpers.get_query_dimensions(req)

        self.assertEqual(result, {"Dimension": "Value"})

    def test_comma_sep_dimensions(self):
        req = Mock()

        req.query_string = ("foo=bar&"
                            "dimensions=Dimension:Value,Dimension-2:Value-2")

        result = helpers.get_query_dimensions(req)

        self.assertEqual(
            result, {"Dimension": "Value", "Dimension-2": "Value-2"})

    def test_multiple_dimension_params(self):
        req = Mock()

        req.query_string = ("foo=bar&"
                            "dimensions=Dimension:Value&"
                            "dimensions=Dimension-2:Value-2")

        result = helpers.get_query_dimensions(req)

        self.assertEqual(
            result, {"Dimension": "Value", "Dimension-2": "Value-2"})

    def test_multiple_dimension_params_with_comma_sep_dimensions(self):
        req = Mock()

        req.query_string = ("foo=bar&"
                            "dimensions=Dimension-3:Value-3&"
                            "dimensions=Dimension:Value,Dimension-2:Value-2")

        result = helpers.get_query_dimensions(req)

        self.assertEqual(
            result, {"Dimension": "Value",
                     "Dimension-2": "Value-2",
                     "Dimension-3": "Value-3"})

    def test_dimension_no_value(self):
        req = Mock()
        req.query_string = ("foo=bar&dimensions=Dimension_no_value")

        result = helpers.get_query_dimensions(req)
        self.assertEqual(result, {"Dimension_no_value": ""})

    def test_dimension_multi_value(self):
        req = Mock()
        req.query_string = ("foo=bar&dimensions=Dimension_multi_value:one|two|three")

        result = helpers.get_query_dimensions(req)
        self.assertEqual(result, {"Dimension_multi_value": "one|two|three"})

    def test_dimension_with_multi_colons(self):
        req = Mock()
        req.query_string = ("foo=bar&dimensions=url:http://192.168.10.4:5601,"
                            "hostname:monasca,component:kibana,service:monitoring")

        result = helpers.get_query_dimensions(req)
        self.assertEqual(result, {"url": "http://192.168.10.4:5601",
                                  "hostname": "monasca",
                                  "component": "kibana",
                                  "service": "monitoring"})

    def test_empty_dimension(self):
        req = Mock()
        req.query_string = ("foo=bar&dimensions=")

        result = helpers.get_query_dimensions(req)
        self.assertEqual(result, {})


class TestGetOldQueryParams(unittest.TestCase):

    def test_old_query_params(self):
        uri = Mock()
        uri.query = "foo=bar&spam=ham"

        result = helpers._get_old_query_params(uri)
        self.assertEqual(result, ["foo=bar", "spam=ham"])

    def test_old_query_params_with_equals(self):
        uri = Mock()
        uri.query = "foo=spam=ham"

        result = helpers._get_old_query_params(uri)
        self.assertEqual(result, ["foo=spam%3Dham"])

    def test_old_query_params_except_offset(self):
        uri = Mock()
        uri.query = "foo=bar&spam=ham"
        result = []

        helpers._get_old_query_params_except_offset(result, uri)
        self.assertEqual(result, ["foo=bar", "spam=ham"])

    def test_old_query_params_except_offset_with_equals(self):
        uri = Mock()
        uri.query = "foo=spam=ham&offset=bar"
        result = []

        helpers._get_old_query_params_except_offset(result, uri)
        self.assertEqual(result, ["foo=spam%3Dham"])
