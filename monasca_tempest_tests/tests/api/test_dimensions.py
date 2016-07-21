# (C) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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

from oslo_utils import timeutils

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import constants
from monasca_tempest_tests.tests.api import helpers
from tempest.common.utils import data_utils
from tempest import test
from tempest.lib import exceptions


class TestDimensions(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestDimensions, cls).resource_setup()
        name = data_utils.rand_name()
        key = data_utils.rand_name()
        value1 = "value_1"
        value2 = "value_2"
        cls._param = key + ':' + value1
        cls._dimension_name = key
        cls._dim_val_1 = value1
        cls._dim_val_2 = value2
        metric = helpers.create_metric(name=name,
                                       dimensions={key: value1})
        cls.monasca_client.create_metrics(metric)
        metric = helpers.create_metric(name=name,
                                       dimensions={key: value2})
        cls.monasca_client.create_metrics(metric)
        cls._test_metric = metric

        start_time = str(timeutils.iso8601_from_timestamp(
                         metric['timestamp'] / 1000.0))
        parms = '?name=' + str(cls._test_metric['name']) + \
                '&start_time=' + start_time

        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = cls.monasca_client.list_metrics(
                parms)
            elements = response_body['elements']
            for element in elements:
                if str(element['name']) == cls._test_metric['name']:
                    return
            time.sleep(constants.RETRY_WAIT_SECS)

        assert False, 'Unable to initialize metrics'

    @classmethod
    def resource_cleanup(cls):
        super(TestDimensions, cls).resource_cleanup()

    @test.attr(type='gate')
    def test_list_dimension_values_without_metric_name(self):
        parms = '?dimension_name=' + self._dimension_name
        resp, response_body = self.monasca_client.list_dimension_values(parms)
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) == set(response_body))
        if not self._is_dimension_name_in_list(response_body):
            self.fail('Dimension name not found in response')
        if self._is_metric_name_in_list(response_body):
            self.fail('Metric name was in response and should not be')
        if not self._are_dim_vals_in_list(response_body):
            self.fail('Dimension value not found in response')

    @test.attr(type='gate')
    def test_list_dimension_values_with_metric_name(self):
        parms = '?metric_name=' + self._test_metric['name']
        parms += '&dimension_name=' + self._dimension_name
        resp, response_body = self.monasca_client.list_dimension_values(parms)
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) == set(response_body))
        if not self._is_metric_name_in_list(response_body):
            self.fail('Metric name not found in response')
        if not self._is_dimension_name_in_list(response_body):
            self.fail('Dimension name not found in response')
        if not self._are_dim_vals_in_list(response_body):
            self.fail('Dimension value not found in response')

    @test.attr(type='gate')
    def test_list_dimension_values_limit_and_offset(self):
        parms = '?dimension_name=' + self._dimension_name
        parms += '&limit=1'
        resp, response_body = self.monasca_client.list_dimension_values(parms)
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) == set(response_body))
        if not self._is_dimension_name_in_list(response_body):
            self.fail('Dimension name not found in response')
        if not self._is_dim_val_in_list(response_body, self._dim_val_1):
            self.fail('First dimension value not found in response')
        if not self._is_offset_in_links(response_body, self._dim_val_1):
            self.fail('Offset not found in response')
        if not self._is_limit_in_links(response_body):
            self.fail('Limit not found in response')

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_list_dimension_values_no_dimension_name(self):
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_dimension_values)

    def _is_metric_name_in_list(self, response_body):
        elements = response_body['elements'][0]
        if 'metric_name' not in elements:
            return False
        if str(elements['metric_name']) == self._test_metric['name']:
            return True
        return False

    def _is_dimension_name_in_list(self, response_body):
        elements = response_body['elements'][0]
        if str(elements['dimension_name']) == self._dimension_name:
            return True
        return False

    def _are_dim_vals_in_list(self, response_body):
        elements = response_body['elements'][0]
        have_dim_1 = self._is_dim_val_in_list(response_body, self._dim_val_1)
        have_dim_2 = self._is_dim_val_in_list(response_body, self._dim_val_2)
        if have_dim_1 and have_dim_1:
            return True
        return False

    def _is_dim_val_in_list(self, response_body, dim_val):
        elements = response_body['elements'][0]
        if dim_val in elements['values']:
            return True
        return False

    def _is_offset_in_links(self, response_body, dim_val):
        links = response_body['links']
        offset = "offset=" + dim_val
        for link in links:
            if offset in link['href']:
                return True
        return False

    def _is_limit_in_links(self, response_body):
        links = response_body['links']
        limit = "limit=1"
        for link in links:
            if limit in link['href']:
                return True
        return False
