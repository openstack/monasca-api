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

import time

from oslo_utils import timeutils

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import constants
from monasca_tempest_tests.tests.api import helpers
from tempest.common.utils import data_utils
from tempest import test
from tempest_lib import exceptions

NUM_MEASUREMENTS = 100
WAIT_TIME = 30


class TestStatistics(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestStatistics, cls).resource_setup()

        start_timestamp = int(time.time() * 1000)
        end_timestamp = int(time.time() * 1000) + NUM_MEASUREMENTS * 1000
        metrics = []

        for i in xrange(NUM_MEASUREMENTS):
            metric = helpers.create_metric(
                name="name-1",
                timestamp=start_timestamp + i)
            metrics.append(metric)

        cls.monasca_client.create_metrics(metric)
        cls._start_timestamp = start_timestamp
        cls._end_timestamp = end_timestamp
        cls._metrics = metrics

    @classmethod
    def resource_cleanup(cls):
        super(TestStatistics, cls).resource_cleanup()

    @test.attr(type="gate")
    def test_list_statistics(self):
        start_time = timeutils.iso8601_from_timestamp(self._start_timestamp /
                                                      1000)
        end_time = timeutils.iso8601_from_timestamp(self._end_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=true&statistics=avg' \
                      '&start_time=' + str(start_time) + '&end_time=' \
                      + str(end_time)
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)

        self.assertTrue(set(['links', 'elements']) == set(response_body))
        elements = response_body['elements']
        element = elements[0]
        self.assertTrue(set(['id', 'name', 'dimensions', 'columns',
                             'statistics']) == set(element))
        # check if 'id' is unicode type
        self.assertTrue(type(element['id']) is unicode)
        # check if 'name' is a string. NOPE its unicode
        self.assertTrue(type(element['name']) is unicode)
        self.assertTrue(type(element['dimensions']) is dict)
        self.assertTrue(type(element['columns']) is list)
        self.assertTrue(type(element['statistics']) is list)
        statistic = element['statistics']
        column = element['columns']
        self.assertTrue(type(statistic) is list)
        self.assertTrue(type(column) is list)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_no_name(self):
        start_time = timeutils.iso8601_from_timestamp(self._start_timestamp /
                                                      1000)
        end_time = timeutils.iso8601_from_timestamp(self._end_timestamp / 1000)
        query_parms = '?merge_metrics=true&statistics=avg&start_time=' + \
                      str(start_time) + '&end_time=' + str(end_time)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_no_statistics(self):
        start_time = timeutils.iso8601_from_timestamp(self._start_timestamp /
                                                      1000)
        end_time = timeutils.iso8601_from_timestamp(self._end_timestamp / 1000)
        query_parms = '?name=name-1&start_time=' + str(start_time) + \
                      '&end_time=' + str(end_time)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_no_start_time(self):
        query_parms = '?name=name-1&statistics=avg'
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_invalid_statistics(self):
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp / 1000)
        end_time = timeutils.iso8601_from_timestamp(self._end_timestamp / 1000)
        query_parms = '?name=name-1&statistics=abc&start_time=' + str(
            start_time) + '&end_time=' + str(end_time)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    def test_list_statistics_with_dimensions(self):
        start_time = timeutils.iso8601_from_timestamp(self._start_timestamp
                                                      / 1000)
        end_time = timeutils.iso8601_from_timestamp(self._end_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=true&statistics=avg&' \
                      'start_time=' + str(start_time) + '&end_time=' + \
                      str(end_time) + '&dimensions=key1:value1'
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)

    @test.attr(type="gate")
    def test_list_statistics_with_end_time(self):
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp / 1000)
        end_time = timeutils.iso8601_from_timestamp(
            self._end_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=true&statistics=avg&' \
                      'start_time=' + str(start_time) + '&end_time=' + \
                      str(end_time)
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)

    @test.attr(type="gate")
    def test_list_statistics_with_period(self):
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp / 1000)
        end_time = timeutils.iso8601_from_timestamp(
            self._end_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=true&statistics=avg&' \
                      'start_time=' + str(start_time) + '&end_time=' + \
                      str(end_time) + '&period=300'
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)

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
                                  value=4),
            helpers.create_metric(name=name, timestamp=start_timestamp + 2000,
                                  dimensions={'key1': 'value-2',
                                              'key2': 'value-2'},
                                  value=5),
            helpers.create_metric(name=name, timestamp=start_timestamp + 2500,
                                  dimensions={'key1': 'value-3',
                                              'key2': 'value-3'},
                                  value=6),
            helpers.create_metric(name=name, timestamp=start_timestamp + 3000,
                                  dimensions={'key1': 'value-4',
                                              'key2': 'value-4'},
                                  value=7),
            helpers.create_metric(name=name, timestamp=start_timestamp + 3500,
                                  dimensions={'key1': 'value-4',
                                              'key2': 'value-4'},
                                  value=8)
        ]

        self.monasca_client.create_metrics(metric)

        for timer in xrange(WAIT_TIME):
            query_parms = '?name=' + name
            resp, response_body = self.monasca_client.list_metrics(query_parms)
            self.assertEqual(200, resp.status)
            elements = response_body['elements']
            if elements:
                break
            else:
                time.sleep(1)
        if timer == WAIT_TIME - 1:
            skip_msg = ("Skipped test_list_statistics_with_offset_limit: "
                        "timeout on waiting for metrics: 4 elements are "
                        "needed. Current number of elements = {}").\
                format(len(elements))
            raise self.skipException(skip_msg)

        start_time = timeutils.iso8601_from_timestamp(
            start_timestamp / 1000)
        end_timestamp = start_timestamp + 4000
        end_time = timeutils.iso8601_from_timestamp(end_timestamp / 1000)
        query_parms = '?name=' + name + '&merge_metrics=true&statistics=avg,' \
                      'max,min,sum,count&start_time=' + str(start_time) + \
                      '&end_time=' + str(end_time) + '&period=1'
        resp, body = self.monasca_client.list_statistics(query_parms)
        self.assertEqual(200, resp.status)
        elements = body['elements'][0]['statistics']
        first_element = elements[0]
        last_element = elements[3]

        query_parms = '?name=' + name + '&merge_metrics=true&statistics=avg,' \
                      'max,min,sum,count&start_time=' + str(start_time) + \
                      '&end_time=' + str(end_time) + '&period=1' + '&limit=4'
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)
        elements = response_body['elements'][0]['statistics']
        self.assertEqual(4, len(elements))
        self.assertEqual(first_element, elements[0])
        timeout = time.time() + 60 * 1   # 1 minute timeout
        for limit in xrange(1, 5):
            next_element = elements[limit - 1]
            offset_timestamp = start_timestamp
            while True:
                if time.time() >= timeout:
                    msg = "Failed test_list_statistics_with_offset_limit: " \
                          "one minute timeout on offset limit test loop."
                    raise exceptions.TimeoutException(msg)
                else:
                    offset_timestamp += 1000 * limit
                    offset = timeutils.iso8601_from_timestamp(
                        offset_timestamp / 1000)
                    query_parms = '?name=' + name + '&merge_metrics=true' + \
                                  '&statistics=avg,max,min,sum,' \
                                  'count&start_time=' + str(start_time) + \
                                  '&end_time=' + str(end_time) + \
                                  '&period=1' + '&limit=' + str(limit) + \
                                  '&offset=' + str(offset)
                    resp, response_body = self.monasca_client.list_statistics(
                        query_parms)
                    self.assertEqual(200, resp.status)
                    new_elements = response_body['elements'][0]['statistics']

                    if len(new_elements) > limit - 1:
                        self.assertEqual(limit, len(new_elements))
                        next_element = new_elements[limit - 1]
                    elif 0 < len(new_elements) <= limit - 1:
                        self.assertEqual(last_element, new_elements[0])
                        break
                    else:
                        self.assertEqual(last_element, next_element)
                        break

    @test.attr(type="gate")
    def test_list_statistics_with_merge_metrics(self):
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp / 1000)
        end_time = timeutils.iso8601_from_timestamp(self._end_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=true&statistics=avg&' \
                      'merge_metrics=true&start_time=' + str(start_time) + \
                      '&end_time=' + str(end_time)
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_no_merge_metrics(self):
        start_time = timeutils.\
            iso8601_from_timestamp(self._start_timestamp / 1000)
        end_time = timeutils.\
            iso8601_from_timestamp(self._end_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=false&' \
                      'statistics=avg,min,max&start_time=' + str(start_time)\
                      + '&end_time=' + str(end_time)
        self.assertRaises(exceptions.Conflict,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_statistics_with_name_exceeds_max_length(self):
        long_name = "x" * (constants.MAX_LIST_STATISTICS_NAME_LENGTH + 1)
        start_time = timeutils.iso8601_from_timestamp(self._start_timestamp
                                                      / 1000)
        end_time = timeutils.iso8601_from_timestamp(self._end_timestamp / 1000)
        query_parms = '?merge_metrics=true&name=' + str(long_name) + \
                      '&start_time=' + str(start_time) + '&end_time=' + str(
                      end_time)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_statistics, query_parms)

    @test.attr(type="gate")
    def test_list_statistics_with_more_than_one_statistics(self):
        start_time = timeutils.\
            iso8601_from_timestamp(self._start_timestamp / 1000)
        end_time = timeutils.iso8601_from_timestamp(self._end_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=true&' \
                      'statistics=avg,min,max&start_time=' + str(start_time)\
                      + '&end_time=' + str(end_time)
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)

    @test.attr(type="gate")
    def test_list_statistics_response_body_statistic_result_type(self):
        start_time = timeutils.iso8601_from_timestamp(self._start_timestamp
                                                      / 1000)
        end_time = timeutils.iso8601_from_timestamp(self._end_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=true&statistics=avg' \
                      '&start_time=' + str(start_time) + '&end_time=' \
                      + str(end_time)
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        self.assertEqual(200, resp.status)
        element = response_body['elements'][0]
        statistic = element['statistics']
        statistic_result_type = type(statistic[0][1])
        self.assertEqual(statistic_result_type, float)
