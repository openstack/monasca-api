# (C) Copyright 2016 Hewlett Packard Enterprise Development LP
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

import time
from urllib import urlencode

from tempest.common.utils import data_utils
from tempest.lib import exceptions
from tempest import test

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import constants
from monasca_tempest_tests.tests.api import helpers


class TestDimensions(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestDimensions, cls).resource_setup()
        metric_name1 = data_utils.rand_name()
        name1 = "name_1"
        name2 = "name_2"
        value1 = "value_1"
        value2 = "value_2"

        timestamp = int(round(time.time() * 1000))
        time_iso = helpers.timestamp_to_iso(timestamp)

        metric1 = helpers.create_metric(name=metric_name1,
                                        dimensions={name1: value1,
                                                    name2: value2
                                                    })
        cls.monasca_client.create_metrics(metric1)
        metric1 = helpers.create_metric(name=metric_name1,
                                        dimensions={name1: value2})
        cls.monasca_client.create_metrics(metric1)

        metric_name2 = data_utils.rand_name()
        name3 = "name_3"
        value3 = "value_3"
        metric2 = helpers.create_metric(name=metric_name2,
                                        dimensions={name3: value3})
        cls.monasca_client.create_metrics(metric2)

        metric_name3 = data_utils.rand_name()
        metric3 = helpers.create_metric(name=metric_name3,
                                        dimensions={name1: value3})

        cls.monasca_client.create_metrics(metric3)

        cls._test_metric1 = metric1
        cls._test_metric2 = metric2
        cls._test_metric_names = {metric_name1, metric_name2, metric_name3}
        cls._dim_names_metric1 = [name1, name2]
        cls._dim_names_metric2 = [name3]
        cls._dim_names = cls._dim_names_metric1 + cls._dim_names_metric2
        cls._dim_values_for_metric1 = [value1, value2]
        cls._dim_values = [value1, value2, value3]

        param = '?start_time=' + time_iso
        returned_name_set = set()
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = cls.monasca_client.list_metrics(
                param)
            elements = response_body['elements']
            for element in elements:
                returned_name_set.add(str(element['name']))
            if cls._test_metric_names.issubset(returned_name_set):
                return
            time.sleep(constants.RETRY_WAIT_SECS)

        assert False, 'Unable to initialize metrics'

    @classmethod
    def resource_cleanup(cls):
        super(TestDimensions, cls).resource_cleanup()

    @test.attr(type='gate')
    def test_list_dimension_values_without_metric_name(self):
        param = '?dimension_name=' + self._dim_names[0]
        resp, response_body = self.monasca_client.list_dimension_values(param)
        self.assertEqual(200, resp.status)
        self.assertTrue({'links', 'elements'} == set(response_body))
        response_values_length = len(response_body['elements'])
        values = [str(response_body['elements'][i]['dimension_value'])
                  for i in xrange(response_values_length)]
        self.assertEqual(values, self._dim_values)

    @test.attr(type='gate')
    def test_list_dimension_values_with_metric_name(self):
        parms = '?metric_name=' + self._test_metric1['name']
        parms += '&dimension_name=' + self._dim_names[0]
        resp, response_body = self.monasca_client.list_dimension_values(parms)
        self.assertEqual(200, resp.status)
        self.assertTrue({'links', 'elements'} == set(response_body))
        response_values_length = len(response_body['elements'])
        values = [str(response_body['elements'][i]['dimension_value'])
                  for i in xrange(response_values_length)]
        self.assertEqual(values, self._dim_values_for_metric1)

    @test.attr(type='gate')
    def test_list_dimension_values_limit_and_offset(self):
        param = '?dimension_name=' + self._dim_names[0]
        resp, response_body = self.monasca_client.list_dimension_values(param)
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
        num_dim_values = len(elements)
        for limit in xrange(1, num_dim_values):
            start_index = 0
            params = [('limit', limit)]
            offset = None
            while True:
                num_expected_elements = limit
                if (num_expected_elements + start_index) > num_dim_values:
                    num_expected_elements = num_dim_values - start_index

                these_params = list(params)
                # If not the first call, use the offset returned by the last
                # call
                if offset:
                    these_params.extend([('offset', str(offset))])
                query_parms = '?dimension_name=' + self._dim_names[0] + '&' + \
                              urlencode(these_params)
                resp, response_body = \
                    self.monasca_client.list_dimension_values(query_parms)
                self.assertEqual(200, resp.status)
                if not response_body['elements']:
                    self.fail("No metrics returned")
                response_values_length = len(response_body['elements'])
                if response_values_length == 0:
                    self.fail("No dimension names returned")
                new_elements = [str(response_body['elements'][i]
                                    ['dimension_value']) for i in
                                xrange(response_values_length)]
                self.assertEqual(num_expected_elements, len(new_elements))

                expected_elements = elements[start_index:start_index + limit]
                expected_dimension_values = \
                    [expected_elements[i]['dimension_value'] for i in xrange(
                        len(expected_elements))]
                self.assertEqual(expected_dimension_values, new_elements)
                start_index += num_expected_elements
                if start_index >= num_dim_values:
                    break
                # Get the next set
                offset = self._get_offset(response_body)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_list_dimension_values_no_dimension_name(self):
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_dimension_values)

    @test.attr(type='gate')
    def test_list_dimension_names(self):
        resp, response_body = self.monasca_client.list_dimension_names()
        self.assertEqual(200, resp.status)
        self.assertTrue({'links', 'elements'} == set(response_body))
        response_names_length = len(response_body['elements'])
        names = [str(response_body['elements'][i]['dimension_name']) for i
                 in xrange(response_names_length)]
        self.assertEqual(names, self._dim_names)

    @test.attr(type='gate')
    def test_list_dimension_names_with_metric_name(self):
        self._test_list_dimension_names_with_metric_name(
            self._test_metric1['name'], self._dim_names_metric1)
        self._test_list_dimension_names_with_metric_name(
            self._test_metric2['name'], self._dim_names_metric2)

    @test.attr(type='gate')
    def test_list_dimension_names_limit_and_offset(self):
        resp, response_body = self.monasca_client.list_dimension_names()
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
        num_dim_names = len(elements)
        for limit in xrange(1, num_dim_names):
            start_index = 0
            params = [('limit', limit)]
            offset = None
            while True:
                num_expected_elements = limit
                if (num_expected_elements + start_index) > num_dim_names:
                    num_expected_elements = num_dim_names - start_index

                these_params = list(params)
                # If not the first call, use the offset returned by the last
                # call
                if offset:
                    these_params.extend([('offset', str(offset))])
                query_parms = '?' + urlencode(these_params)
                resp, response_body = self.monasca_client.list_dimension_names(
                    query_parms)
                self.assertEqual(200, resp.status)
                if not response_body['elements']:
                    self.fail("No metrics returned")
                response_names_length = len(response_body['elements'])
                if response_names_length == 0:
                    self.fail("No dimension names returned")
                new_elements = [str(response_body['elements'][i]
                                    ['dimension_name']) for i in
                                xrange(response_names_length)]
                self.assertEqual(num_expected_elements, len(new_elements))

                expected_elements = elements[start_index:start_index + limit]
                expected_dimension_names = \
                    [expected_elements[i]['dimension_name'] for i in xrange(
                        len(expected_elements))]
                self.assertEqual(expected_dimension_names, new_elements)
                start_index += num_expected_elements
                if start_index >= num_dim_names:
                    break
                # Get the next set
                offset = self._get_offset(response_body)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_list_dimension_names_with_wrong_metric_name(self):
        self._test_list_dimension_names_with_metric_name(
            'wrong_metric_name', [])

    def _test_list_dimension_names_with_metric_name(self, metric_name,
                                                    dimension_names):
        param = '?metric_name=' + metric_name
        resp, response_body = self.monasca_client.list_dimension_names(param)
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) == set(response_body))
        response_names_length = len(response_body['elements'])
        names = [str(response_body['elements'][i]['dimension_name']) for i
                 in xrange(response_names_length)]
        self.assertEqual(names, dimension_names)
