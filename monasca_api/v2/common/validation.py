# (C) Copyright 2015,2016 Hewlett Packard Enterprise Development Company LP
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

from monasca_api.v2.common.exceptions import HTTPUnprocessableEntityError

import json
import re

invalid_chars = "<>={}(),\"\\\\|;&"
restricted_chars = re.compile('[' + invalid_chars + ']')

VALID_ALARM_STATES = ["ALARM", "OK", "UNDETERMINED"]

VALID_ALARM_DEFINITION_SEVERITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]

VALUE_META_MAX_NUMBER = 16

VALUE_META_MAX_LENGTH = 2048

VALUE_META_NAME_MAX_LENGTH = 255

EMAIL_PATTERN = '^.+@.+$'


def metric_name(name):
    assert isinstance(name, (str, unicode)), "Metric name must be a string"
    assert len(name) <= 255, "Metric name must be 255 characters or less"
    assert len(name) >= 1, "Metric name cannot be empty"
    assert not restricted_chars.search(name), "Invalid characters in metric name " + name


def dimension_key(dkey):
    assert isinstance(dkey, (str, unicode)), "Dimension key must be a string"
    assert len(dkey) <= 255, "Dimension key must be 255 characters or less"
    assert len(dkey) >= 1, "Dimension key cannot be empty"
    assert dkey[0] != '_', "Dimension key cannot start with underscore (_)"
    assert not restricted_chars.search(dkey), "Invalid characters in dimension name " + dkey


def dimension_value(value):
    assert isinstance(value, (str, unicode)), "Dimension value must be a string"
    assert len(value) <= 255, "Dimension value must be 255 characters or less"
    assert len(value) >= 1, "Dimension value cannot be empty"
    assert not restricted_chars.search(value), "Invalid characters in dimension value " + value


def validate_alarm_state(state):
    if state not in VALID_ALARM_STATES:
        raise HTTPUnprocessableEntityError("Invalid State",
                                           "State {} must be one of {}".format(state.encode('utf8'),
                                                                               VALID_ALARM_STATES))


def validate_alarm_definition_severity(severity):
    if severity not in VALID_ALARM_DEFINITION_SEVERITIES:
        raise HTTPUnprocessableEntityError("Invalid Severity",
                                           "Severity {} must be one of {}".format(severity.encode('utf8'),
                                                                                  VALID_ALARM_DEFINITION_SEVERITIES))


def validate_severity_query(severity_str):
    severities = severity_str.split('|')
    for severity in severities:
        validate_alarm_definition_severity(severity)


def validate_sort_by(sort_by_list, allowed_sort_by):
    for sort_by_field in sort_by_list:
        sort_by_values = sort_by_field.split()
        if len(sort_by_values) > 2:
            raise HTTPUnprocessableEntityError("Unprocessable Entity",
                                               "Invalid sort_by {}".format(sort_by_field))
        if sort_by_values[0] not in allowed_sort_by:
            raise HTTPUnprocessableEntityError("Unprocessable Entity",
                                               "sort_by field {} must be one of [{}]".format(
                                                   sort_by_values[0],
                                                   ','.join(list(allowed_sort_by))))
        if len(sort_by_values) > 1 and sort_by_values[1] not in ['asc', 'desc']:
            raise HTTPUnprocessableEntityError("Unprocessable Entity",
                                               "sort_by value {} must be 'asc' or 'desc'".format(
                                                   sort_by_values[1]))


def validate_value_meta(value_meta):
    if not value_meta:
        return

    value_meta_string = json.dumps(value_meta)
    # entries
    assert len(value_meta) <= VALUE_META_MAX_NUMBER, "ValueMeta entries must be {} or less".format(
        VALUE_META_MAX_NUMBER)
    # total length
    assert len(value_meta_string) <= VALUE_META_MAX_LENGTH, \
        "ValueMeta name value combinations must be {} characters or less".format(
        VALUE_META_MAX_LENGTH)
    for name in value_meta:
        # name
        assert isinstance(name, (str, unicode)), "ValueMeta name must be a string"
        assert len(name) <= VALUE_META_NAME_MAX_LENGTH, "ValueMeta name must be {} characters or less".format(
            VALUE_META_NAME_MAX_LENGTH)
        assert len(name) >= 1, "ValueMeta name cannot be empty"
        # value
        assert isinstance(value_meta[name], (str, unicode)), "ValueMeta value must be a string"
        assert len(value_meta[name]) >= 1, "ValueMeta value cannot be empty"


def validate_state_query(state_str):
    if state_str not in VALID_ALARM_STATES:
        raise HTTPUnprocessableEntityError("Unprocessable Entity",
                                           "state {} must be one of 'ALARM','OK','UNDETERMINED'".format(state_str))


def validate_email_address(email):
    if re.match(EMAIL_PATTERN, email) is None:
        return False
    else:
        return True
