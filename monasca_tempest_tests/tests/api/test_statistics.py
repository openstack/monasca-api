# (C) Copyright 2015-2016 Hewlett Packard Enterprise Development Company LP
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

NUM_MEASUREMENTS = 100
MIN_REQUIRED_MEASUREMENTS = 2
WAIT_TIME = 30
metric_value1 = 1.23
metric_value2 = 4.56


class TestStatistics(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestStatistics, cls).resource_setup()
        name = data_utils.rand_name('name')
        key = data_utils.rand_name('key')
        value = data_utils.rand_name('value')
        cls._test_name = name
        cls._test_key = key
        cls._test_value = value
        cls._start_timestamp = int(time.time() * 1000)
        metrics = [
            helpers.create_metric(name=cls._test_name,
                                  dimensions={cls._test_key: cls._test_value},
                                  timestamp=cls._start_timestamp,
                                  value=metric_value1),
            helpers.create_metric(name=cls._test_name,
                                  dimensions={cls._test_key: cls._test_value},
                                  timestamp=cls._start_timestamp + 1000,
                                  value=metric_value2)
        ]
        cls.monasca_client.create_metrics(metrics)
        start_time_iso = helpers.timestamp_to_iso(cls._start_timestamp)
        query_param = '?name=' + str(name) + '&start_time=' + \
                      start_time_iso + '&end_time=' + \
                      helpers.timestamp_to_iso(cls._start_timestamp + 1000 * 2)
        start_time_iso = helpers.timestamp_to_iso(cls._start_timestamp)
        cls._start_time_iso = start_time_iso

        num_measurements = 0
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = cls.monasca_client.\
                list_measurements(query_param)
            elements = response_body['elements']
            for element in elements:
                if str(element['name']) == name:
                    if len(element['measurements']) >= MIN_REQUIRED_MEASUREMENTS:
                        cls._end_timestamp = cls._start_timestamp + 1000 * 3
                        cls._end_time_iso = helpers.timestamp_to_iso(
                            cls._end_timestamp)
                        return
                    else:
                        num_measurements = len(element['measurements'])
                        break
            time.sleep(constants.RETRY_WAIT_SECS)

        assert False, "Required {} measurements, found {}".format(MIN_REQUIRED_MEASUREMENTS, num_measurements)

    @classmethod
    def resource_cleanup(cls):
        super(TestStatistics, cls).resource_cleanup()

    @test.attr(type="gate")
    def test_list_statistics(self):
        query_parms = '?name=' + str(self._test_name) + \
                      '&statistics=avg,min,max,sum,count' + '&start_time=' + \
                      str(self._start_time_iso) + '&end_time=' + \
                      str(self._end_time_iso) + '&merge_metrics=true' + \
                      '&period=100000'
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) == set(response_body))
        element = response_body['elements'][0]
        self._verify_element(element)
        column = element['columns']
        num_statistics_method = 5
        self._verify_column(column, num_statistics_method)
        statistics = element['statistics'][0]
        self._verify_statistics(statistics, metric_value1, metric_value2)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_no_name(self):
        query_parms = '?merge_metrics=true&statistics=avg&start_time=' + \
                      str(self._start_time_iso) + '&end_time=' + \
                      str(self._end_time_iso)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_no_statistics(self):
        query_parms = '?name=' + str(self._test_name) + '&start_time=' + str(
            self._start_time_iso) + '&end_time=' + str(self._end_time_iso)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_no_start_time(self):
        query_parms = '?name=' + str(self._test_name) + '&statistics=avg'
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_invalid_statistics(self):
        query_parms = '?name=' + str(self._test_name) + '&statistics=abc' + \
                      '&start_time=' + str(self._start_time_iso) + \
                      '&end_time=' + str(self._end_time_iso)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    def test_list_statistics_with_dimensions(self):
        query_parms = '?name=' + str(self._test_name) + '&statistics=avg' \
                      '&start_time=' + str(self._start_time_iso) + \
                      '&end_time=' + str(self._end_time_iso) + \
                      '&dimensions=' + str(self._test_key) + ':' + \
                      str(self._test_value) + '&period=100000'
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)
        dimensions = response_body['elements'][0]['dimensions']
        self.assertEqual(dimensions[self._test_key], self._test_value)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_end_time_equals_start_time(self):
        query_parms = '?name=' + str(self._test_name) + \
                      '&merge_metrics=true&statistics=avg&' \
                      'start_time=' + str(self._start_time_iso) + \
                      '&end_time=' + str(self._start_time_iso) + \
                      '&period=100000'
        self.assertRaises(exceptions.BadRequest,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    def test_list_statistics_with_period(self):
        query_parms = '?name=' + str(self._test_name) + \
                      '&merge_metrics=true&statistics=avg&' \
                      'start_time=' + str(self._start_time_iso) + \
                      '&end_time=' + str(self._end_time_iso) + \
                      '&period=1'
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)
        time_diff = self._end_timestamp - self._start_timestamp
        len_statistics = len(response_body['elements'][0]['statistics'])
        self.assertEqual(time_diff / 1000, len_statistics)

    @test.attr(type="gate")
    def test_list_statistics_with_offset_limit(self):
        start_timestamp = int(time.time() * 1000)
        name = data_utils.rand_name()
        metric = [
            helpers.create_metric(name=name, timestamp=start_timestamp + 0,
                                  dimensions={'key1': 'value-1',
                                              'key2': 'value-1'},
                                  value=1),
            helpers.create_metric(name=name, timestamp=start_timestamp + 500,
                                  dimensions={'key1': 'value-2',
                                              'key2': 'value-2'},
                                  value=2),
            helpers.create_metric(name=name, timestamp=start_timestamp + 1000,
                                  dimensions={'key1': 'value-3',
                                              'key2': 'value-3'},
                                  value=3),
            helpers.create_metric(name=name, timestamp=start_timestamp + 1500,
                                  dimensions={'key1': 'value-4',
                                              'key2': 'value-4'},
                                  value=4)
        ]

        self.monasca_client.create_metrics(metric)
        query_parms = '?name=' + name
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = self.monasca_client.list_metrics(query_parms)
            self.assertEqual(200, resp.status)
            elements = response_body['elements']
            if elements:
                break
            else:
                time.sleep(constants.RETRY_WAIT_SECS)
        self._check_timeout(i, constants.MAX_RETRIES, elements, 4)

        start_time = helpers.timestamp_to_iso(start_timestamp)
        end_timestamp = start_timestamp + 4000
        end_time = helpers.timestamp_to_iso(end_timestamp)
        query_parms = '?name=' + name + '&merge_metrics=true&statistics=avg' \
                      + '&start_time=' + str(start_time) + '&end_time=' + \
                      str(end_time) + '&period=1'
        resp, body = self.monasca_client.list_statistics(query_parms)
        self.assertEqual(200, resp.status)
        elements = body['elements'][0]['statistics']
        first_element = elements[0]

        query_parms = '?name=' + name + '&merge_metrics=true&statistics=avg'\
                      + '&start_time=' + str(start_time) + '&end_time=' + \
                      str(end_time) + '&period=1' + '&limit=4'
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)
        elements = response_body['elements'][0]['statistics']
        self.assertEqual(4, len(elements))
        self.assertEqual(first_element, elements[0])

        for index in xrange(1, 5):
            max_limit = 5 - index

            for limit in xrange(1, max_limit):
                offset_timestamp = start_timestamp + (1000 * index)
                offset = timeutils.iso8601_from_timestamp(offset_timestamp / 1000)

                last_index = index + limit
                expected_elements = elements[index:last_index]

                query_parms = '?name=' + name + '&merge_metrics=true' + \
                                      '&statistics=avg' + '&start_time=' + \
                                      str(start_time) + '&end_time=' + \
                                      str(end_time) + '&period=1' + '&limit=' + \
                                      str(limit) + '&offset=' + str(offset)
                resp, response_body = self.monasca_client.list_statistics(query_parms)
                self.assertEqual(200, resp.status)
                if not response_body['elements']:
                    self.fail("No metrics returned")
                if not response_body['elements'][0]['statistics']:
                    self.fail("No statistics returned")
                new_elements = response_body['elements'][0]['statistics']

                self.assertEqual(limit, len(new_elements))
                # bug in the python API causes limit 1 to not have matching timestamps
                if limit > 1:
                    self.assertEqual(expected_elements, new_elements)


    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_no_merge_metrics(self):
        key = data_utils.rand_name('key')
        value = data_utils.rand_name('value')
        metric3 = helpers.create_metric(
            name=self._test_name,
            dimensions={key: value},
            timestamp=self._start_timestamp + 2000)
        self.monasca_client.create_metrics(metric3)
        query_param = '?name=' + str(self._test_name) + '&start_time=' + \
                      self._start_time_iso + '&end_time=' + helpers.\
            timestamp_to_iso(self._start_timestamp + 1000 * 4) + \
                      '&merge_metrics=True'

        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = self.monasca_client.\
                list_measurements(query_param)
            elements = response_body['elements']
            for element in elements:
                if str(element['name']) == self._test_name and len(
                        element['measurements']) == 3:
                    end_time_iso = helpers.timestamp_to_iso(
                        self._start_timestamp + 1000 * 4)
                    query_parms = '?name=' + str(self._test_name) + \
                                  '&statistics=avg' + '&start_time=' + \
                                  str(self._start_time_iso) + '&end_time=' +\
                                  str(end_time_iso) + '&period=100000'
                    self.assertRaises(exceptions.Conflict,
                                      self.monasca_client.list_statistics,
                                      query_parms)
                    return
            time.sleep(constants.RETRY_WAIT_SECS)
        self._check_timeout(i, constants.MAX_RETRIES, elements, 3)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_name_exceeds_max_length(self):
        long_name = "x" * (constants.MAX_LIST_STATISTICS_NAME_LENGTH + 1)
        query_parms = '?name=' + str(long_name) + '&merge_metrics=true' + \
                      '&start_time=' + str(self._start_time_iso) + \
                      '&end_time=' + str(self._end_time_iso)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    def test_list_statistics_response_body_statistic_result_type(self):
        query_parms = '?name=' + str(self._test_name) + '&period=100000' + \
                      '&statistics=avg' + '&merge_metrics=true' + \
                      '&start_time=' + str(self._start_time_iso) + \
                      '&end_time=' + str(self._end_time_iso)
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)
        element = response_body['elements'][0]
        statistic = element['statistics']
        statistic_result_type = type(statistic[0][1])
        self.assertEqual(statistic_result_type, float)

    def _verify_statistics(self, statistics, num1, num2):
        self.assertTrue(type(statistics) is list)
        self.assertEqual(statistics[1], (num1 + num2) / 2)
        self.assertEqual(statistics[2], min(num1, num2))
        self.assertEqual(statistics[3], max(num1, num2))
        self.assertEqual(statistics[4], num1 + num2)
        self.assertEqual(statistics[5], 2)

    def _verify_element(self, element):
        self.assertTrue(set(['id', 'name', 'dimensions', 'columns',
                             'statistics']) == set(element))
        self.assertTrue(type(element['id']) is unicode)
        self.assertTrue(element['id'] is not None)
        self.assertTrue(type(element['name']) is unicode)
        self.assertTrue(type(element['dimensions']) is dict)
        self.assertEqual(len(element['dimensions']), 0)
        self.assertTrue(type(element['columns']) is list)
        self.assertTrue(type(element['statistics']) is list)
        self.assertEqual(element['name'], self._test_name)

    def _verify_column(self, column, num_statistics_method):
        self.assertTrue(type(column) is list)
        self.assertEqual(len(column), num_statistics_method + 1)
        self.assertEqual(column[0], 'timestamp')
        self.assertEqual(column[1], 'avg')
        self.assertEqual(column[2], 'min')
        self.assertEqual(column[3], 'max')
        self.assertEqual(column[4], 'sum')
        self.assertEqual(column[5], 'count')

    def _check_timeout(self, timer, max_retries, elements,
                       expect_num_elements):
        if timer == max_retries - 1:
            error_msg = ("Failed: timeout on waiting for metrics: {} elements "
                         "are needed. Current number of elements = {}").\
                format(expect_num_elements, len(elements))
            raise self.fail(error_msg)
