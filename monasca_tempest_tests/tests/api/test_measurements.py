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


class TestMeasurements(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestMeasurements, cls).resource_setup()

        start_timestamp = int(time.time() * 1000)
        end_timestamp = int(time.time() * 1000) + NUM_MEASUREMENTS * 1000
        metrics = []

        for i in xrange(NUM_MEASUREMENTS):
            metric = helpers.create_metric(
                name="name-1",
                timestamp=start_timestamp + i)
            metrics.append(metric)

        resp, response_body = cls.monasca_client.create_metrics(metrics)
        cls._start_timestamp = start_timestamp
        cls._end_timestamp = end_timestamp
        cls._metrics = metrics

    @test.attr(type="gate")
    def test_list_measurements(self):
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=true&start_time=' + str(
            start_time)
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
        query_parms = '?name=name-1'
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_measurements, query_parms)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_measurements_with_no_name(self):
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp / 1000)
        query_parms = '?start_time=' + str(start_time)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_measurements, query_parms)

    @test.attr(type="gate")
    def test_list_measurements_with_dimensions(self):
        key1 = data_utils.rand_name('key1')
        value1 = data_utils.rand_name('value1')
        start_timestamp = int(time.time() * 1000)
        name = data_utils.rand_name()
        metric = [
            helpers.create_metric(name=name, timestamp=start_timestamp,
                                  dimensions={key1: value1}, value=123)
        ]
        resp, response_body = self.monasca_client.create_metrics(metric)
        time.sleep(WAIT_TIME)

        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp / 1000)
        query_parms = '?name=' + name + '&start_time=' + str(
            start_time) + '&dimensions=' + key1 + ':' + value1
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        value_new = response_body['elements'][0]['measurements'][0][1]
        self.assertEqual(200, resp.status)
        self.assertEqual(123, value_new)

    @test.attr(type="gate")
    def test_list_measurements_with_endtime(self):
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp / 1000)
        end_time = timeutils.iso8601_from_timestamp(
            self._end_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=true&true&start_time=' + str(
            start_time) + '&end_time' + str(end_time)
        resp, body = self.monasca_client.list_measurements(query_parms)
        self.assertEqual(200, resp.status)
        len_measurements = len(body['elements'][0]['measurements'])
        self.assertEqual(len_measurements, NUM_MEASUREMENTS)

    @test.attr(type="gate")
    def test_list_measurements_with_offset_limit(self):
        start_timestamp = int(time.time() * 1000)
        name = data_utils.rand_name()
        metric = [
            helpers.create_metric(name=name, timestamp=start_timestamp + 0,
                                  dimensions={'key1': 'value-1',
                                              'key2': 'value-1'}),
            helpers.create_metric(name=name, timestamp=start_timestamp + 1,
                                  dimensions={'key1': 'value-2',
                                              'key2': 'value-2'}),
            helpers.create_metric(name=name, timestamp=start_timestamp + 2,
                                  dimensions={'key1': 'value-3',
                                              'key2': 'value-3'}),
            helpers.create_metric(name=name, timestamp=start_timestamp + 3,
                                  dimensions={'key1': 'value-4',
                                              'key2': 'value-4'})
        ]

        resp, response_body = self.monasca_client.create_metrics(metric)
        time.sleep(WAIT_TIME)

        query_parms = '?name=' + name
        resp, response_body = self.monasca_client.list_metrics(query_parms)
        self.assertEqual(200, resp.status)

        start_time = timeutils.iso8601_from_timestamp(
            start_timestamp / 1000)
        query_parms = '?name=' + name + '&merge_metrics=true&start_time=' + \
                      str(start_time)
        resp, body = self.monasca_client.list_measurements(query_parms)
        self.assertEqual(200, resp.status)
        elements = body['elements'][0]['measurements']
        first_element = elements[0]
        last_element = elements[3]

        query_parms = '?name=' + name + '&merge_metrics=true&start_time=' + \
                      str(start_time) + '&limit=4'
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        self.assertEqual(200, resp.status)

        elements = response_body['elements'][0]['measurements']
        self.assertEqual(4, len(elements))

        self.assertEqual(first_element, elements[0])

        for limit in xrange(1, 5):
            next_element = elements[limit - 1]
            while True:
                query_parms = '?name=' + name + \
                              '&merge_metrics=true&start_time=' + \
                              str(start_time) + '&offset=' + \
                              str(next_element[0]) + '&limit=' + \
                              str(limit)
                resp, response_body = self.monasca_client.list_measurements(
                    query_parms)
                self.assertEqual(200, resp.status)
                new_elements = response_body['elements'][0]['measurements']

                if len(new_elements) > limit - 1:
                    self.assertEqual(limit, len(new_elements))
                    next_element = new_elements[limit - 1]
                elif len(new_elements) > 0 and len(new_elements) <= limit - 1:
                    self.assertEqual(last_element, new_elements[0])
                    break
                else:
                    self.assertEqual(last_element, next_element)
                    break

    @test.attr(type="gate")
    def test_list_measurements_with_merge_metrics(self):
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=true&start_time=' + str(
            start_time)
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        self.assertEqual(200, resp.status)

    @test.attr(type="gate")
    def test_list_measurements_with_name_exceeds_max_length(self):
        long_name = "x" * (constants.MAX_LIST_MEASUREMENTS_NAME_LENGTH + 1)
        start_time = timeutils.iso8601_from_timestamp(self._start_timestamp
                                                      / 1000)
        query_parms = '?name=' + str(long_name) \
                      + '&merge_metrics=true&start_time=' + str(start_time)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.list_measurements, query_parms)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_measurements_with_no_merge_metrics(self):
        start_time = timeutils.iso8601_from_timestamp(
            self._start_timestamp / 1000)
        query_parms = '?name=name-1&merge_metrics=false&start_time=' + str(
            start_time)
        self.assertRaises(exceptions.Conflict,
                          self.monasca_client.list_measurements, query_parms)
