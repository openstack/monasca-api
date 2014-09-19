# Copyright 2013 IBM Corp
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

from oslo.config import cfg
from stevedore import driver

from monasca.common import resource_api
from monasca.openstack.common import log

DISPATCHER_NAMESPACE = 'monasca.dispatcher'


cfg.CONF.register_opts(
    [cfg.StrOpt('driver', default='kafka',
                help='The name of the dispatcher for the api server'),
     ], group="dispatcher")

LOG = log.getLogger(__name__)


def api_app(conf):

    # Setup logs
    log_levels = (cfg.CONF.default_log_levels)
    cfg.set_defaults(log.log_opts, default_log_levels=log_levels)

    cfg.CONF(args=[], project='monasca')
    log.setup('monasca')

    # Create the application
    app = resource_api.ResourceAPI()

    # load the driver specified by dispatcher in the monasca.ini file
    manager = driver.DriverManager(namespace=DISPATCHER_NAMESPACE,
                                   name=cfg.CONF.dispatcher.driver,
                                   invoke_on_load=True,
                                   invoke_args=[conf])

    LOG.debug('Dispatcher driver %s is loaded.' % cfg.CONF.dispatcher.driver)

    # add the driver to the application
    app.add_route(None, manager.driver)

    LOG.debug('Dispatcher driver has been added to the routes!')
    return app
