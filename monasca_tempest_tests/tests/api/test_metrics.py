# (C) Copyright 2014-2016 Hewlett Packard Enterprise Development LP
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

# TODO(RMH): Check if ' should be added in the list of INVALID_CHARS.
# TODO(RMH): test_create_metric_no_value, should return 422 if value not sent
import time

from six.moves import range as xrange

from tempest.common.utils import data_utils
from tempest.lib import exceptions
from tempest import test

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import constants
from monasca_tempest_tests.tests.api import helpers


class TestMetrics(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestMetrics, cls).resource_setup()

    @classmethod
    def resource_cleanup(cls):
        super(TestMetrics, cls).resource_cleanup()

    @test.attr(type='gate')
    def test_create_metric(self):
        name = data_utils.rand_name('name')
        key = data_utils.rand_name('key')
        value = data_utils.rand_name('value')
        timestamp = int(round(time.time() * 1000))
        time_iso = helpers.timestamp_to_iso(timestamp)
        end_timestamp = int(round((time.time() + 3600 * 24) * 1000))
        end_time_iso = helpers.timestamp_to_iso(end_timestamp)
        value_meta_key = data_utils.rand_name('value_meta_key')
        value_meta_value = data_utils.rand_name('value_meta_value')
        metric = helpers.create_metric(name=name,
                                       dimensions={key: value},
                                       timestamp=timestamp,
                                       value=1.23,
                                       value_meta={
                                           value_meta_key: value_meta_value
                                       })
        resp, response_body = self.monasca_client.create_metrics(metric)
        self.assertEqual(204, resp.status)
        query_param = '?name=' + name + '&start_time=' + time_iso + \
                      '&end_time=' + end_time_iso
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = self.monasca_client.\
                list_measurements(query_param)
            self.assertEqual(200, resp.status)
            elements = response_body['elements']
            for element in elements:
                if str(element['name']) == name:
                    self._verify_list_measurements_element(element, key, value)
                    measurement = element['measurements'][0]
                    self._verify_list_measurements_measurement(
                        measurement, metric, value_meta_key, value_meta_value)
                    return
            time.sleep(constants.RETRY_WAIT_SECS)
            if i == constants.MAX_RETRIES - 1:
                error_msg = "Failed test_create_metric: " \
                            "timeout on waiting for metrics: at least " \
                            "one metric is needed. Current number of " \
                            "metrics = 0"
                self.fail(error_msg)

    @test.attr(type='gate')
    def test_create_metrics(self):
        name = data_utils.rand_name('name')
        key = data_utils.rand_name('key')
        value = data_utils.rand_name('value')
        timestamp = int(round(time.time() * 1000))
        time_iso = helpers.timestamp_to_iso(timestamp)
        end_timestamp = int(round(timestamp + 3600 * 24 * 1000))
        end_time_iso = helpers.timestamp_to_iso(end_timestamp)
        value_meta_key1 = data_utils.rand_name('meta_key')
        value_meta_value1 = data_utils.rand_name('meta_value')
        value_meta_key2 = data_utils.rand_name('value_meta_key')
        value_meta_value2 = data_utils.rand_name('value_meta_value')
        metrics = [
            helpers.create_metric(name=name,
                                  dimensions={key: value},
                                  timestamp=timestamp,
                                  value=1.23,
                                  value_meta={
                                      value_meta_key1: value_meta_value1
                                  }),
            helpers.create_metric(name=name,
                                  dimensions={key: value},
                                  timestamp=timestamp + 6000,
                                  value=4.56,
                                  value_meta={
                                      value_meta_key2: value_meta_value2
                                  })
        ]
        resp, response_body = self.monasca_client.create_metrics(metrics)
        self.assertEqual(204, resp.status)
        query_param = '?name=' + name + '&start_time=' + str(time_iso) + \
                      '&end_time=' + str(end_time_iso)
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = self.monasca_client.\
                list_measurements(query_param)
            self.assertEqual(200, resp.status)
            elements = response_body['elements']
            for element in elements:
                if str(element['name']) == name \
                        and len(element['measurements']) == 2:
                    self._verify_list_measurements_element(element, key, value)
                    first_measurement = element['measurements'][0]
                    second_measurement = element['measurements'][1]
                    self._verify_list_measurements_measurement(
                        first_measurement, metrics[0], value_meta_key1,
                        value_meta_value1)
                    self._verify_list_measurements_measurement(
                        second_measurement, metrics[1], value_meta_key2,
                        value_meta_value2)
                    return
            time.sleep(constants.RETRY_WAIT_SECS)
            if i == constants.MAX_RETRIES - 1:
                error_msg = "Failed test_create_metrics: " \
                            "timeout on waiting for metrics: at least " \
                            "one metric is needed. Current number of " \
                            "metrics = 0"
                self.fail(error_msg)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_with_no_name(self):
        metric = helpers.create_metric(name=None)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_metrics,
                          metric)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_with_empty_name(self):
        metric = helpers.create_metric(name='')
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_metrics,
                          metric)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_with_empty_value_in_dimensions(self):
        name = data_utils.rand_name('name')
        metric = helpers.create_metric(name=name,
                                       dimensions={'key': ''})
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_metrics,
                          metric)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_with_empty_key_in_dimensions(self):
        name = data_utils.rand_name('name')
        metric = helpers.create_metric(name=name,
                                       dimensions={'': 'value'})
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_metrics,
                          metric)

    @test.attr(type='gate')
    def test_create_metric_with_no_dimensions(self):
        name = data_utils.rand_name('name')
        timestamp = int(round(time.time() * 1000))
        time_iso = helpers.timestamp_to_iso(timestamp)
        end_timestamp = int(round(timestamp + 3600 * 24 * 1000))
        end_time_iso = helpers.timestamp_to_iso(end_timestamp)
        value_meta_key = data_utils.rand_name('value_meta_key')
        value_meta_value = data_utils.rand_name('value_meta_value')
        metric = helpers.create_metric(name=name,
                                       dimensions=None,
                                       timestamp=timestamp,
                                       value=1.23,
                                       value_meta={
                                           value_meta_key: value_meta_value})
        resp, response_body = self.monasca_client.create_metrics(metric)
        self.assertEqual(204, resp.status)
        query_param = '?name=' + str(name) + '&start_time=' + str(time_iso) \
                      + '&end_time=' + str(end_time_iso)
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = self.monasca_client.\
                list_measurements(query_param)
            self.assertEqual(200, resp.status)
            elements = response_body['elements']
            for element in elements:
                if str(element['name']) == name:
                    self._verify_list_measurements_element(
                        element, test_key=None, test_value=None)
                    if len(element['measurements']) > 0:
                        measurement = element['measurements'][0]
                        self._verify_list_measurements_measurement(
                            measurement, metric, value_meta_key,
                            value_meta_value)
                        return
            time.sleep(constants.RETRY_WAIT_SECS)
            if i == constants.MAX_RETRIES - 1:
                error_msg = "Failed test_create_metric_with_no_dimensions: " \
                            "timeout on waiting for metrics: at least " \
                            "one metric is needed. Current number of " \
                            "metrics = 0"
                self.fail(error_msg)

    @test.attr(type='gate')
    def test_create_metric_with_colon_in_dimension_value(self):
        name = data_utils.rand_name('name')
        key = 'url'
        value = 'http://localhost:8070/v2.0'
        timestamp = int(round(time.time() * 1000))
        time_iso = helpers.timestamp_to_iso(timestamp)
        end_timestamp = int(round((time.time() + 3600 * 24) * 1000))
        end_time_iso = helpers.timestamp_to_iso(end_timestamp)
        metric = helpers.create_metric(name=name,
                                       dimensions={key: value})
        resp, response_body = self.monasca_client.create_metrics(metric)
        self.assertEqual(204, resp.status)
        query_param = '?name=' + name + '&start_time=' + time_iso + \
                      '&end_time=' + end_time_iso + \
                      '&dimensions=' + key + ':' + value
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = self.monasca_client. \
                list_measurements(query_param)
            self.assertEqual(200, resp.status)
            elements = response_body['elements']
            for element in elements:
                if str(element['name']) == name:
                    self._verify_list_measurements_element(element, key, value)
                    measurement = element['measurements'][0]
                    self._verify_list_measurements_measurement(
                        measurement, metric, None, None)
                    return
            time.sleep(constants.RETRY_WAIT_SECS)
            if i == constants.MAX_RETRIES - 1:
                error_msg = "Failed test_create_metric: " \
                            "timeout on waiting for metrics: at least " \
                            "one metric is needed. Current number of " \
                            "metrics = 0"
                self.fail(error_msg)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_with_no_timestamp(self):
        metric = helpers.create_metric()
        metric['timestamp'] = None
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_metrics,
                          metric)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_no_value(self):
        timestamp = int(round(time.time() * 1000))
        metric = helpers.create_metric(timestamp=timestamp,
                                       value=None)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_metrics,
                          metric)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_with_name_exceeds_max_length(self):
        long_name = "x" * (constants.MAX_METRIC_NAME_LENGTH + 1)
        metric = helpers.create_metric(long_name)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_metrics,
                          metric)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_with_invalid_chars_in_name(self):
        for invalid_char in constants.INVALID_CHARS:
            metric = helpers.create_metric(invalid_char)
            self.assertRaises(exceptions.UnprocessableEntity,
                              self.monasca_client.create_metrics,
                              metric)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_with_invalid_chars_in_dimensions(self):
        for invalid_char in constants.INVALID_CHARS:
            metric = helpers.create_metric('name-1', {'key-1': invalid_char})
            self.assertRaises(exceptions.UnprocessableEntity,
                              self.monasca_client.create_metrics,
                              metric)
        for invalid_char in constants.INVALID_CHARS:
            metric = helpers.create_metric('name-1', {invalid_char: 'value-1'})
            self.assertRaises(exceptions.UnprocessableEntity,
                              self.monasca_client.create_metrics,
                              metric)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_dimension_key_exceeds_max_length(self):
        long_key = "x" * (constants.MAX_DIMENSION_KEY_LENGTH + 1)
        metric = helpers.create_metric('name-1', {long_key: 'value-1'})
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_metrics,
                          metric)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_dimension_value_exceeds_max_length(self):
        long_value = "x" * (constants.MAX_DIMENSION_VALUE_LENGTH + 1)
        metric = helpers.create_metric('name-1', {'key-1': long_value})
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_metrics,
                          metric)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_with_value_meta_name_exceeds_max_length(self):
        long_value_meta_name = "x" * (constants.MAX_VALUE_META_NAME_LENGTH + 1)
        value_meta_dict = {long_value_meta_name: "value_meta_value"}
        metric = helpers.create_metric(name='name', value_meta=value_meta_dict)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_metrics,
                          metric)

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_with_value_meta_exceeds_max_length(self):
        value_meta_name = "x"
        long_value_meta_value = "y" * constants.MAX_VALUE_META_TOTAL_LENGTH
        value_meta_dict = {value_meta_name: long_value_meta_value}
        metric = helpers.create_metric(name='name', value_meta=value_meta_dict)
        self.assertRaises(exceptions.UnprocessableEntity,
                          self.monasca_client.create_metrics,
                          metric)

    @test.attr(type='gate')
    def test_list_metrics(self):
        resp, response_body = self.monasca_client.list_metrics()
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) == set(response_body))
        elements = response_body['elements']
        element = elements[0]
        self._verify_list_metrics_element(element, test_key=None,
                                          test_value=None, test_name=None)
        self.assertTrue(set(['id', 'name', 'dimensions']) == set(element))

    @test.attr(type='gate')
    def test_list_metrics_with_dimensions(self):
        name = data_utils.rand_name('name')
        key = data_utils.rand_name('key')
        value = data_utils.rand_name('value')
        metric = helpers.create_metric(name=name, dimensions={key: value})
        resp, response_body = self.monasca_client.create_metrics(metric)
        self.assertEqual(204, resp.status)
        query_param = '?dimensions=' + key + ':' + value
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = self.monasca_client.list_metrics(query_param)
            self.assertEqual(200, resp.status)
            elements = response_body['elements']
            for element in elements:
                if str(element['dimensions'][key]) == value:
                    self._verify_list_metrics_element(element, test_name=name)
                    return
            time.sleep(constants.RETRY_WAIT_SECS)
            if i == constants.MAX_RETRIES - 1:
                error_msg = "Failed test_list_metrics_with_dimensions: " \
                            "timeout on waiting for metrics: at least " \
                            "one metric is needed. Current number of " \
                            "metrics = 0"
                self.fail(error_msg)

    @test.attr(type='gate')
    def test_list_metrics_dimension_query_multi_value_with_diff_names(self):
        metrics, name, key_service, values = \
            self._create_metrics_with_different_dimensions(same_name=False)
        metric_dimensions = self._get_metric_dimensions(
            key_service, values, same_metric_name=False)
        query_param = '?dimensions=' + key_service + ':' + values[0] + '|' +\
                      values[1]
        self._verify_dimensions(query_param, metric_dimensions)

    @test.attr(type='gate')
    def test_list_metrics_dimension_query_no_value_with_diff_names(self):
        metrics, name, key_service, values = \
            self._create_metrics_with_different_dimensions(same_name=False)
        metric_dimensions = self._get_metric_dimensions(
            key_service, values, same_metric_name=False)
        query_param = '?dimensions=' + key_service
        self._verify_dimensions(query_param, metric_dimensions)

    @test.attr(type='gate')
    def test_list_metrics_dimension_query_multi_value_with_same_name(self):
        # Skip the test for now due to InfluxDB Inconsistency
        return
        metrics, name, key_service, values = \
            self._create_metrics_with_different_dimensions(same_name=True)
        metric_dimensions = self._get_metric_dimensions(
            key_service, values, same_metric_name=True)
        query_param = '?name=' + name + '&dimensions=' + key_service + ':' +\
                      values[0] + '|' + values[1]
        self._verify_dimensions(query_param, metric_dimensions)

    @test.attr(type='gate')
    def test_list_metrics_dimension_query_no_value_with_same_name(self):
        # Skip the test for now due to InfluxDB Inconsistency
        return
        metrics, name, key_service, values = \
            self._create_metrics_with_different_dimensions(same_name=True)
        metric_dimensions = self._get_metric_dimensions(
            key_service, values, same_metric_name=True)
        query_param = '?name=' + name + '&dimensions=' + key_service
        self._verify_dimensions(query_param, metric_dimensions)

    @test.attr(type='gate')
    def test_list_metrics_with_name(self):
        name = data_utils.rand_name('name')
        key = data_utils.rand_name('key')
        value = data_utils.rand_name('value')
        metric = helpers.create_metric(name=name,
                                       dimensions={key: value})
        resp, response_body = self.monasca_client.create_metrics(metric)
        self.assertEqual(204, resp.status)
        query_param = '?name=' + str(name)
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = self.monasca_client.list_metrics(query_param)
            self.assertEqual(200, resp.status)
            elements = response_body['elements']
            for element in elements:
                if str(element['name']) == name:
                    self._verify_list_metrics_element(element, test_key=key,
                                                      test_value=value)
                    return
            time.sleep(constants.RETRY_WAIT_SECS)
            if i == constants.MAX_RETRIES - 1:
                error_msg = "Failed test_list_metrics_with_name: " \
                            "timeout on waiting for metrics: at least " \
                            "one metric is needed. Current number of " \
                            "metrics = 0"
                self.fail(error_msg)

    @test.attr(type='gate')
    def test_list_metrics_with_tenant(self):
        name = data_utils.rand_name('name')
        key = data_utils.rand_name('key')
        value = data_utils.rand_name('value')
        tenant = self.tenants_client.create_tenant(
            name=data_utils.rand_name('test_tenant'))['tenant']
        # Delete the tenant at the end of the test
        self.addCleanup(self.tenants_client.delete_tenant, tenant['id'])
        metric = helpers.create_metric(name=name,
                                       dimensions={key: value})
        resp, response_body = self.monasca_client.create_metrics(
            metric, tenant_id=tenant['id'])
        self.assertEqual(204, resp.status)
        query_param = '?tenant_id=' + str(tenant['id'])
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = self.monasca_client.list_metrics(query_param)
            self.assertEqual(200, resp.status)
            elements = response_body['elements']
            for element in elements:
                if str(element['name']) == name:
                    self._verify_list_metrics_element(element, test_key=key,
                                                      test_value=value)
                    return
            time.sleep(constants.RETRY_WAIT_SECS)
            if i == constants.MAX_RETRIES - 1:
                error_msg = "Failed test_list_metrics_with_tenant: " \
                            "timeout on waiting for metrics: at least " \
                            "one metric is needed. Current number of " \
                            "metrics = 0"
                self.fail(error_msg)

    @test.attr(type='gate')
    def test_list_metrics_with_offset_limit(self):
        name = data_utils.rand_name()
        key1 = data_utils.rand_name()
        key2 = data_utils.rand_name()

        metrics = [
            helpers.create_metric(name=name, dimensions={
                key1: 'value-1', key2: 'value-1'}),
            helpers.create_metric(name=name, dimensions={
                key1: 'value-2', key2: 'value-2'}),
            helpers.create_metric(name=name, dimensions={
                key1: 'value-3', key2: 'value-3'}),
            helpers.create_metric(name=name, dimensions={
                key1: 'value-4', key2: 'value-4'})
        ]
        self.monasca_client.create_metrics(metrics)
        query_param = '?name=' + name
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = self.monasca_client.list_metrics(query_param)
            elements = response_body['elements']
            if elements and len(elements) == 4:
                break
            time.sleep(constants.RETRY_WAIT_SECS)
            if i == constants.MAX_RETRIES - 1:
                error_msg = ("Failed test_list_metrics_with_offset_limit: "
                             "timeout on waiting for metrics: 4 metrics "
                             "are needed. Current number of elements = "
                             "{}").format(len(elements))
                self.fail(error_msg)

        first_element = elements[0]
        query_parms = '?name=' + name + '&limit=4'
        resp, response_body = self.monasca_client.list_metrics(query_parms)
        self.assertEqual(200, resp.status)
        elements = response_body['elements']
        self.assertEqual(4, len(elements))
        self.assertEqual(first_element, elements[0])

        for metric_index in xrange(len(elements) - 1):
            metric = elements[metric_index]
            max_limit = 3 - metric_index

            for limit in xrange(1, max_limit):
                first_index = metric_index + 1
                last_index = first_index + limit
                expected_elements = elements[first_index:last_index]

                query_parms = '?name=' + name + '&offset=' + \
                              str(metric['id']) + '&limit=' + \
                              str(limit)
                resp, response_body = self.\
                    monasca_client.list_metrics(query_parms)
                self.assertEqual(200, resp.status)
                new_elements = response_body['elements']

                self.assertEqual(limit, len(new_elements))
                for i in xrange(len(expected_elements)):
                    self.assertEqual(expected_elements[i], new_elements[i])

    def _verify_list_measurements_element(self, element, test_key, test_value):
        self.assertEqual(set(element),
                         set(['columns', 'dimensions', 'id', 'measurements',
                             'name']))
        self.assertEqual(set(element['columns']),
                         set(['timestamp', 'value', 'value_meta']))
        self.assertTrue(str(element['id']) is not None)
        if test_key is not None and test_value is not None:
            self.assertEqual(str(element['dimensions'][test_key]), test_value)

    def _verify_list_measurements_measurement(self, measurement,
                                              test_metric, test_vm_key,
                                              test_vm_value):
        # Timestamps stored in influx sometimes are 1 millisecond different to
        # the value stored by the persister. Check if the timestamps are
        # equal in one millisecond range to pass the test.
        time_iso_millis = helpers.timestamp_to_iso_millis(
            test_metric['timestamp'] + 0)
        time_iso_millis_plus = helpers.timestamp_to_iso_millis(
            test_metric['timestamp'] + 1)
        time_iso_millis_minus = helpers.timestamp_to_iso_millis(
            test_metric['timestamp'] - 1)
        if str(measurement[0]) != time_iso_millis and str(measurement[0]) != \
                time_iso_millis_plus and str(measurement[0]) != \
                time_iso_millis_minus:
            error_msg = ("Mismatch Error: None of {}, {}, {} matches {}").\
                format(time_iso_millis, time_iso_millis_plus,
                       time_iso_millis_minus, str(measurement[0]))
            self.fail(error_msg)
        self.assertEqual(measurement[1], test_metric['value'])
        if test_vm_key is not None and test_vm_value is not None:
            self.assertEqual(str(measurement[2][test_vm_key]), test_vm_value)

    def _verify_list_metrics_element(self, element, test_key=None,
                                     test_value=None, test_name=None):
        self.assertTrue(type(element['id']) is unicode)
        self.assertTrue(type(element['name']) is unicode)
        self.assertTrue(type(element['dimensions']) is dict)
        self.assertEqual(set(element), set(['dimensions', 'id', 'name']))
        self.assertTrue(str(element['id']) is not None)
        if test_key is not None and test_value is not None:
            self.assertEqual(str(element['dimensions'][test_key]), test_value)
        if test_name is not None:
            self.assertEqual(str(element['name']), test_name)

    @test.attr(type='gate')
    def test_list_metrics_with_time_args(self):
        name = data_utils.rand_name('name')
        key = data_utils.rand_name('key')
        value_org = data_utils.rand_name('value')

        now = int(round(time.time() * 1000))
        #
        # Built start and end time args before and after the measurement.
        #
        start_iso = helpers.timestamp_to_iso(now - 1000)
        end_timestamp = int(round(now + 1000))
        end_iso = helpers.timestamp_to_iso(end_timestamp)

        metric = helpers.create_metric(name=name,
                                       dimensions={key: value_org},
                                       timestamp=now)

        self.monasca_client.create_metrics(metric)
        for timer in xrange(constants.MAX_RETRIES):
            query_parms = '?name=' + name + '&start_time=' + start_iso + '&end_time=' + end_iso
            resp, response_body = self.monasca_client.list_metrics(query_parms)
            self.assertEqual(200, resp.status)
            elements = response_body['elements']
            if elements:
                dimensions = elements[0]
                dimension = dimensions['dimensions']
                value = dimension[unicode(key)]
                self.assertEqual(value_org, str(value))
                break
            else:
                time.sleep(constants.RETRY_WAIT_SECS)
                if timer == constants.MAX_RETRIES - 1:
                    skip_msg = "Skipped test_list_metrics_with_time_args: " \
                               "timeout on waiting for metrics: at least one " \
                               "metric is needed. Current number of metrics " \
                               "= 0"
                    raise self.skipException(skip_msg)

    @staticmethod
    def _get_metric_dimensions(key_service, values, same_metric_name):
        if same_metric_name:
            metric_dimensions = [{key_service: values[0], 'key3': ''},
                                 {key_service: values[1], 'key3': ''},
                                 {key_service: '', 'key3': 'value3'}]
        else:
            metric_dimensions = [{key_service: values[0]},
                                 {key_service: values[1]},
                                 {'key3': 'value3'}]
        return metric_dimensions

    def _verify_dimensions(self, query_param, metric_dimensions):
        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = self.monasca_client.list_metrics(query_param)
            self.assertEqual(200, resp.status)
            elements = response_body['elements']
            if len(elements) == 2:
                dimension_sets = []
                for element in elements:
                    dimension_sets.append(element['dimensions'])
                self.assertIn(metric_dimensions[0], dimension_sets)
                self.assertIn(metric_dimensions[1], dimension_sets)
                self.assertNotIn(metric_dimensions[2], dimension_sets)
                return
            time.sleep(constants.RETRY_WAIT_SECS)
            if i == constants.MAX_RETRIES - 1:
                error_msg = "Timeout on waiting for metrics: at least " \
                            "2 metrics are needed. Current number of " \
                            "metrics = {}".format(len(elements))
                self.fail(error_msg)

    def _create_metrics_with_different_dimensions(self, same_name=True):
        name1 = data_utils.rand_name('name1')
        name2 = name1 if same_name else data_utils.rand_name('name2')
        name3 = name1 if same_name else data_utils.rand_name('name3')
        key_service = data_utils.rand_name('service')
        values = [data_utils.rand_name('value1'),
                  data_utils.rand_name('value2')]
        metrics = [helpers.create_metric(name1, {key_service: values[0]}),
                   helpers.create_metric(name2, {key_service: values[1]}),
                   helpers.create_metric(name3, {'key3': 'value3'})]
        resp, response_body = self.monasca_client.create_metrics(metrics)
        self.assertEqual(204, resp.status)
        return metrics, name1, key_service, values
