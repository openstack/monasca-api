# Copyright 2014 IBM Corp.
# Copyright 2016-2017 FUJITSU LIMITED
# (C) Copyright 2016-2017 Hewlett Packard Enterprise Development LP
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

_DEFAULT_NOTIF_PERIODS = [0, 60]

global_opts = [
    cfg.StrOpt('region', sample_default='RegionOne',
               help='''
Region that API is running in
'''),
    cfg.ListOpt('valid_notification_periods', default=_DEFAULT_NOTIF_PERIODS,
                item_type=int,
                help='''
Valid periods for notification methods
'''),
    cfg.BoolOpt('enable_metrics_api', default='true',
                help='''
Enable Metrics api endpoints'''),
    cfg.BoolOpt('enable_logs_api', default='false',
                help='''
Enable Logs api endpoints''')
]


def register_opts(conf):
    conf.register_opts(global_opts)


def list_opts():
    return 'DEFAULT', global_opts
