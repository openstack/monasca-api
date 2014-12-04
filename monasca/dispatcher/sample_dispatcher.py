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

import falcon
from oslo.config import cfg

from monasca.api import monasca_api_v2
from monasca.common import resource_api
from monasca.openstack.common import log

OPTS = [
    cfg.MultiStrOpt('id',
                    default=['sample'],
                    help='Multiple String configuration.'),
    cfg.StrOpt('prefix',
               default='monasca_',
               help='String configuration sample.'),
]
cfg.CONF.register_opts(OPTS, group='sample_dispatcher')


LOG = log.getLogger(__name__)


class SampleDispatcher(monasca_api_v2.V2API):
    """Monasca dispatcher sample class

    This class shows how to develop a dispatcher and how the configuration
    parameters should be defined and how these configuration parameters
    should be set in monasca.conf file.

    This class uses configuration parameters appear in sample_dispatcher
    section such as the following:

    [sample_dispatcher]
    id = 101
    id = 105
    id = 180
    prefix = sample__

    If the above section appears in file monasca.conf, these values will be
    loaded to cfg.CONF after the dispatcher gets loaded. The cfg.CONF should
    have the following values:

    cfg.CONF.sample_dispatcher.id = [101, 105, 180]
    cfg.CONF.sample_dispatcher.prefix = "sample__"
    """
    def __init__(self, global_conf):
        LOG.debug('initializing SampleDispatcher!')
        super(SampleDispatcher, self).__init__(global_conf)

        LOG.debug('SampleDispatcher conf entries: prefix')
        LOG.debug(global_conf.sample_dispatcher.prefix)
        LOG.debug('SampleDispatcher conf entries: id')
        LOG.debug(global_conf.sample_dispatcher.id)

    @resource_api.Restify('/v2.0/datapoints/', method='post')
    def do_post_metrics(self, req, res):
        LOG.debug('Getting the call at endpoint datapoints.')
        msg = req.stream.read()
        LOG.debug('The msg:', msg)
        res.status = getattr(falcon, 'HTTP_201')

    @resource_api.Restify('/v2.0/demopoints/', method='get')
    def do_get_metrics(self, req, res):
        LOG.debug('Getting the call at endpoint demopoints.')
        res.body = 'demo response'
        res.status = getattr(falcon, 'HTTP_200')
