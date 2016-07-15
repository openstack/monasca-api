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

import six.moves.urllib.parse as urlparse

from monasca_tempest_tests.tests.api import base
from monasca_tempest_tests.tests.api import helpers
from tempest import test
from tempest.lib import exceptions

from monasca_tempest_tests import clients

class TestReadOnlyRole(base.BaseMonascaTest):

    @classmethod
    def resource_setup(cls):
        super(TestReadOnlyRole, cls).resource_setup()
        credentials = cls.cred_provider.get_creds_by_roles(
            ['monasca-read-only-user']).credentials
        cls.os = clients.Manager(credentials=credentials)
        cls.monasca_client = cls.os.monasca_client

    @classmethod
    def resource_cleanup(cls):
        super(TestReadOnlyRole, cls).resource_cleanup()

    @test.attr(type="gate")
    def test_list_alarms_success(self):
        resp, response_body = self.monasca_client.list_alarms()
        #
        # Validate the call succeeds with empty result (we didn't
        # create any alarms)
        #
        self.assertEqual(200, resp.status)
        self.assertEqual(0, len(response_body['elements']))
        self.assertTrue(response_body['links'][0]['href'].endswith('/v2.0/alarms'))

    @test.attr(type="gate")
    def test_list_metrics_success(self):
        resp, response_body = self.monasca_client.list_metrics()
        #
        # Validate the call succeeds with empty result (we didn't
        # create any metrics)
        #
        self.assertEqual(200, resp.status)
        self.assertEqual(0, len(response_body['elements']))
        self.assertTrue(response_body['links'][0]['href'].endswith('/v2.0/metrics'))

    @test.attr(type="gate")
    def test_list_alarm_definition_success(self):
        resp, response_body = self.monasca_client.list_alarm_definitions()
        #
        # Validate the call succeeds with empty result (we didn't
        # create any alarm definitions)
        #
        self.assertEqual(200, resp.status)
        self.assertEqual(0, len(response_body['elements']))
        self.assertTrue(response_body['links'][0]['href'].endswith('/v2.0/alarm-definitions'))

    @test.attr(type="gate")
    def test_list_notification_methods_success(self):
        resp, response_body = self.monasca_client.list_notification_methods()
        #
        # Validate the call succeeds with empty result (we didn't
        # create any notifications)
        #
        self.assertEqual(200, resp.status)
        self.assertEqual(0, len(response_body['elements']))
        self.assertTrue(response_body['links'][0]['href'].endswith('/v2.0/notification-methods'))

    @test.attr(type="gate")
    def test_list_alarm_count_success(self):
        resp, response_body = self.monasca_client.count_alarms()
        #
        # Validate the call succeeds with empty result (we didn't
        # create any alarms to count)
        #
        self.assertEqual(200, resp.status)
        self.assertEqual(0, response_body['counts'][0][0])
        self.assertTrue(response_body['links'][0]['href'].endswith('/v2.0/alarms/count'))

    @test.attr(type="gate")
    def test_list_alarm_state_history_success(self):
        resp, response_body = self.monasca_client.list_alarms_state_history()
        #
        # Validate the call succeeds with empty result (we didn't
        # create any alarms that have history)
        #
        self.assertEqual(200, resp.status)
        self.assertEqual(0, len(response_body['elements']))
        self.assertTrue(response_body['links'][0]['href'].endswith('/v2.0/alarms/state-history'))

    @test.attr(type="gate")
    def test_list_dimension_values_success(self):
        parms = '?dimension_name=foo'
        resp, response_body = self.monasca_client.list_dimension_values(parms)
        #
        # Validate the call succeeds with empty result (we didn't
        # create any metrics/dimensions)
        #
        url = '/v2.0/metrics/dimensions/names/values?dimension_name=foo'
        self.assertEqual(200, resp.status)
        self.assertEqual(0, len(response_body['elements'][0]['values']))
        self.assertTrue(response_body['links'][0]['href'].endswith(url))

    @test.attr(type="gate")
    def test_list_measurements_success(self):
        start_timestamp = int(time.time() * 1000)
        start_time = str(helpers.timestamp_to_iso(start_timestamp))
        parms = '?name=foo&start_time=' + start_time
        resp, response_body = self.monasca_client.list_measurements(parms)
        #
        # Validate the call succeeds with empty result (we didn't
        # create any metrics to get measurements for)
        #
        self.assertEqual(200, resp.status)
        self.assertEqual(0, len(response_body['elements']))
        self.assertTrue('/v2.0/metrics/measurements' in response_body['links'][0]['href'])


    @test.attr(type="gate")
    def test_list_statistics_success(self):
        start_timestamp = int(time.time() * 1000)
        start_time = str(helpers.timestamp_to_iso(start_timestamp))
        query_parms = '?name=foo&statistics=avg&start_time=' + start_time
        resp, response_body = self.monasca_client.list_statistics(
            query_parms)
        #
        # Validate the call succeeds with empty result (we didn't
        # create any metrics to get statistics for)
        #
        self.assertEqual(200, resp.status)
        self.assertEqual(0, len(response_body['elements']))
        self.assertTrue('/v2.0/metrics/statistics' in response_body['links'][0]['href'])

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_delete_alarms_fails(self):
        self.assertRaises(exceptions.Unauthorized,
                          self.monasca_client.delete_alarm, "foo")

    @test.attr(type='gate')
    @test.attr(type=['negative'])
    def test_create_metric_fails(self):
        self.assertRaises(exceptions.Unauthorized,
                          self.monasca_client.create_metrics,
                          None)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_alarm_definition_fails(self):
        self.assertRaises(exceptions.Unauthorized,
                          self.monasca_client.create_alarm_definitions,
                          None)

    @test.attr(type="gate")
    @test.attr(type=['negative'])
    def test_create_notification_fails(self):
        notif = helpers.create_notification()
        self.assertRaises(exceptions.Unauthorized,
                          self.monasca_client.create_notifications,
                          notif)
