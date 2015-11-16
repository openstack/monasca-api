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

from tempest.common.utils import data_utils

NUM_ALARM_DEFINITIONS = 2
NUM_MEASUREMENTS = 100


def create_metric(name='name-1',
                  dimensions={
                      'key-1': 'value-1',
                      'key-2': 'value-2'
                  },
                  timestamp=time.time() * 1000,
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
    if value is not None:
        metric['value'] = value
    if value_meta is not None:
        metric['value_meta'] = value_meta
    return metric


def create_notification(name=data_utils.rand_name('notification-'),
                        type='EMAIL',
                        address='john.doe@domain.com'):
    notification = {}
    if name is not None:
        notification['name'] = name
    if type is not None:
        notification['type'] = type
    if address is not None:
        notification['address'] = address
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


def create_alarms_for_test_alarms(cls, num):
    for num in xrange(num):
        # create an alarm definition
        expression = "avg(name-1) > 0"
        name = data_utils.rand_name('name-1')
        alarm_definition = create_alarm_definition(
            name=name, expression=expression)
        resp, response_body = cls.monasca_client.create_alarm_definitions(
            alarm_definition)
        cls.assertEqual(201, resp.status)

    # create some metrics
    for i in xrange(180):
        metric = create_metric(name='name-1')
        cls.monasca_client.create_metrics(metric)
        time.sleep(1)
        resp, response_body = cls.monasca_client.list_alarms()
        elements = response_body['elements']
        if len(elements) >= num:
            break


def delete_alarm_definitions(self):
    # Delete alarm definitions
    resp, response_body = self.monasca_client.list_alarm_definitions()
    elements = response_body['elements']
    if elements:
        for element in elements:
            alarm_def_id = element['id']
            self.monasca_client.delete_alarm_definition(alarm_def_id)


def create_alarm_definitions_with_num(cls, expression):
    cls.rule = {'expression': 'mem_total_mb > 0'}
    alarm_def_id = []
    for i in xrange(NUM_ALARM_DEFINITIONS):
        alarm_definition = create_alarm_definition(
            name='alarm-definition-' + str(i),
            description=data_utils.rand_name('description'),
            expression=expression)
        resp, response_body = cls.monasca_client.create_alarm_definitions(
            alarm_definition)
        cls.assertEqual(201, resp.status)
        alarm_def_id.append(response_body['id'])
    return alarm_def_id


def create_alarm_definition_for_test_alarm_definition():
    # Create an alarm definition
    name = data_utils.rand_name('alarm_definition')
    expression = "max(cpu.system_perc) > 0"
    alarm_definition = create_alarm_definition(
        name=name,
        description="description",
        expression=expression)
    return alarm_definition


def create_metrics_for_test_alarms_match_by(cls, num, sub_expressions, list):
    # list=True when match_by is a set
    # sub_expressions=True when expression of the alarm has multiple
    # sub expressions
    # create some metrics
    for i in xrange(180):
        if list:
            metric1 = create_metric(
                name='cpu.idle_perc',
                dimensions={'service': 'monitoring', 'hostname': 'mini-mon',
                            'device': '/dev/sda1'})
            metric2 = create_metric(
                name='cpu.idle_perc',
                dimensions={'service': 'monitoring', 'hostname': 'devstack',
                            'device': '/dev/sda1'})
            metric3 = create_metric(
                name='cpu.idle_perc',
                dimensions={'service': 'monitoring', 'hostname': 'mini-mon',
                            'device': 'tmpfs'})
            metric4 = create_metric(
                name='cpu.idle_perc',
                dimensions={'service': 'monitoring', 'hostname': 'devstack',
                            'device': 'tmpfs'})
            cls.monasca_client.create_metrics(metric1)
            cls.monasca_client.create_metrics(metric2)
            cls.monasca_client.create_metrics(metric3)
            cls.monasca_client.create_metrics(metric4)
            time.sleep(1)
            resp, response_body = cls.monasca_client.list_alarms()
            elements = response_body['elements']
            if len(elements) >= num:
                break
        else:
            metric1 = create_metric(
                name='cpu.idle_perc',
                dimensions={'service': 'monitoring', 'hostname': 'mini-mon'})
            metric2 = create_metric(
                name='cpu.idle_perc',
                dimensions={'service': 'monitoring', 'hostname': 'devstack'})
            cls.monasca_client.create_metrics(metric1)
            cls.monasca_client.create_metrics(metric2)
            if sub_expressions:
                metric3 = create_metric(
                    name='cpu.user_perc',
                    dimensions={'service': 'monitoring',
                                'hostname': 'mini-mon'})
                metric4 = create_metric(
                    name='cpu.user_perc',
                    dimensions={'service': 'monitoring',
                                'hostname': 'devstack'})
                cls.monasca_client.create_metrics(metric3)
                cls.monasca_client.create_metrics(metric4)
            else:
                pass
            time.sleep(1)
            resp, response_body = cls.monasca_client.list_alarms()
            elements = response_body['elements']
            if len(elements) >= num:
                break


def timestamp_to_iso(timestamp):
    time_utc = datetime.datetime.utcfromtimestamp(timestamp / 1000.0)
    time_iso_base = time_utc.strftime("%Y-%m-%dT%H:%M:%S")
    time_iso_base += 'Z'
    return time_iso_base


def timestamp_to_iso_millis(timestamp):
    time_utc = datetime.datetime.utcfromtimestamp(timestamp / 1000.0)
    time_iso_base = time_utc.strftime("%Y-%m-%dT%H:%M")
    time_iso_microsecond = time_utc.strftime(".%f")
    time_iso_second = time_utc.strftime("%S")
    if float(time_iso_microsecond[0:4]) == 0.0:
        time_iso_millisecond = time_utc.strftime("%Y-%m-%dT%H:%M:%S") + 'Z'
    else:
        millisecond = str(int(time_iso_second[1]) +
                          float(time_iso_microsecond[0:4]))
        time_iso_new = time_iso_base + ':' + time_iso_second[0] + millisecond
        time_iso_millisecond = time_iso_new + 'Z'
    return time_iso_millisecond


def test_list_measurements_test_element(cls, element, test_key,
                                        test_value):
    cls.assertEqual(set(element),
                    set(['columns', 'dimensions', 'id', 'measurements',
                         'name']))
    cls.assertEqual(set(element['columns']),
                    set(['timestamp', 'value', 'value_meta']))
    cls.assertTrue(str(element['id']) is not None)
    if test_key is not None and test_value is not None:
        cls.assertEqual(str(element['dimensions'][test_key]), test_value)


def test_list_measurements_test_measurement(cls, measurement, test_metric,
                                            test_vm_key, test_vm_value):
    time_iso_millisecond = timestamp_to_iso_millis(test_metric['timestamp'])
    cls.assertEqual(str(measurement[0]), time_iso_millisecond)
    cls.assertEqual(measurement[1], test_metric['value'])
    if test_vm_key is not None and test_vm_value is not None:
        cls.assertEqual(str(measurement[2][test_vm_key]), test_vm_value)


def test_list_metrics_test_element(cls, element, test_key=None, test_value=None,
                                   test_name=None):
    cls.assertTrue(type(element['id']) is unicode)
    cls.assertTrue(type(element['name']) is unicode)
    cls.assertTrue(type(element['dimensions']) is dict)
    cls.assertEqual(set(element), set(['dimensions', 'id', 'name']))
    cls.assertTrue(str(element['id']) is not None)
    if test_key is not None and test_value is not None:
        cls.assertEqual(str(element['dimensions'][test_key]), test_value)
    if test_name is not None:
        cls.assertEqual(str(element['name']), test_name)
