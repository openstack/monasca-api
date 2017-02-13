# (C) Copyright 2015-2017 Hewlett Packard Enterprise Development LP
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

import re

VALID_ALARM_STATES = ["ALARM", "OK", "UNDETERMINED"]

VALID_ALARM_DEFINITION_SEVERITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]

EMAIL_PATTERN = '^.+@.+$'


def validate_alarm_state(state):
    if state.upper() not in VALID_ALARM_STATES:
        raise HTTPUnprocessableEntityError("Invalid State",
                                           "State {} must be one of {}".format(state.encode('utf8'),
                                                                               VALID_ALARM_STATES))


def validate_alarm_definition_severity(severity):
    if severity.upper() not in VALID_ALARM_DEFINITION_SEVERITIES:
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


def validate_email_address(email):
    if re.match(EMAIL_PATTERN, email) is None:
        return False
    else:
        return True
