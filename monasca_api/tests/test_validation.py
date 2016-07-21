# (C) Copyright 2015-2016 Hewlett Packard Enterprise Development Company LP
# Copyright 2015 Cray Inc. All Rights Reserved.
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

import unittest

import falcon
import mock

import monasca_api.v2.common.schemas.exceptions as schemas_exceptions
import monasca_api.v2.common.schemas.notifications_request_body_schema as schemas_notifications
import monasca_api.v2.common.validation as validation
import monasca_api.v2.reference.helpers as helpers


invalid_chars = "<>={}(),\"\\|;&"


class TestMetricNameValidation(unittest.TestCase):
    def test_valid_name(self):
        metric_name = "this.is_a.valid-name"
        validation.metric_name(metric_name)
        self.assertTrue(True)

    def test_nonstring_name(self):
        metric_name = 123456789
        self.assertRaises(AssertionError, validation.metric_name, metric_name)

    def test_long_name(self):
        metric_name = ("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
                       "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
                       "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
                       "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz")
        self.assertRaises(AssertionError, validation.metric_name, metric_name)

    def test_invalid_chars(self):
        for c in invalid_chars:
            metric_name = "this{}that".format(c)
            self.assertRaises(AssertionError, validation.metric_name, metric_name)


class TestDimensionValidation(unittest.TestCase):
    def test_valid_key(self):
        dim_key = "this.is_a.valid-key"
        validation.dimension_key(dim_key)
        self.assertTrue(True)

    def test_nonstring_key(self):
        dim_key = 123456
        self.assertRaises(AssertionError, validation.dimension_key, dim_key)

    def test_long_key(self):
        dim_key = ("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
                   "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
                   "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
                   "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz")
        self.assertRaises(AssertionError, validation.dimension_key, dim_key)

    def test_key_starts_with_underscore(self):
        dim_key = '_key'
        self.assertRaises(AssertionError, validation.dimension_key, dim_key)

    def test_invalid_chars_key(self):
        for c in invalid_chars:
            dim_key = "this{}that".format(c)
            self.assertRaises(AssertionError, validation.dimension_key, dim_key)

    def test_valid_value(self):
        dim_value = "this.is_a.valid-value"
        validation.dimension_value(dim_value)
        self.assertTrue(True)

    def test_nonstring_value(self):
        dim_value = None
        self.assertRaises(AssertionError, validation.dimension_value, dim_value)

    def test_long_value(self):
        dim_value = ("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
                     "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
                     "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
                     "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz")
        self.assertRaises(AssertionError, validation.dimension_value, dim_value)

    def test_invalid_chars_value(self):
        for c in invalid_chars:
            dim_value = "this{}that".format(c)
            self.assertRaises(AssertionError, validation.dimension_value, dim_value)


class TestValueMetaValidation(unittest.TestCase):
    def test_valid_name(self):
        value_meta_name = "this.is_a.valid-name"
        value_meta = {value_meta_name: 'value_meta_value'}
        validation.validate_value_meta(value_meta)
        self.assertTrue(True)

    def test_nonstring_name(self):
        value_meta_name = 123456
        value_meta = {value_meta_name: 'value_meta_value'}
        self.assertRaises(AssertionError, validation.validate_value_meta,
                          value_meta)

    def test_long_name(self):
        value_meta_name = "x" * 256
        value_meta = {value_meta_name: 'value_meta_value'}
        self.assertRaises(AssertionError, validation.validate_value_meta,
                          value_meta)

    def test_valid_value(self):
        value_meta_value = "this.is_a.valid-value"
        value_meta = {'value_meta_name': value_meta_value}
        validation.validate_value_meta(value_meta)
        self.assertTrue(True)

    def test_nonstring_value(self):
        value_meta_value = 123456
        value_meta = {'value_meta_name': value_meta_value}
        self.assertRaises(AssertionError, validation.validate_value_meta,
                          value_meta)

    def test_long_value_meta(self):
        value_meta_value = "x" * 2048
        value_meta = {'value_meta_name': value_meta_value}
        self.assertRaises(AssertionError, validation.validate_value_meta,
                          value_meta)


class TestRoleValidation(unittest.TestCase):

    def test_role_valid(self):
        req_roles = 'role0,rOlE1'
        authorized_roles = ['RolE1', 'Role2']

        req = mock.Mock()
        req.get_header.return_value = req_roles

        helpers.validate_authorization(req, authorized_roles)

    def test_role_invalid(self):
        req_roles = 'role2 ,role3'
        authorized_roles = ['role0', 'role1', 'role2']

        req = mock.Mock()
        req.get_header.return_value = req_roles

        self.assertRaises(
            falcon.HTTPUnauthorized,
            helpers.validate_authorization, req, authorized_roles)

    def test_empty_role_header(self):
        req_roles = ''
        authorized_roles = ['Role1', 'Role2']

        req = mock.Mock()
        req.get_header.return_value = req_roles

        self.assertRaises(
            falcon.HTTPUnauthorized,
            helpers.validate_authorization, req, authorized_roles)

    def test_no_role_header(self):
        req_roles = None
        authorized_roles = ['Role1', 'Role2']

        req = mock.Mock()
        req.get_header.return_value = req_roles

        self.assertRaises(
            falcon.HTTPUnauthorized,
            helpers.validate_authorization, req, authorized_roles)


class TestTimestampsValidation(unittest.TestCase):

    def test_valid_timestamps(self):
        start_time = '2015-01-01T00:00:00Z'
        end_time = '2015-01-01T00:00:01Z'
        start_timestamp = helpers._convert_time_string(start_time)
        end_timestamp = helpers._convert_time_string(end_time)

        try:
            helpers.validate_start_end_timestamps(start_timestamp,
                                                  end_timestamp)
        except:
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


class TestConvertTimeString(unittest.TestCase):

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


class TestNotificationValidation(unittest.TestCase):

    def test_validation_for_email(self):
        notification = {"name": "MyEmail", "type": "EMAIL", "address": "name@domain.com"}
        try:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        except schemas_exceptions.ValidationException:
            self.fail("shouldn't happen")

    def test_validation_exception_for_invalid_email_address(self):
        notification = {"name": "MyEmail", "type": "EMAIL", "address": "name@"}
        with self.assertRaises(schemas_exceptions.ValidationException) as ve:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        ex = ve.exception
        self.assertEqual("Address name@ is not of correct format", ex.message)

    def test_validation_exception_for_invalid_period_for_email(self):
        notification = {"name": "MyEmail", "type": "EMAIL", "address": "name@domain.com", "period": "60"}
        with self.assertRaises(schemas_exceptions.ValidationException) as ve:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        ex = ve.exception
        self.assertEqual("Period can only be set with webhooks", ex.message)

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
        with self.assertRaises(schemas_exceptions.ValidationException) as ve:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        ex = ve.exception
        self.assertEqual("Address //somedomain.com does not have URL scheme", ex.message)

    def test_validation_exception_for_webhook_no_netloc(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "http://"}
        with self.assertRaises(schemas_exceptions.ValidationException) as ve:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        ex = ve.exception
        self.assertEqual("Address http:// does not have network location", ex.message)

    def test_validation_exception_for_webhook_invalid_scheme(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "ftp://somedomain.com"}
        with self.assertRaises(schemas_exceptions.ValidationException) as ve:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        ex = ve.exception
        self.assertEqual("Address ftp://somedomain.com scheme is not in ['http', 'https']", ex.message)

    def test_validation_exception_for_webhook_invalid_period(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "//somedomain.com",
                        "period": "10"}
        with self.assertRaises(schemas_exceptions.ValidationException) as ve:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        ex = ve.exception
        self.assertEqual("10 is not a valid period", ex.message)

    def test_validation_for_pagerduty(self):
        notification = {"name": "MyPagerduty", "type": "PAGERDUTY",
                        "address": "nzH2LVRdMzun11HNC2oD"}
        try:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        except schemas_exceptions.ValidationException:
            self.fail("shouldn't happen")

    def test_validation_exception_for_invalid_period_for_pagerduty(self):
        notification = {"name": "MyPagerduty", "type": "PAGERDUTY",
                        "address": "nzH2LVRdMzun11HNC2oD", "period": 60}
        with self.assertRaises(schemas_exceptions.ValidationException) as ve:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        ex = ve.exception
        self.assertEqual("Period can only be set with webhooks", ex.message)

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
            schemas_notifications.parse_and_validate, notification, valid_periods)

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
        with self.assertRaises(schemas_exceptions.ValidationException) as ve:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        ex = ve.exception
        self.assertEqual("expected int for dictionary value @ data['period']", ex.message)

    def test_validation_exception_for_invalid_period_non_int(self):
        notification = {"name": "MyWebhook", "type": "WEBHOOK", "address": "//somedomain.com",
                        "period": "zero"}
        with self.assertRaises(schemas_exceptions.ValidationException) as ve:
            schemas_notifications.parse_and_validate(notification, valid_periods)
        ex = ve.exception
        self.assertEqual("Period zero must be a valid integer", ex.message)

    def test_validation_exception_for_missing_period(self):
        notification = {"name": "MyEmail", "type": "EMAIL", "address": "name@domain."}
        with self.assertRaises(schemas_exceptions.ValidationException) as ve:
            schemas_notifications.parse_and_validate(notification, valid_periods, require_all=True)
        ex = ve.exception
        self.assertEqual("Period is required", ex.message)
