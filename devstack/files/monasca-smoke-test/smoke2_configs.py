# -*- encoding: utf-8 -*-
#
# (C) Copyright 2015 Hewlett Packard Enterprise Development Company LP
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""configurations for smoke2 test"""

test_config = {
    'default': {   # the default configuration,
                   # simple test of each component of monasca-vagrant
        'kafka': {
            'topics': [
                'metrics', 'events', 'raw-events', 'transformed-events',
                'stream-definitions', 'transform-definitions',
                'alarm-state-transitions', 'alarm-notifications',
                'retry-notifications'
            ]
        },
        'mysql_schema': [
            'alarm', 'alarm_action', 'alarm_definition', 'alarm_metric',
            'metric_definition', 'metric_definition_dimensions',
            'metric_dimension', 'notification_method', 'sub_alarm',
            'sub_alarm_definition', 'sub_alarm_definition_dimension',
            'alarm_state', 'alarm_definition_severity', 'notification_method_type'
        ],
        'arg_defaults': {
            'dbtype': "influxdb",
            'kafka': "127.0.0.1:9092",
            'zoo': "127.0.0.1:2181",
            'mysql': "127.0.0.1",
            'monapi': "127.0.0.1",

        },

        'check': {
            'expected_processes': [
                'apache-storm', 'monasca-api', 'monasca-statsd',
                'monasca-collector', 'monasca-forwarder',
                'monasca-notification', 'monasca-persister',
            ]
        },
    },
    'keystone': {
        'user': "mini-mon",
        'pass': "password",
        'host': "127.0.0.1"
    },
    'storm': "127.0.0.1",
    'mysql': {
        'user': "monapi",
        'pass': "password"
    },
    'influx': {
        'user': "mon_api",
        'pass': "password",
        'node': "http://127.0.0.1:8086"
    },
    'help': {
        'test': 'wiki link for help with specific process'
    }
}
