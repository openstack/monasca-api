# Copyright 2017 FUJITSU LIMITED
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

kafka_check_opts = [
    cfg.StrOpt('kafka_url',
               required=True,
               help='Url to kafka server'),
    cfg.ListOpt('kafka_topics',
                required=True,
                default=['logs'],
                help='Verify existence of configured topics')
]
kafka_check_group = cfg.OptGroup(name='kafka_healthcheck',
                                 title='kafka_healthcheck')


def register_opts(conf):
    conf.register_group(kafka_check_group)
    conf.register_opts(kafka_check_opts, kafka_check_group)


def list_opts():
    return kafka_check_group, kafka_check_opts
