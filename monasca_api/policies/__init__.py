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

import os
import pkgutil

from oslo_config import cfg
from oslo_log import log
from oslo_utils import importutils

from monasca_api.conf import security

LOG = log.getLogger(__name__)
_BASE_MOD_PATH = 'monasca_api.policies.'
CONF = cfg.CONF


def roles_list_to_check_str(roles_list):
    converted_roles_list = ["role:" + role if role != '@' else role for role in roles_list]
    return ' or '.join(converted_roles_list)


security.register_opts(CONF)


def load_policy_modules():
    """Load all modules that contain policies.

    Method iterates over modules of :py:mod:`monasca_events_api.policies`
    and imports only those that contain following methods:

    - list_rules

    """
    for modname in _list_module_names():
        mod = importutils.import_module(_BASE_MOD_PATH + modname)
        if hasattr(mod, 'list_rules'):
            yield mod


def _list_module_names():
    package_path = os.path.dirname(os.path.abspath(__file__))
    for _, modname, ispkg in pkgutil.iter_modules(path=[package_path]):
        if not (modname == "opts" and ispkg):
            yield modname


def list_rules():
    """List all policy modules rules.

    Goes through all policy modules and yields their rules

    """
    all_rules = []
    for mod in load_policy_modules():
        rules = mod.list_rules()
        all_rules.extend(rules)
    return all_rules
