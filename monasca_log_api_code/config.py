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

import sys
from monasca_log_api import conf
from monasca_log_api import version
from oslo_config import cfg
from oslo_log import log
from oslo_policy import opts as policy_opts


CONF = conf.CONF
LOG = log.getLogger(__name__)

_CONF_LOADED = False
_GUNICORN_MARKER = 'gunicorn'


def _is_running_under_gunicorn():
    """Evaluates if api runs under gunicorn"""
    content = filter(lambda x: x != sys.executable and _GUNICORN_MARKER in x,
                     sys.argv or [])
    return len(list(content) if not isinstance(content, list) else content) > 0


def get_config_files():
    """Get the possible configuration files accepted by oslo.config

    This also includes the deprecated ones
    """
    # default files
    conf_files = cfg.find_config_files(project='monasca',
                                       prog='monasca-log-api')
    # deprecated config files (only used if standard config files are not there)
    if len(conf_files) == 0:
        old_conf_files = cfg.find_config_files(project='monasca',
                                               prog='log-api')
        if len(old_conf_files) > 0:
            LOG.warning('Found deprecated old location "{}" '
                        'of main configuration file'.format(old_conf_files))
            conf_files += old_conf_files
    return conf_files


def parse_args(argv=None):
    global _CONF_LOADED
    if _CONF_LOADED:
        LOG.debug('Configuration has been already loaded')
        return

    log.set_defaults()
    log.register_options(CONF)

    argv = (argv if argv is not None else sys.argv[1:])
    args = ([] if _is_running_under_gunicorn() else argv or [])

    CONF(args=args,
         prog=sys.argv[1:],
         project='monasca',
         version=version.version_str,
         default_config_files=get_config_files(),
         description='RESTful API to collect log files')

    log.setup(CONF,
              product_name='monasca-log-api',
              version=version.version_str)

    conf.register_opts()
    policy_opts.set_defaults(CONF)

    _CONF_LOADED = True
