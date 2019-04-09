# Copyright 2014 IBM Corp
# (C) Copyright 2015,2016 Hewlett Packard Enterprise Development LP
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

from oslo_config import cfg

dispatcher_opts = [
    cfg.StrOpt('versions',
               default='monasca_api.v2.reference.versions:Versions',
               help='Versions controller'),
    cfg.StrOpt('version_2_0',
               default='monasca_api.v2.reference.version_2_0:Version2',
               help='Version 2.0 controller'),
    cfg.StrOpt('metrics',
               default='monasca_api.v2.reference.metrics:Metrics',
               help='Metrics controller'),
    cfg.StrOpt('metrics_measurements',
               default='monasca_api.v2.reference.metrics:MetricsMeasurements',
               help='Metrics measurements controller'),
    cfg.StrOpt('metrics_statistics',
               default='monasca_api.v2.reference.metrics:MetricsStatistics',
               help='Metrics statistics controller'),
    cfg.StrOpt('metrics_names',
               default='monasca_api.v2.reference.metrics:MetricsNames',
               help='Metrics names controller'),
    cfg.StrOpt('alarm_definitions',
               default='monasca_api.v2.reference.'
                       'alarm_definitions:AlarmDefinitions',
               help='Alarm definitions controller'),
    cfg.StrOpt('alarms',
               default='monasca_api.v2.reference.alarms:Alarms',
               help='Alarms controller'),
    cfg.StrOpt('alarms_count',
               default='monasca_api.v2.reference.alarms:AlarmsCount',
               help='Alarms Count controller'),
    cfg.StrOpt('alarms_state_history',
               default='monasca_api.v2.reference.alarms:AlarmsStateHistory',
               help='Alarms state history controller'),
    cfg.StrOpt('notification_methods',
               default='monasca_api.v2.reference.notifications:Notifications',
               help='Notification Methods controller'),
    cfg.StrOpt('dimension_values',
               default='monasca_api.v2.reference.metrics:DimensionValues',
               help='Dimension Values controller'),
    cfg.StrOpt('dimension_names',
               default='monasca_api.v2.reference.metrics:DimensionNames',
               help='Dimension Names controller'),
    cfg.StrOpt('notification_method_types',
               default='monasca_api.v2.reference.'
                       'notificationstype:NotificationsType',
               help='Notifications Type Methods controller'),
    cfg.StrOpt('logs',
               default='monasca_api.v2.reference.logs:Logs',
               help='Logs controller'),
    cfg.StrOpt('healthchecks',
               default='monasca_api.healthchecks:HealthChecks',
               help='Health checks endpoint controller')
]

dispatcher_group = cfg.OptGroup(name='dispatcher', title='dispatcher')


def register_opts(conf):
    conf.register_group(dispatcher_group)
    conf.register_opts(dispatcher_opts, dispatcher_group)


def list_opts():
    return dispatcher_group, dispatcher_opts
