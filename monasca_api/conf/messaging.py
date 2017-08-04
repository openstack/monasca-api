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

messaging_opts = [
    cfg.StrOpt('driver', help='''
The message queue driver to use
'''),
    cfg.StrOpt('metrics_message_format', default='reference',
               deprecated_for_removal=True,
               deprecated_since="2.1.0",
               deprecated_reason='''
Option is not used anywhere in the codebase
''',
               help='''
The type of metrics message format to publish to the message queue
'''),
    cfg.StrOpt('events_message_format', default='reference',
               deprecated_for_removal=True,
               deprecated_since='2.1.0',
               deprecated_reason='''
Option is not used anywhere in the codebase
''',
               help='''
The type of events message format to publish to the message queue
''')
]

messaging_group = cfg.OptGroup(name='messaging', title='messaging')


def register_opts(conf):
    conf.register_group(messaging_group)
    conf.register_opts(messaging_opts, messaging_group)


def list_opts():
    return messaging_group, messaging_opts
