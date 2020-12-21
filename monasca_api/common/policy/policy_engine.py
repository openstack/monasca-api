# Copyright 2017 OP5 AB
# Copyright 2017 FUJITSU LIMITED
# Copyright (c) 2011 OpenStack Foundation
# All Rights Reserved.
#
#    Licensed under the Apache License, Version 2.0 (the "License"); you may
#    not use this file except in compliance with the License. You may obtain
#    a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#    License for the specific language governing permissions and limitations
#    under the License.


import copy
import re
import sys

import logging
from oslo_config import cfg
from oslo_policy import opts
from oslo_policy import policy

from monasca_api.common.policy.i18n import _LW

CONF = cfg.CONF
LOG = logging.getLogger(__name__)
POLICIES = None
USER_BASED_RESOURCES = ['os-keypairs']
KEY_EXPR = re.compile(r'%\((\w+)\)s')


_ENFORCER = None
# oslo_policy will read the policy configuration file again when the file
# is changed in runtime so the old policy rules will be saved to
# saved_file_rules and used to compare with new rules to determine
# whether the rules were updated.
saved_file_rules = []


# TODO(gmann): Remove setting the default value of config policy_file
# once oslo_policy change the default value to 'policy.yaml'.
# https://github.com/openstack/oslo.policy/blob/a626ad12fe5a3abd49d70e3e5b95589d279ab578/oslo_policy/opts.py#L49
DEFAULT_POLICY_FILE = 'policy.yaml'
opts.set_defaults(cfg.CONF, DEFAULT_POLICY_FILE)


def reset():
    """Reset Enforcer class."""
    global _ENFORCER
    if _ENFORCER:
        _ENFORCER.clear()
        _ENFORCER = None


def init(policy_file=None, rules=None, default_rule=None, use_conf=True):
    """Init an Enforcer class.

       :param policy_file: Custom policy file to use, if none is specified,
                           `CONF.policy_file` will be used.
       :param rules: Default dictionary / Rules to use. It will be
                     considered just in the first instantiation.
       :param default_rule: Default rule to use, CONF.default_rule will
                            be used if none is specified.
       :param use_conf: Whether to load rules from config file.
    """

    global _ENFORCER
    global saved_file_rules

    if not _ENFORCER:
        _ENFORCER = policy.Enforcer(CONF,
                                    policy_file=policy_file,
                                    rules=rules,
                                    default_rule=default_rule,
                                    use_conf=use_conf
                                    )
        register_rules(_ENFORCER)
        _ENFORCER.load_rules()
    # Only the rules which are loaded from file may be changed
    current_file_rules = _ENFORCER.file_rules
    current_file_rules = _serialize_rules(current_file_rules)

    if saved_file_rules != current_file_rules:
        _warning_for_deprecated_user_based_rules(current_file_rules)
        saved_file_rules = copy.deepcopy(current_file_rules)


def _serialize_rules(rules):
    """Serialize all the Rule object as string.

    New string is used to compare the rules list.
    """
    result = [(rule_name, str(rule)) for rule_name, rule in rules.items()]
    return sorted(result, key=lambda rule: rule[0])


def _warning_for_deprecated_user_based_rules(rules):
    """Warning user based policy enforcement used in the rule but the rule
    doesn't support it.
    """
    for rule in rules:
        # We will skip the warning for the resources which support user based
        # policy enforcement.
        if [resource for resource in USER_BASED_RESOURCES
                if resource in rule[0]]:
            continue
        if 'user_id' in KEY_EXPR.findall(rule[1]):
            LOG.warning(_LW("The user_id attribute isn't supported in the "
                            "rule '%s'. All the user_id based policy "
                            "enforcement will be removed in the "
                            "future."), rule[0])


def register_rules(enforcer):
    """Register default policy rules."""
    rules = POLICIES.list_rules()
    enforcer.register_defaults(rules)


def authorize(context, action, target, do_raise=True):
    """Verify that the action is valid on the target in this context.

    :param context: monasca project context
    :param action: String representing the action to be checked. This
                   should be colon separated for clarity.
    :param target: Dictionary representing the object of the action for
                   object creation. This should be a dictionary representing
                   the location of the object e.g.
                   ``{'project_id': 'context.project_id'}``
    :param do_raise: if True (the default), raises PolicyNotAuthorized,
                     if False returns False
    :type context: object
    :type action: str
    :type target: dict
    :type do_raise: bool
    :return: returns a non-False value (not necessarily True) if authorized,
             and the False if not authorized and do_raise if False

    :raises oslo_policy.policy.PolicyNotAuthorized: if verification fails
    """
    init()
    credentials = context.to_policy_values()
    try:
        result = _ENFORCER.authorize(action, target, credentials,
                                     do_raise=do_raise, action=action)
        return result
    except policy.PolicyNotRegistered:
        LOG.exception('Policy not registered')
        raise
    except Exception:
        LOG.debug('Policy check for %(action)s failed with credentials '
                  '%(credentials)s',
                  {'action': action, 'credentials': credentials})
        raise


def check_is_admin(context):
    """Check if roles contains 'admin' role according to policy settings."""
    init()
    credentials = context.to_policy_values()
    target = credentials
    return _ENFORCER.authorize('admin_required', target, credentials)


def set_rules(rules, overwrite=True, use_conf=False):  # pragma: no cover
    """Set rules based on the provided dict of rules.

    Note:
        Used in tests only.

    :param rules: New rules to use. It should be an instance of dict
    :param overwrite: Whether to overwrite current rules or update them
                      with the new rules.
    :param use_conf: Whether to reload rules from config file.
    """
    init(use_conf=False)
    _ENFORCER.set_rules(rules, overwrite, use_conf)


def verify_deprecated_policy(old_policy, new_policy, default_rule, context):
    """Check the rule of the deprecated policy action

    If the current rule of the deprecated policy action is set to a non-default
    value, then a warning message is logged stating that the new policy
    action should be used to dictate permissions as the old policy action is
    being deprecated.

    :param old_policy: policy action that is being deprecated
    :param new_policy: policy action that is replacing old_policy
    :param default_rule: the old_policy action default rule value
    :param context: the monasca context
    """

    if _ENFORCER:
        current_rule = str(_ENFORCER.rules[old_policy])
    else:
        current_rule = None

    if current_rule != default_rule:
        LOG.warning("Start using the new action '{0}'. The existing "
                    "action '{1}' is being deprecated and will be "
                    "removed in future release.".format(new_policy,
                                                        old_policy))
        target = {'project_id': context.project_id,
                  'user_id': context.user_id}

        return authorize(context=context, action=old_policy, target=target)
    else:
        return False


def get_rules():
    if _ENFORCER:
        return _ENFORCER.rules


def get_enforcer():
    # This method is for use by oslopolicy CLI scripts. Those scripts need the
    # 'output-file' and 'namespace' options, but having those in sys.argv means
    # loading the project config options will fail as those are not expected to
    # be present. So we pass in an arg list with those stripped out.
    conf_args = []
    # Start at 1 because cfg.CONF expects the equivalent of sys.argv[1:]
    i = 1
    while i < len(sys.argv):
        if sys.argv[i].strip('-') in ['namespace', 'output-file']:
            i += 2
            continue
        conf_args.append(sys.argv[i])
        i += 1

    cfg.CONF(conf_args, project='monasca')
    init()
    return _ENFORCER


@policy.register('is_admin')
class IsAdminCheck(policy.Check):
    """An explicit check for is_admin."""

    def __init__(self, kind, match):
        """Initialize the check."""

        self.expected = (match.lower() == 'true')

        super(IsAdminCheck, self).__init__(kind, str(self.expected))

    def __call__(self, target, creds, enforcer):
        """Determine whether is_admin matches the requested value."""

        return creds['is_admin'] == self.expected
