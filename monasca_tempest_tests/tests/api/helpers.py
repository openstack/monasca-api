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
import datetime
import time

import six.moves.urllib.parse as urlparse

from tempest.common.utils import data_utils

NUM_ALARM_DEFINITIONS = 2
NUM_MEASUREMENTS = 100


def create_metric(name='name-1',
                  dimensions={
                      'key-1': 'value-1',
                      'key-2': 'value-2'
                  },
                  timestamp=None,
                  value=0.0,
                  value_meta={
                      'key-1': 'value-1',
                      'key-2': 'value-2'
                  },
                  ):
    metric = {}
    if name is not None:
        metric['name'] = name
    if dimensions is not None:
        metric['dimensions'] = dimensions
    if timestamp is not None:
        metric['timestamp'] = timestamp
    else:
        metric['timestamp'] = int(time.time() * 1000)
    if value is not None:
        metric['value'] = value
    if value_meta is not None:
        metric['value_meta'] = value_meta
    return metric


def create_notification(name=data_utils.rand_name('notification-'),
                        type='EMAIL',
                        address='john.doe@domain.com',
                        period=0):
    notification = {}
    if name is not None:
        notification['name'] = name
    if type is not None:
        notification['type'] = type
    if address is not None:
        notification['address'] = address
    if period is not None:
        notification['period'] = period
    return notification


def create_alarm_definition(name=None,
                            description=None,
                            expression=None,
                            match_by=None,
                            severity=None,
                            alarm_actions=None,
                            ok_actions=None,
                            undetermined_actions=None):
    alarm_definition = {}
    if name is not None:
        alarm_definition['name'] = name
    if description is not None:
        alarm_definition['description'] = description
    if expression is not None:
        alarm_definition['expression'] = expression
    if match_by is not None:
        alarm_definition['match_by'] = match_by
    if severity is not None:
        alarm_definition['severity'] = severity
    if alarm_actions is not None:
        alarm_definition['alarm_actions'] = alarm_actions
    if ok_actions is not None:
        alarm_definition['ok_actions'] = ok_actions
    if undetermined_actions is not None:
        alarm_definition['undetermined_actions'] = undetermined_actions
    return alarm_definition


def delete_alarm_definitions(monasca_client):
    # Delete alarm definitions
    resp, response_body = monasca_client.list_alarm_definitions()
    elements = response_body['elements']
    if elements:
        for element in elements:
            alarm_def_id = element['id']
            monasca_client.delete_alarm_definition(alarm_def_id)


def timestamp_to_iso(timestamp):
    time_utc = datetime.datetime.utcfromtimestamp(timestamp / 1000.0)
    time_iso_base = time_utc.strftime("%Y-%m-%dT%H:%M:%S")
    time_iso_base += 'Z'
    return time_iso_base


def timestamp_to_iso_millis(timestamp):
    time_utc = datetime.datetime.utcfromtimestamp(timestamp / 1000.0)
    time_iso_base = time_utc.strftime("%Y-%m-%dT%H:%M:%S")
    time_iso_microsecond = time_utc.strftime(".%f")
    time_iso_millisecond = time_iso_base + time_iso_microsecond[0:4] + 'Z'
    return time_iso_millisecond


def get_query_param(uri, query_param_name):
    query_param_val = None
    parsed_uri = urlparse.urlparse(uri)
    for query_param in parsed_uri.query.split('&'):
        parsed_query_name, parsed_query_val = query_param.split('=', 1)
        if query_param_name == parsed_query_name:
            query_param_val = parsed_query_val
    return query_param_val
