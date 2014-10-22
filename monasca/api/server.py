# Copyright 2014 IBM Corp
#
# Author: Tong Li <litong01@us.ibm.com>
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

from stevedore import driver
import os
from monasca.common import resource_api
from monasca.openstack.common import log
from oslo.config import cfg
from oslo.config import types
from paste.deploy import loadapp
from wsgiref import simple_server

METRICS_DISPATCHER_NAMESPACE = 'monasca.metrics_dispatcher'
EVENTS_DISPATCHER_NAMESPACE = 'monasca.events_dispatcher'
TRANSFORMS_DISPATCHER_NAMESPACE = 'monasca.transforms_dispatcher'
NOTIFICATIONS_DISPATCHER_NAMESPACE = 'monasca.notifications_dispatcher'

LOG = log.getLogger(__name__)

global_opts = [
    cfg.StrOpt('region', help='Region that API is running in')
]

cfg.CONF.register_opts(global_opts)

security_opts = [
    cfg.ListOpt('default_authorized_roles', default=['admin'],
                help='Roles that are allowed full access to the API'),
    cfg.ListOpt('agent_authorized_roles', default=['agent'],
                help='Roles that are only allowed to POST to the API'),
    cfg.ListOpt('delegate_authorized_roles', default=['admin'],
                help='Roles that are allowed to POST metrics on behalf of another tenant')
]

security_group = cfg.OptGroup(name='security', title='security')
cfg.CONF.register_group(security_group)
cfg.CONF.register_opts(security_opts, security_group)

messaging_opts = [
    cfg.StrOpt('driver', default='kafka', help='The message queue driver to use'),
    cfg.StrOpt('metrics_message_format', default='reference',
               help='The type of metrics message format to publish to the message queue'),
    cfg.StrOpt('events_message_format', default='reference',
               help='The type of events message format to publish to the message queue')
]

messaging_group = cfg.OptGroup(name='messaging', title='messaging')
cfg.CONF.register_group(messaging_group)
cfg.CONF.register_opts(messaging_opts, messaging_group)

repositories_opts = [
    cfg.StrOpt('metrics_driver', default='influxdb_metrics_repo', help='The repository driver to use for metrics'),
    cfg.StrOpt('events_driver', default='fake_events_repo', help='The repository driver to use for events'),
    cfg.StrOpt('transforms_driver', default='mysql_transforms_repo', help='The repository driver to use for transforms'),
    cfg.StrOpt('notifications_driver', default='mysql_notifications_repo', help='The repository driver to use for notifications')
]

repositories_group = cfg.OptGroup(name='repositories', title='repositories')
cfg.CONF.register_group(repositories_group)
cfg.CONF.register_opts(repositories_opts, repositories_group)

dispatcher_opts = [
    cfg.StrOpt('driver', default='monasca.v2.reference.metrics:Metrics',
               help='The name of the dispatcher for the api server')
]

dispatcher_group = cfg.OptGroup(name='dispatcher', title='dispatcher')
cfg.CONF.register_group(dispatcher_group)
cfg.CONF.register_opts(dispatcher_opts, dispatcher_group)

kafka_opts = [
    cfg.StrOpt('uri',
               help='Address to kafka server. For example: '
                    'uri=192.168.1.191:9092'),
    cfg.StrOpt('metrics_topic',
               default='metrics',
               help='The topic that metrics will be published too.'),
    cfg.StrOpt('events_topic',
               default='raw-events',
               help='The topic that events will be published too.'),
    cfg.StrOpt('group',
               default='api',
               help='The group name that this service belongs to.'),
    cfg.IntOpt('wait_time',
               default=1,
               help='The wait time when no messages on kafka queue.'),
    cfg.IntOpt('ack_time',
               default=20,
               help='The ack time back to kafka.'),
    cfg.IntOpt('max_retry',
               default=3,
               help='The number of retry when there is a connection error.'),
    cfg.BoolOpt('auto_commit',
                default=False,
                help='If automatically commmit when consume messages.'),
    cfg.BoolOpt('async',
                default=True,
                help='The type of posting.'),
    cfg.BoolOpt('compact',
                default=True,
                help=('Specify if the message received should be parsed.'
                      'If True, message will not be parsed, otherwise '
                      'messages will be parsed.')),
    cfg.MultiOpt('partitions',
                 item_type=types.Integer(),
                 default=[0],
                 help='The sleep time when no messages on kafka queue.'),
    cfg.BoolOpt('drop_data',
                default=False,
                help=('Specify if received data should be simply dropped. '
                      'This parameter is only for testing purposes.')),
]

kafka_group = cfg.OptGroup(name='kafka', title='title')
cfg.CONF.register_group(kafka_group)
cfg.CONF.register_opts(kafka_opts, kafka_group)

influxdb_opts = [
    cfg.StrOpt('database_name'),
    cfg.StrOpt('ip_address'),
    cfg.StrOpt('port'),
    cfg.StrOpt('user'),
    cfg.StrOpt('password')
]

influxdb_group = cfg.OptGroup(name='influxdb', title='influxdb')
cfg.CONF.register_group(influxdb_group)
cfg.CONF.register_opts(influxdb_opts, influxdb_group)

mysql_opts = [
    cfg.StrOpt('database_name'),
    cfg.StrOpt('hostname'),
    cfg.StrOpt('username'),
    cfg.StrOpt('password')
]

mysql_group = cfg.OptGroup(name='mysql', title='mysql')
cfg.CONF.register_group(mysql_group)
cfg.CONF.register_opts(mysql_opts, mysql_group)


def api_app(conf):
    # Setup logs
    log_levels = (cfg.CONF.default_log_levels)
    cfg.set_defaults(log.log_opts, default_log_levels=log_levels)

    cfg.CONF(args=[], project='monasca')
    log.setup('monasca')

    # Create the application
    app = resource_api.ResourceAPI()

    # add the metrics resource
    app.add_resource('metrics', METRICS_DISPATCHER_NAMESPACE,
                     cfg.CONF.dispatcher.driver, [conf])

    # add the events resource
    app.add_resource('events', EVENTS_DISPATCHER_NAMESPACE,
                     cfg.CONF.dispatcher.driver, [conf])

    # add the transforms resource
    app.add_resource('transforms', TRANSFORMS_DISPATCHER_NAMESPACE,
                     cfg.CONF.dispatcher.driver, [conf])

    # add the notifications resource
    app.add_resource('notifications', NOTIFICATIONS_DISPATCHER_NAMESPACE,
                     cfg.CONF.dispatcher.driver, [conf])

    return app


if __name__ == '__main__':
    wsgi_app = loadapp('config:etc/monasca.ini', relative_to=os.getcwd())
    httpd = simple_server.make_server('127.0.0.1', 9000, wsgi_app)
    httpd.serve_forever()
