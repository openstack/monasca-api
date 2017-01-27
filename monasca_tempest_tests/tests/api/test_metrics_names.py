# (C) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
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

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import constants
from monasca_tempest_tests.tests.api import helpers
from tempest.common.utils import data_utils
from tempest import test
from urllib import urlencode


class TestMetricsNames(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestMetricsNames, cls).resource_setup()
        name1 = data_utils.rand_name('name1')
        name2 = data_utils.rand_name('name2')
        name3 = data_utils.rand_name('name3')
        key = data_utils.rand_name()
        key1 = data_utils.rand_name()
        value = data_utils.rand_name()
        value1 = data_utils.rand_name()

        timestamp = int(round(time.time() * 1000))
        time_iso = helpers.timestamp_to_iso(timestamp)

        metric1 = helpers.create_metric(name=name1,
                                        dimensions={key: value})
        metric2 = helpers.create_metric(name=name2,
                                        dimensions={key1: value1})
        metric3 = helpers.create_metric(name=name3,
                                        dimensions={key: value})
        cls._test_metric_names = {name1, name2, name3}
        cls._expected_names_list = list(cls._test_metric_names)
        cls._expected_names_list.sort()
        cls._test_metric_names_with_same_dim = [name1, name3]
        cls._test_metrics = [metric1, metric2, metric3]
        cls._dimensions_param = key + ':' + value

        cls.monasca_client.create_metrics(cls._test_metrics)

        query_param = '?start_time=' + time_iso
        returned_name_set = set()
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = cls.monasca_client.list_metrics(query_param)
            elements = response_body['elements']
            for element in elements:
                returned_name_set.add(str(element['name']))
            if cls._test_metric_names.issubset(returned_name_set):
                return
            time.sleep(constants.RETRY_WAIT_SECS)

        assert False, 'Unable to initialize metrics'

    @classmethod
    def resource_cleanup(cls):
        super(TestMetricsNames, cls).resource_cleanup()

    @test.attr(type='gate')
    def test_list_metrics_names(self):
        resp, response_body = self.monasca_client.list_metrics_names()
        metric_names = self._verify_response(resp, response_body)
        self.assertEqual(metric_names, self._expected_names_list)

    @test.attr(type='gate')
    def test_list_metrics_names_with_dimensions(self):
        query_params = '?dimensions=' + self._dimensions_param
        resp, response_body = self.monasca_client.list_metrics_names(
            query_params)
        metric_names = self._verify_response(resp, response_body)
        self.assertEqual(metric_names,
                         self._test_metric_names_with_same_dim)

    @test.attr(type='gate')
    def test_list_metrics_names_with_limit_offset(self):
        resp, response_body = self.monasca_client.list_metrics_names()
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
        num_names = len(elements)

        for limit in xrange(1, num_names):
            start_index = 0
            params = [('limit', limit)]
            offset = None
            while True:
                num_expected_elements = limit
                if (num_expected_elements + start_index) > num_names:
                    num_expected_elements = num_names - start_index

                these_params = list(params)
                # If not the first call, use the offset returned by the last
                # call
                if offset:
                    these_params.extend([('offset', str(offset))])
                query_params = '?' + urlencode(these_params)

                resp, response_body = \
                    self.monasca_client.list_metrics_names(query_params)
                new_elements = self._verify_response(resp, response_body)
                self.assertEqual(num_expected_elements, len(new_elements))

                expected_elements = elements[start_index:start_index + limit]
                expected_names = \
                    [expected_elements[i]['name'] for i in xrange(
                        len(expected_elements))]

                self.assertEqual(expected_names, new_elements)
                start_index += num_expected_elements
                if start_index >= num_names:
                    break
                # Get the next set
                offset = self._get_offset(response_body)

    @test.attr(type='gate')
    def test_list_metrics_names_with_offset_not_in_metrics_names_list(self):
        offset1 = 'tempest-abc'
        offset2 = 'tempest-name111'
        offset3 = 'tempest-name4-random'
        query_param1 = '?' + urlencode([('offset', offset1)])
        query_param2 = '?' + urlencode([('offset', offset2)])
        query_param3 = '?' + urlencode([('offset', offset3)])

        resp, response_body = self.monasca_client.list_metrics_names(
            query_param1)
        metric_names = self._verify_response(resp, response_body)

        self.assertEqual(metric_names, self._expected_names_list[:])

        resp, response_body = self.monasca_client.list_metrics_names(
            query_param2)
        metric_names = self._verify_response(resp, response_body)
        self.assertEqual(metric_names, self._expected_names_list[1:])

        resp, response_body = self.monasca_client.list_metrics_names(
            query_param3)
        self.assertEqual(response_body['elements'], [])

    def _verify_response(self, resp, response_body):
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) == set(response_body))

        response_names_length = len(response_body['elements'])
        if response_names_length == 0:
            self.fail("No metric names returned")

        metric_names = [str(response_body['elements'][i]['name']) for i in
                        xrange(response_names_length)]
        return metric_names
