# (C) Copyright 2015-2017 Hewlett Packard Enterprise Development LP
# Copyright 2015 Cray Inc. All Rights Reserved.
# Copyright 2017 Fujitsu LIMITED
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

import falcon
from unittest import mock

from monasca_api.tests import base
import monasca_api.v2.common.exceptions as common_exceptions
import monasca_api.v2.common.schemas.alarm_definition_request_body_schema as schemas_alarm_defs
import monasca_api.v2.common.schemas.exceptions as schemas_exceptions
import monasca_api.v2.common.schemas.notifications_request_body_schema as schemas_notifications
import monasca_api.v2.common.validation as validation
import monasca_api.v2.reference.helpers as helpers


def mock_req_can(authorised_rule):
    if authorised_rule != 'authorized':
        raise Exception


class TestStateValidation(base.BaseTestCase):

    VALID_STATES = "OK", "ALARM", "UNDETERMINED"

    def test_valid_states(self):
        for state in self.VALID_STATES:
            validation.validate_alarm_state(state)

    def test_valid_states_lower_case(self):
        for state in self.VALID_STATES:
            validation.validate_alarm_state(state.lower())

    def test_invalid_state(self):
        self.assertRaises(common_exceptions.HTTPUnprocessableEntityError,
                          validation.validate_alarm_state, 'BOGUS')


class TestSeverityValidation(base.BaseTestCase):

    VALID_SEVERITIES = "LOW", "MEDIUM", "HIGH", "CRITICAL"

    def test_valid_severities(self):
        for state in self.VALID_SEVERITIES:
            validation.validate_severity_query(state)

    def test_valid_severities_lower_case(self):
        for state in self.VALID_SEVERITIES:
            validation.validate_severity_query(state.lower())

    def test_valid_multi_severities(self):
        validation.validate_severity_query('|'.join(self.VALID_SEVERITIES))

    def test_valid_multi_severities_lower_case(self):
        validation.validate_severity_query('|'.join(self.VALID_SEVERITIES)
                                           .lower())

    def test_invalid_state(self):
        self.assertRaises(common_exceptions.HTTPUnprocessableEntityError,
                          validation.validate_severity_query,
                          'BOGUS')
        self.assertRaises(common_exceptions.HTTPUnprocessableEntityError,
                          validation.validate_severity_query,
                          '|'.join([self.VALID_SEVERITIES[0], 'BOGUS']))


class TestRuleValidation(base.BaseApiTestCase):
    def test_rule_valid(self):
        req = mock.Mock()
        req.can = mock_req_can
        test_rules = ['Rule1', 'authorized']
        helpers.validate_authorization(req, test_rules)

    def test_rule_invalid(self):
        req = mock.Mock()
        req.can = mock_req_can
        test_rules = ['rule1', 'rule2']
        self.assertRaises(
            falcon.HTTPUnauthorized,
            helpers.validate_authorization, req, test_rules)


class TestTimestampsValidation(base.BaseTestCase):

    def test_valid_timestamps(self):
        start_time = '2015-01-01T00:00:00Z'
        end_time = '2015-01-01T00:00:01Z'
        start_timestamp = helpers._convert_time_string(start_time)
        end_timestamp = helpers._convert_time_string(end_time)

        try:
            helpers.validate_start_end_timestamps(start_timestamp,
                                                  end_timestamp)
        except Exception:
            self.fail("shouldn't happen")

    def test_same_timestamps(self):
        start_time = '2015-01-01T00:00:00Z'
        end_time = start_time
        start_timestamp = helpers._convert_time_string(start_time)
        end_timestamp = helpers._convert_time_string(end_time)

        self.assertRaises(
            falcon.HTTPBadRequest,
            helpers.validate_start_end_timestamps,
            start_timestamp, end_timestamp)

    def test_end_before_than_start(self):
        start_time = '2015-01-01T00:00:00Z'
        end_time = '2014-12-31T23:59:59Z'
        start_timestamp = helpers._convert_time_string(start_time)
        end_timestamp = helpers._convert_time_string(end_time)

        self.assertRaises(
            falcon.HTTPBadRequest,
            helpers.validate_start_end_timestamps,
            start_timestamp, end_timestamp)


class TestConvertTimeString(base.BaseTestCase):

    def test_valid_date_time_string(self):
        date_time_string = '2015-01-01T00:00:00Z'

        timestamp = helpers._convert_time_string(date_time_string)
        self.assertEqual(1420070400., timestamp)

    def test_valid_date_time_string_with_mills(self):
        date_time_string = '2015-01-01T00:00:00.025Z'

        timestamp = helpers._convert_time_string(date_time_string)
        self.assertEqual(1420070400.025, timestamp)

    def test_valid_date_time_string_with_timezone(self):
        date_time_string = '2015-01-01T09:00:00+09:00'

        timestamp = helpers._convert_time_string(date_time_string)
        self.assertEqual(1420070400., timestamp)

    def test_invalid_date_time_string(self):
        date_time_string = '2015-01-01T00:00:000Z'

        self.assertRaises(
            ValueError,
            helpers._convert_time_string, date_time_string)


valid_periods = [0, 60]


class TestNotificationValidation(base.BaseTestCase):

    def test_validation_for_email(self):
        notification = {"name": "MyEmail", "type": "EMAIL", "address": "name@domain.com"}
        try:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        except schemas_exceptions.ValidationException:
            self.fail("shouldn't happen")

    def test_validation_exception_for_invalid_email_address(self):
        notification = {"name": "MyEmail", "type": "EMAIL", "address": "name@"}
        ex = self.assertRaises(schemas_exceptions.ValidationException,
                               schemas_notifications.parse_and_validate,
                               notification, valid_periods)
        self.assertEqual("Address name@ is not of correct format", str(ex))

    def test_validation_for_email_non_zero_period(self):
        notification = {
            "name": "MyEmail",
            "type": "EMAIL",
            "address": "name@domain.com",
            "period": "60"}
        try:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        except schemas_exceptions.ValidationException:
            self.fail("shouldn't happen")

    def test_validation_for_webhook(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "http://somedomain.com"}
        try:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        except schemas_exceptions.ValidationException:
            self.fail("shouldn't happen")

    def test_validation_for_webhook_non_zero_period(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "http://somedomain.com",
                        "period": 60}
        try:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        except schemas_exceptions.ValidationException:
            self.fail("shouldn't happen")

    def test_validation_exception_for_webhook_no_scheme(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "//somedomain.com"}
        ex = self.assertRaises(schemas_exceptions.ValidationException,
                               schemas_notifications.parse_and_validate,
                               notification, valid_periods)
        self.assertEqual("Address //somedomain.com does not have URL scheme", str(ex))

    def test_validation_exception_for_webhook_no_netloc(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "http://"}
        ex = self.assertRaises(schemas_exceptions.ValidationException,
                               schemas_notifications.parse_and_validate,
                               notification, valid_periods)
        self.assertEqual("Address http:// does not have network location", str(ex))

    def test_validation_exception_for_webhook_invalid_scheme(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "ftp://somedomain.com"}
        ex = self.assertRaises(schemas_exceptions.ValidationException,
                               schemas_notifications.parse_and_validate,
                               notification, valid_periods)
        self.assertEqual("Address ftp://somedomain.com scheme is not in ['http', 'https']", str(ex))

    def test_validation_exception_for_webhook_invalid_period(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "//somedomain.com",
                        "period": "10"}
        ex = self.assertRaises(schemas_exceptions.ValidationException,
                               schemas_notifications.parse_and_validate,
                               notification, valid_periods)
        self.assertEqual("10 is not in the configured list of valid periods: [0, 60]", str(ex))

    def test_validation_for_pagerduty(self):
        notification = {"name": "MyPagerduty", "type": "PAGERDUTY",
                        "address": "nzH2LVRdMzun11HNC2oD"}
        try:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        except schemas_exceptions.ValidationException:
            self.fail("shouldn't happen")

    def test_validation_for_pagerduty_non_zero_period(self):
        notification = {"name": "MyPagerduty", "type": "PAGERDUTY",
                        "address": "nzH2LVRdMzun11HNC2oD", "period": 60}
        try:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        except schemas_exceptions.ValidationException:
            self.fail("shouldn't happen")

    def test_validation_for_max_name_address(self):
        name = "A" * 250
        self.assertEqual(250, len(name))
        address = "http://" + "A" * 502 + ".io"
        self.assertEqual(512, len(address))
        notification = {"name": name, "type": "WEBHOOK", "address": address}
        try:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        except schemas_exceptions.ValidationException:
            self.fail("shouldn't happen")

    def test_validation_exception_for_exceeded_name_length(self):
        name = "A" * 251
        self.assertEqual(251, len(name))
        notification = {"name": name, "type": "WEBHOOK", "address": "http://somedomain.com"}
        self.assertRaises(
            schemas_exceptions.ValidationException,
            schemas_notifications.parse_and_validate,
            notification, valid_periods)

    def test_validation_exception_for_exceeded_address_length(self):
        address = "http://" + "A" * 503 + ".io"
        self.assertEqual(513, len(address))
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": address}
        self.assertRaises(
            schemas_exceptions.ValidationException,
            schemas_notifications.parse_and_validate, notification, valid_periods)

    def test_validation_exception_for_invalid_period_float(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "//somedomain.com",
                        "period": 1.2}
        ex = self.assertRaises(schemas_exceptions.ValidationException,
                               schemas_notifications.parse_and_validate,
                               notification, valid_periods)
        self.assertEqual("expected int for dictionary value @ data['period']", str(ex))

    def test_validation_exception_for_invalid_period_non_int(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "//somedomain.com",
                        "period": "zero"}
        ex = self.assertRaises(schemas_exceptions.ValidationException,
                               schemas_notifications.parse_and_validate,
                               notification, valid_periods)
        self.assertEqual("Period zero must be a valid integer", str(ex))

    def test_validation_exception_for_missing_period(self):
        notification = {"name": "MyEmail", "type": "EMAIL", "address": "name@domain."}
        ex = self.assertRaises(schemas_exceptions.ValidationException,
                               schemas_notifications.parse_and_validate,
                               notification, valid_periods, require_all=True)
        self.assertEqual("Period is required", str(ex))


class TestAlarmDefinitionValidation(base.BaseTestCase):

    def setUp(self):
        super(TestAlarmDefinitionValidation, self).setUp()
        self.full_alarm_definition = (
            {"name": self._create_string_of_length(255),
             "expression": "min(cpu.idle_perc) < 10",
             "description": self._create_string_of_length(255),
             "severity": "MEDIUM",
             "match_by": ["hostname"],
             "ok_actions:": [self._create_string_of_length(50)],
             "undetermined_actions": [self._create_string_of_length(50)],
             "alarm_actions": [self._create_string_of_length(50)],
             "actions_enabled": True})

    def _create_string_of_length(self, length):
        s = ''
        for i in range(0, length):
            s += str(i % 10)
        return s

    def test_validation_good_minimum(self):
        alarm_definition = {"name": "MyAlarmDefinition", "expression": "min(cpu.idle_perc) < 10"}
        try:
            schemas_alarm_defs.validate(alarm_definition)
        except schemas_exceptions.ValidationException as e:
            self.fail("shouldn't happen: {}".format(str(e)))

    def test_validation_good_full(self):
        alarm_definition = self.full_alarm_definition
        try:
            schemas_alarm_defs.validate(alarm_definition)
        except schemas_exceptions.ValidationException as e:
            self.fail("shouldn't happen: {}".format(str(e)))

    def _ensure_fails_with_new_value(self, name, value):
        alarm_definition = self.full_alarm_definition.copy()
        alarm_definition[name] = value
        self._ensure_validation_fails(alarm_definition)

    def _ensure_validation_fails(self, alarm_definition):
        self.assertRaises(
            schemas_exceptions.ValidationException,
            schemas_alarm_defs.validate, alarm_definition)

    def _run_duplicate_action_test(self, actions_type):
        actions = ["a", "b", "a"]
        self._ensure_fails_with_new_value(actions_type, actions)

    def test_validation_too_long_name(self):
        self._ensure_fails_with_new_value("name",
                                          self._create_string_of_length(256))

    def test_validation_too_long_description(self):
        self._ensure_fails_with_new_value("description",
                                          self._create_string_of_length(256))

    def test_validation_duplicate_ok_actions(self):
        self._run_duplicate_action_test("ok_actions")

    def test_validation_duplicate_alarm_actions(self):
        self._run_duplicate_action_test("alarm_actions")

    def test_validation_duplicate_undetermined_actions(self):
        self._run_duplicate_action_test("undetermined_actions")

    def test_validation_too_many_actions(self):
        actions = [self._create_string_of_length(51)]
        self._ensure_fails_with_new_value("ok_actions", actions)

    def test_validation_invalid_severity(self):
        self._ensure_fails_with_new_value("severity", "BOGUS")

    def test_validation_invalid_match_by(self):
        self._ensure_fails_with_new_value("match_by", "NOT_A_LIST")

    def test_validation_invalid_actions_enabled(self):
        self._ensure_fails_with_new_value("actions_enabled", 42)
