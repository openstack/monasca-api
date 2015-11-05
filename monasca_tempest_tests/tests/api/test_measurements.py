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


class TestMeasurements(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestMeasurements, cls).resource_setup()

        start_timestamp = int(time.time() * 1000)
        metrics = []
        name = data_utils.rand_name()

        for i in xrange(NUM_MEASUREMENTS):
            metric = helpers.create_metric(
                name=name,
                timestamp=start_timestamp + i)
            metrics.append(metric)
        cls.monasca_client.create_metrics(metrics)
        cls._metrics = metrics
        cls._name = name

        # Create metric1 for test_list_measurements_with_dimensions
        key1 = data_utils.rand_name('key1')
        value1 = data_utils.rand_name('value1')
        start_timestamp1 = int(time.time() * 1000)
        name1 = data_utils.rand_name()
        metric1 = [
            helpers.create_metric(name=name1, timestamp=start_timestamp1,
                                  dimensions={key1: value1}, value=123)
        ]
        cls.monasca_client.create_metrics(metric1)
        cls._start_timestamp1 = start_timestamp1
        cls._name1 = name1
        cls._key1 = key1
        cls._value1 = value1

        # Create metric2 for test_list_measurements_with_offset_limit
        start_timestamp2 = int(time.time() * 1000)
        name2 = data_utils.rand_name()
        metric2 = [
            helpers.create_metric(name=name2, timestamp=start_timestamp2 + 0,
                                  dimensions={'key1': 'value-1',
                                              'key2': 'value-1'}),
            helpers.create_metric(name=name2, timestamp=start_timestamp2 + 1,
                                  dimensions={'key1': 'value-2',
                                              'key2': 'value-2'}),
            helpers.create_metric(name=name2, timestamp=start_timestamp2 + 2,
                                  dimensions={'key1': 'value-3',
                                              'key2': 'value-3'}),
            helpers.create_metric(name=name2, timestamp=start_timestamp2 + 3,
                                  dimensions={'key1': 'value-4',
                                              'key2': 'value-4'})
        ]
        cls.monasca_client.create_metrics(metric2)
        cls._name2 = name2
        cls._start_timestamp2 = start_timestamp2

        # Create metric3 for test_list_measurements_with_no_merge_metrics
        start_timestamp3 = int(time.time() * 1000)
        metric3 = helpers.create_metric(name=cls._name,
                                        timestamp=start_timestamp3,
                                        dimensions={'key-1': 'value-1',
                                                    'key-3': 'value-3'}
                                        )
        cls.monasca_client.create_metrics(metric3)
        cls._start_timestamp3 = start_timestamp3

        start_time = str(timeutils.iso8601_from_timestamp(
            start_timestamp / 1000))

        end_timestamp = int(time.time()) + 1
        end_time = timeutils.iso8601_from_timestamp(end_timestamp)

        queries = []
        queries.append('?name={}&start_time={}&end_time={}&merge_metrics=true'.
                       format(cls._name, start_time, end_time))
        queries.append('?name={}&start_time={}&end_time={}&merge_metrics=true'.
                       format(cls._name1, start_time, end_time))
        queries.append('?name={}&start_time={}&end_time={}&merge_metrics=true'.
                       format(cls._name2, start_time, end_time))

        for i in xrange(3):
            while True:
                responses = map(cls.monasca_client.list_measurements, queries)
                resp = responses[i][0]
                response_body = responses[i][1]
                if resp.status == 200 and 'elements' in response_body:
                    len_elements = len(response_body['elements'])
                    if len_elements > 0:
                        len_meas = len(
                            response_body['elements'][0]['measurements'])
                        if i == 0 and len_meas == NUM_MEASUREMENTS + 1:
                            break
                        elif i == 1 and len_meas == 1:
                            break
                        elif i == 2 and len_meas == 4:
                            break
                    else:
                        time.sleep(1)

        end_timestamp = int(time.time())
        end_time = timeutils.iso8601_from_timestamp(end_timestamp)
        cls._start_time = start_time
        cls._end_time = end_time

    @test.attr(type="gate")
    def test_list_measurements(self):
        query_parms = '?name=' + str(self._name) + '&merge_metrics=true' + \
                      '&start_time=' + str(self._start_time) + \
                      '&end_time=' + str(self._end_time)
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        self.assertEqual(200, resp.status)

        self.assertTrue(set(['links', 'elements']) == set(response_body))
        elements = response_body['elements']
        element = elements[0]
        self.assertTrue(set(['id', 'name', 'dimensions', 'columns',
                             'measurements']) == set(element))
        self.assertTrue(type(element['name']) is unicode)
        self.assertTrue(type(element['dimensions']) is dict)
        self.assertTrue(type(element['columns']) is list)
        self.assertTrue(type(element['measurements']) is list)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_measurements_with_no_start_time(self):
        query_parms = '?name=' + str(self._name)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_measurements, query_parms)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_measurements_with_no_name(self):
        query_parms = '?start_time=' + str(self._start_time) + '&end_time=' + \
                      str(self._end_time)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_measurements, query_parms)

    @test.attr(type="gate")
    def test_list_measurements_with_dimensions(self):
        start_time1 = timeutils.iso8601_from_timestamp(
            self._start_timestamp1 / 1000)
        end_time1 = timeutils.iso8601_from_timestamp(time.time())
        query_parms = '?name=' + self._name1 + '&start_time=' + \
                      str(start_time1) + '&end_time=' + str(end_time1) + \
                      '&dimensions=' + self._key1 + ':' + self._value1
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        value_new = response_body['elements'][0]['measurements'][0][1]
        self.assertEqual(200, resp.status)
        self.assertEqual(123, value_new)

    @test.attr(type="gate")
    def test_list_measurements_with_endtime(self):
        query_parms = '?name=' + str(self._name) + '&merge_metrics=true' \
                      '&start_time=' + str(self._start_time) + \
                      '&end_time=' + str(self._end_time) + \
                      '&dimensions=' + 'key-1:value-1,key-2:value-2'
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        self.assertEqual(200, resp.status)
        len_measurements = len(response_body['elements'][0]['measurements'])
        self.assertEqual(len_measurements, NUM_MEASUREMENTS)

    @test.attr(type="gate")
    def test_list_measurements_with_offset_limit(self):
        query_parms = '?name=' + str(self._name2)
        resp, response_body = self.monasca_client.list_metrics(query_parms)
        self.assertEqual(200, resp.status)

        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp2 / 1000)
        end_time = timeutils.iso8601_from_timestamp(time.time())
        query_parms = '?name=' + str(self._name2) + \
                      '&merge_metrics=true&start_time=' + str(start_time) + \
                      '&end_time=' + end_time
        resp, body = self.monasca_client.list_measurements(query_parms)
        self.assertEqual(200, resp.status)
        elements = body['elements'][0]['measurements']
        first_element = elements[0]
        last_element = elements[3]

        query_parms = '?name=' + str(self._name2) + \
                      '&merge_metrics=true&start_time=' + str(start_time) + \
                      '&end_time=' + end_time + '&limit=4'
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        self.assertEqual(200, resp.status)

        elements = response_body['elements'][0]['measurements']
        self.assertEqual(4, len(elements))

        self.assertEqual(first_element, elements[0])

        for limit in xrange(1, 5):
            next_element = elements[limit - 1]
            while True:
                query_parms = '?name=' + str(self._name2) + \
                              '&merge_metrics=true&start_time=' + \
                              str(start_time) + '&end_time=' + end_time + \
                              '&offset=' + str(next_element[0]) + '&limit=' + \
                              str(limit)
                resp, response_body = self.monasca_client.\
                    list_measurements(query_parms)
                self.assertEqual(200, resp.status)
                new_elements = response_body['elements'][0]['measurements']

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
    def test_list_measurements_with_merge_metrics(self):
        query_parms = '?name=' + str(self._name) + '&merge_metrics=true' + \
                      '&start_time=' + str(self._start_time) + \
                      '&end_time=' + str(self._end_time)
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        self.assertEqual(200, resp.status)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_measurements_with_name_exceeds_max_length(self):
        long_name = "x" * (constants.MAX_LIST_MEASUREMENTS_NAME_LENGTH + 1)
        query_parms = '?name=' + str(long_name) + '&merge_metrics=true' + \
                      '&start_time=' + str(self._start_time) + \
                      '&end_time=' + str(self._end_time)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_measurements, query_parms)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_measurements_with_no_merge_metrics(self):
        end_timestamp = int(time.time())
        end_time = timeutils.iso8601_from_timestamp(end_timestamp)
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp3 / 1000)
        query_parms = '?name=' + str(self._name) + '&start_time=' + str(
            start_time) + '&end_time=' + str(end_time)
        self.assertRaises(exceptions.Conflict,
                          self.monasca_client.list_measurements, query_parms)

    @test.attr(type="gate")
    def test_list_measurements_with_duplicate_query_param_merges_positive(
            self):
        end_timestamp = int(time.time())
        end_time = timeutils.iso8601_from_timestamp(end_timestamp)
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp3 / 1000)
        queries = []
        queries.append('?name={}&merge_metrics=true&start_time={}&end_time={'
                       '}&merge_metrics=true'.
                       format(self._name, start_time, end_time))
        queries.append('?name={}&merge_metrics=true&start_time={}&end_time={'
                       '}&merge_metrics=false'.
                       format(self._name, start_time, end_time))
        responses = map(self.monasca_client.list_measurements, queries)
        for i in xrange(2):
            resp = responses[i][0]
            self.assertEqual(200, resp.status)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_measurements_with_duplicate_query_param_merges_negative(
            self):
        end_timestamp = int(time.time())
        end_time = timeutils.iso8601_from_timestamp(end_timestamp)
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp3 / 1000)
        queries = []
        queries.append('?name={}&merge_metrics=false&start_time={}&end_time={'
                       '}&merge_metrics=true'.
                       format(self._name, start_time, end_time))
        queries.append('?name={}&merge_metrics=false&start_time={}&end_time={'
                       '}&merge_metrics=false'.
                       format(self._name, start_time, end_time))
        for i in xrange(2):
            self.assertRaises(exceptions.Conflict,
                              self.monasca_client.list_measurements,
                              queries[i])
