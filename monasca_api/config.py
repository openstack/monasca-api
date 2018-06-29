# Copyright 2017 FUJITSU LIMITED
# Copyright 2018 OP5 AB
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

from oslo_config import cfg
from oslo_log import log
from oslo_policy import opts as policy_opts

from monasca_api import conf
from monasca_api import version

CONF = conf.CONF
LOG = log.getLogger(__name__)

_CONF_LOADED = False
_GUNICORN_MARKER = 'gunicorn'


def parse_args(argv=None, config_file=None):
    """Loads application configuration.

    Loads entire application configuration just once.

    """
    global _CONF_LOADED
    if _CONF_LOADED:
        LOG.debug('Configuration has been already loaded')
        return

    log.set_defaults()
    log.register_options(CONF)

    argv = (argv if argv is not None else sys.argv[1:])
    args = ([] if _is_running_under_gunicorn() else argv or [])

    CONF(args=args,
         prog='api',
         project='monasca',
         version=version.version_str,
         default_config_files=get_config_file(config_file),
         description='RESTful API for alarming in the cloud')

    log.setup(CONF,
              product_name='monasca-api',
              version=version.version_str)
    conf.register_opts()
    policy_opts.set_defaults(CONF)

    _CONF_LOADED = True


def get_config_file(config_file):
    """Get config file in a format suitable for CONF constructor

    Returns the config file name as a single element array. If a config file
    was explicitly, specified, that file's name is returned. If there isn't and a
    legacy config file is present that one is returned. Otherwise we return
    None.  This is what the CONF constructor expects for its
    default_config_files keyword argument.
    """
    if config_file is not None:
        return [config_file]

    return _get_deprecated_config_file()


def _is_running_under_gunicorn():
    """Evaluates if api runs under gunicorn."""
    content = filter(lambda x: x != sys.executable and _GUNICORN_MARKER in x,
                     sys.argv or [])
    return len(list(content) if not isinstance(content, list) else content) > 0


def _get_deprecated_config_file():
    """Get deprecated config file.

    Responsible for keeping  backward compatibility with old name of
    the configuration file i.e. api-config.conf.
    New name is => api.conf as prog=api.

    Note:
        Old configuration file name did not follow a convention
        oslo_config expects.

    """
    old_files = cfg.find_config_files(project='monasca', prog='api-config')
    if old_files is not None and len(old_files) > 0:
        LOG.warning('Detected old location "/etc/monasca/api-config.conf" '
                    'of main configuration file')
        return [old_files[0]]
    return None
