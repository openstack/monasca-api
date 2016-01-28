# Copyright 2015,2016 Hewlett Packard Enterprise Development Company, L.P.
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

invalid_chars = "<>={}(),\"\\\\|;&"
restricted_chars = re.compile('[' + invalid_chars + ']')


def metric_name(name):
    assert isinstance(name, (str, unicode)), "Metric name must be a string"
    assert len(name) <= 255, "Metric name must be 255 characters or less"
    assert not restricted_chars.search(name), "Invalid characters in metric name " + name


def dimension_key(dkey):
    assert isinstance(dkey, (str, unicode)), "Dimension key must be a string"
    assert len(dkey) <= 255, "Dimension key must be 255 characters or less"
    assert not restricted_chars.search(dkey), "Invalid characters in dimension name " + dkey


def dimension_value(value):
    assert isinstance(value, (str, unicode)), "Dimension value must be a string"
    assert len(value) <= 255, "Dimension value must be 255 characters or less"
    assert not restricted_chars.search(value), "Invalid characters in dimension value " + value


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
