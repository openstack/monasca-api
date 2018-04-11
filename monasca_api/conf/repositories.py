# Copyright 2014 IBM Corp.
# Copyright 2016-2017 FUJITSU LIMITED
# (C) Copyright 2016-2017 Hewlett Packard Enterprise Development LP
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from oslo_config import cfg

BASE_SQL_PATH = 'monasca_api.common.repositories.sqla.'

repositories_opts = [
    cfg.StrOpt(
        'metrics_driver',
        default='monasca_api.common.repositories.influxdb.metrics_repository:MetricsRepository',
        advanced=True,
        help='''
The repository driver to use for metrics
'''),
    cfg.StrOpt(
        'alarm_definitions_driver',
        default=BASE_SQL_PATH +
        'alarm_definitions_repository:AlarmDefinitionsRepository',
        advanced=True,
        help='''
The repository driver to use for alarm definitions
'''),
    cfg.StrOpt(
        'alarms_driver',
        default=BASE_SQL_PATH +
        'alarms_repository:AlarmsRepository',
        advanced=True,
        help='''
The repository driver to use for alarms
'''),
    cfg.StrOpt(
        'notifications_driver',
        default=BASE_SQL_PATH +
        'notifications_repository:NotificationsRepository',
        advanced=True,
        help='''
The repository driver to use for notifications
'''),
    cfg.StrOpt(
        'notification_method_type_driver',
        default=BASE_SQL_PATH +
        'notification_method_type_repository:NotificationMethodTypeRepository',
        advanced=True,
        help='''
The repository driver to use for notifications
''')]

repositories_group = cfg.OptGroup(name='repositories', title='repositories')


def register_opts(conf):
    conf.register_group(repositories_group)
    conf.register_opts(repositories_opts, repositories_group)


def list_opts():
    return repositories_group, repositories_opts
