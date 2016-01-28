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


class TestMetricsNames(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestMetricsNames, cls).resource_setup()
        name = data_utils.rand_name()
        key = data_utils.rand_name()
        value = data_utils.rand_name()
        cls._param = key + ':' + value
        metric = helpers.create_metric(name=name,
                                       dimensions={key: value})
        cls._test_metric = metric
        cls.monasca_client.create_metrics(metric)

        start_time = str(timeutils.iso8601_from_timestamp(
                         metric['timestamp'] / 1000.0))
        query_params = '?name=' + str(cls._test_metric['name']) +\
                       '&start_time=' + start_time

        for i in xrange(constants.MAX_RETRIES):
            resp, response_body = cls.monasca_client.list_metrics(
                query_params)
            elements = response_body['elements']
            for element in elements:
                if str(element['name']) == cls._test_metric['name']:
                    return
            time.sleep(constants.RETRY_WAIT_SECS)

        assert False, 'Unable to initialize metrics'

    @classmethod
    def resource_cleanup(cls):
        super(TestMetricsNames, cls).resource_cleanup()

    @test.attr(type='gate')
    def test_list_metrics_names(self):
        resp, response_body = self.monasca_client.list_metrics_names()
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) == set(response_body))
        if self._is_name_in_list(response_body):
            return
        self.fail('Metric name not found')

    @test.attr(type='gate')
    def test_list_metrics_names_with_dimensions(self):
        query_params = '?dimensions=' + self._param
        resp, response_body = self.monasca_client.list_metrics_names(
            query_params)
        self.assertEqual(200, resp.status)
        self.assertTrue(set(['links', 'elements']) == set(response_body))
        if self._is_name_in_list(response_body):
            return
        self.fail('Metric name not found')

    @test.attr(type='gate')
    def test_list_metrics_names_with_limit_offset(self):
        # Can not test list_metrics_names_with_limit_offset for now because
        #  list_metrics_names returns a list of metric names with no
        # duplicates. But the limit and offset are using the original list
        # with duplicates as reference.
        self.skipException('list_metrics_names_with_limit_offset')

    def _is_name_in_list(self, response_body):
        elements = response_body['elements']
        for element in elements:
            self.assertTrue(set(['id', 'name']) == set(element))
            if str(element['name']) == self._test_metric['name']:
                return True
        return False
