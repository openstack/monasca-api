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

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import constants
from monasca_tempest_tests.tests.api import helpers
from tempest.common.utils import data_utils
from tempest import test
from tempest.lib import exceptions

NUM_MEASUREMENTS = 50
ONE_SECOND = 1000


class TestMeasurements(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestMeasurements, cls).resource_setup()

        start_timestamp = int(time.time() * 1000)
        start_time = str(helpers.timestamp_to_iso(start_timestamp))
        metrics = []
        name1 = data_utils.rand_name()
        name2 = data_utils.rand_name()
        cls._names_list = [name1, name2]
        key = data_utils.rand_name('key')
        value = data_utils.rand_name('value')
        cls._key = key
        cls._value = value
        cls._start_timestamp = start_timestamp

        for i in xrange(NUM_MEASUREMENTS):
            metric = helpers.create_metric(
                name=name1,
                timestamp=start_timestamp + (i * 10),
                value=i)
            metrics.append(metric)
        cls.monasca_client.create_metrics(metrics)

        # Create metric2 for test_list_measurements_with_dimensions
        metric2 = helpers.create_metric(
            name=name1, timestamp=start_timestamp + ONE_SECOND * 2,
            dimensions={key: value}, value=NUM_MEASUREMENTS)
        cls.monasca_client.create_metrics(metric2)

        # Create metric3 for test_list_measurements_with_offset_limit
        metric3 = [
            helpers.create_metric(
                name=name2, timestamp=start_timestamp + ONE_SECOND * 3,
                dimensions={'key1': 'value1', 'key2': 'value1'}),
            helpers.create_metric(
                name=name2, timestamp=start_timestamp + ONE_SECOND * 3 + 10,
                dimensions={'key1': 'value2', 'key2': 'value2'}),
            helpers.create_metric(
                name=name2, timestamp=start_timestamp + ONE_SECOND * 3 + 20,
                dimensions={'key1': 'value3', 'key2': 'value3'}),
            helpers.create_metric(
                name=name2, timestamp=start_timestamp + ONE_SECOND * 3 + 30,
                dimensions={'key1': 'value4', 'key2': 'value4'})
        ]
        cls.monasca_client.create_metrics(metric3)

        # Create metric3 for test_list_measurements_with_no_merge_metrics
        metric4 = helpers.create_metric(
            name=name1, timestamp=start_timestamp + ONE_SECOND * 4,
            dimensions={'key-1': 'value-1'},
            value=NUM_MEASUREMENTS + 1)
        cls.monasca_client.create_metrics(metric4)

        end_time = str(helpers.timestamp_to_iso(
            start_timestamp + NUM_MEASUREMENTS + ONE_SECOND * 5))
        queries = []
        queries.append('?name={}&start_time={}&end_time={}&merge_metrics=true'.
                       format(name1, start_time, end_time))
        queries.append('?name={}&start_time={}&end_time={}&merge_metrics=true'.
                       format(name2, start_time, end_time))

        for timer in xrange(constants.MAX_RETRIES):
            responses = map(cls.monasca_client.list_measurements, queries)
            resp_first = responses[0][0]
            response_body_first = responses[0][1]
            resp_second = responses[1][0]
            response_body_second = responses[1][1]
            if resp_first.status == 200 and resp_second.status == 200 \
                    and len(response_body_first['elements']) == 1 \
                    and len(response_body_second['elements']) == 1:
                len_meas_first = len(
                    response_body_first['elements'][0]['measurements'])
                len_meas_second = len(
                    response_body_second['elements'][0]['measurements'])
                if len_meas_first == NUM_MEASUREMENTS + 2 \
                        and len_meas_second == 4:
                    break
                else:
                    time.sleep(constants.RETRY_WAIT_SECS)
            else:
                time.sleep(constants.RETRY_WAIT_SECS)

        cls._start_time = start_time
        cls._end_time = end_time

    @test.attr(type="gate")
    def test_list_measurements(self):
        query_parms = '?name=' + str(self._names_list[0]) + \
                      '&merge_metrics=true' + \
                      '&start_time=' + str(self._start_time) + \
                      '&end_time=' + str(self._end_time)
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        self._verify_list_measurements(resp, response_body)
        elements = response_body['elements']
        self._verify_list_measurements_elements(
            elements=elements, test_key=None, test_value=None)
        measurements = elements[0]['measurements']
        self._verify_list_measurements_meas_len(
            measurements, test_len=NUM_MEASUREMENTS + 2)
        i = 0
        for measurement in measurements:
            self._verify_list_measurements_measurement(measurement, i)
            i += 1

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_measurements_with_no_start_time(self):
        query_parms = '?name=' + str(self._names_list[0])
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
        query_parms = '?name=' + self._names_list[0] + '&start_time=' + \
                      str(self._start_time) + '&end_time=' + \
                      str(self._end_time) + '&dimensions=' + self._key + ':' \
                      + self._value
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        self._verify_list_measurements(resp, response_body)
        elements = response_body['elements']
        self._verify_list_measurements_elements(
            elements=elements, test_key=None, test_value=None)
        measurements = elements[0]['measurements']
        self._verify_list_measurements_meas_len(measurements, 1)
        measurement = measurements[0]
        self._verify_list_measurements_measurement(
            measurement=measurement, test_value=NUM_MEASUREMENTS)

    @test.attr(type="gate")
    def test_list_measurements_with_endtime(self):
        time_iso = helpers.timestamp_to_iso(
            self._start_timestamp + ONE_SECOND * 2)
        query_parms = '?name=' + str(self._names_list[0]) + \
                      '&merge_metrics=true' \
                      '&start_time=' + str(self._start_time) + \
                      '&end_time=' + str(time_iso)
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        self._verify_list_measurements(resp, response_body)
        elements = response_body['elements']
        self._verify_list_measurements_elements(elements=elements,
                                                test_key=None, test_value=None)
        measurements = elements[0]['measurements']
        self._verify_list_measurements_meas_len(measurements=measurements,
                                                test_len=NUM_MEASUREMENTS)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_measurements_with_endtime_equals_starttime(self):
        query_parms = '?name=' + str(self._names_list[0]) + \
                      '&merge_metrics=true' \
                      '&start_time=' + str(self._start_time) + \
                      '&end_time=' + str(self._start_time)
        self.assertRaises(exceptions.BadRequest,
                          self.monasca_client.list_measurements, query_parms)


    @test.attr(type="gate")
    def test_list_measurements_with_offset_limit(self):
        query_parms = '?name=' + str(self._names_list[1]) + \
                      '&merge_metrics=true&start_time=' + self._start_time + \
                      '&end_time=' + self._end_time
        resp, response_body = self.monasca_client.list_measurements(
            query_parms)
        self._verify_list_measurements(resp, response_body)
        elements = response_body['elements']
        self._verify_list_measurements_elements(elements=elements,
                                                test_key=None, test_value=None)
        measurements = elements[0]['measurements']
        self._verify_list_measurements_meas_len(measurements=measurements,
                                                test_len=4)

        for measurement_index in xrange(1, len(measurements) - 3):
            max_limit = len(measurements) - measurement_index

            # Get first offset from api
            query_parms = '?name=' + str(self._names_list[1]) + \
                          '&merge_metrics=true&start_time=' + measurements[measurement_index - 1][0] + \
                          '&end_time=' + self._end_time + \
                          '&limit=1'
            resp, response_body = self.monasca_client.list_measurements(query_parms)
            for link in response_body['links']:
                if link['rel'] == 'next':
                    next_link = link['href']
            if not next_link:
                self.fail("No next link returned with query parameters: {}".formet(query_parms))
            offset = helpers.get_query_param(next_link, "offset")

            first_index = measurement_index + 1

            for limit in xrange(1, max_limit):
                last_index = measurement_index + limit + 1
                expected_measurements = measurements[first_index:last_index]

                query_parms = '?name=' + str(self._names_list[1]) + \
                              '&merge_metrics=true&start_time=' + \
                              self._start_time + '&end_time=' + \
                              self._end_time + '&limit=' + str(limit) + \
                              '&offset=' + str(offset)

                resp, response_body = self.monasca_client.list_measurements(
                    query_parms)
                self._verify_list_measurements(resp, response_body)
                new_measurements = response_body['elements'][0]['measurements']

                self.assertEqual(limit, len(new_measurements))
                for i in xrange(len(expected_measurements)):
                    self.assertEqual(expected_measurements[i],
                                     new_measurements[i])

    @test.attr(type="gate")
    def test_list_measurements_with_merge_metrics(self):
        query_parms = '?name=' + str(self._names_list[0]) + \
                      '&merge_metrics=true' + \
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
        query_parms = '?name=' + str(self._names_list[0]) + \
                      '&start_time=' + str(self._start_time) + '&end_time=' \
                      + str(self._end_time)
        self.assertRaises(exceptions.Conflict,
                          self.monasca_client.list_measurements, query_parms)

    @test.attr(type="gate")
    def test_list_measurements_with_duplicate_query_param_merges_positive(
            self):
        queries = []
        queries.append('?name={}&merge_metrics=true&start_time={}&end_time={'
                       '}&merge_metrics=true'.
                       format(self._names_list[0], self._start_time,
                              self._end_time))
        queries.append('?name={}&merge_metrics=true&start_time={}&end_time={'
                       '}&merge_metrics=false'.
                       format(self._names_list[0], self._start_time,
                              self._end_time))
        responses = map(self.monasca_client.list_measurements, queries)
        for i in xrange(2):
            self._verify_list_measurements(responses[i][0], responses[i][1])

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_list_measurements_with_duplicate_query_param_merges_negative(
            self):
        queries = []
        queries.append('?name={}&merge_metrics=false&start_time={}&end_time={'
                       '}&merge_metrics=true'.
                       format(self._names_list[0], self._start_time,
                              self._end_time))
        queries.append('?name={}&merge_metrics=false&start_time={}&end_time={'
                       '}&merge_metrics=false'.
                       format(self._names_list[0], self._start_time,
                              self._end_time))
        for i in xrange(2):
            self.assertRaises(exceptions.Conflict,
                              self.monasca_client.list_measurements,
                              queries[i])

    def _verify_list_measurements_measurement(self, measurement, test_value):
        self.assertEqual(test_value, float(measurement[1]))

    def _verify_list_measurements(self, resp, response_body):
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) == set(response_body))

    def _verify_list_measurements_elements(self, elements, test_key,
                                           test_value):
        if elements:
            element = elements[0]
            self.assertEqual(set(element),
                             set(['columns', 'dimensions', 'id',
                                  'measurements', 'name']))
            self.assertTrue(type(element['name']) is unicode)
            self.assertTrue(type(element['dimensions']) is dict)
            self.assertTrue(type(element['columns']) is list)
            self.assertTrue(type(element['measurements']) is list)
            self.assertEqual(set(element['columns']),
                             set(['timestamp', 'value', 'value_meta']))
            self.assertTrue(str(element['id']) is not None)
            if test_key is not None and test_value is not None:
                self.assertEqual(str(element['dimensions'][test_key]),
                                 test_value)
        else:
            error_msg = "Failed: at least one element is needed. " \
                        "Number of element = 0."
            self.fail(error_msg)

    def _verify_list_measurements_meas_len(self, measurements, test_len):
        if measurements:
            len_measurements = len(measurements)
            self.assertEqual(len_measurements, test_len)
        else:
            error_msg = "Failed: one specific measurement is needed. " \
                        "Number of measurements = 0"
            self.fail(error_msg)
