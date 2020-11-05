# Copyright 2016-2017 FUJITSU LIMITED
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
from falcon import testing

from oslo_context import context
from oslo_policy import policy as os_policy

from monasca_api.api.core import request
from monasca_api.common.policy import policy_engine as policy
from monasca_api.policies import roles_list_to_check_str
from monasca_api.tests import base


class TestPolicyFileCase(base.BaseTestCase):
    def setUp(self):
        """
        Sets the roles for this view.

        Args:
            self: (todo): write your description
        """
        super(TestPolicyFileCase, self).setUp()
        self.context = context.RequestContext(user='fake',
                                              tenant='fake',
                                              roles=['fake'])
        self.target = {'tenant_id': 'fake'}

    def test_modified_policy_reloads(self):
        """
        .. versionadded :: 0. 17. 0

        Args:
            self: (todo): write your description
        """
        tmp_file = \
            self.create_tempfiles(files=[('policies', '{}')], ext='.yaml')[0]
        base.BaseTestCase.conf_override(policy_file=tmp_file,
                                        group='oslo_policy')

        policy.reset()
        policy.init()
        action = 'example:test'
        rule = os_policy.RuleDefault(action, '')
        policy._ENFORCER.register_defaults([rule])

        with open(tmp_file, 'w') as policy_file:
            policy_file.write('{"example:test": ""}')
        policy.authorize(self.context, action, self.target)

        with open(tmp_file, 'w') as policy_file:
            policy_file.write('{"example:test": "!"}')
        policy._ENFORCER.load_rules(True)
        self.assertRaises(os_policy.PolicyNotAuthorized, policy.authorize,
                          self.context, action, self.target)


class TestPolicyCase(base.BaseTestCase):
    def setUp(self):
        """
        Sets the default rules for this module.

        Args:
            self: (todo): write your description
        """
        super(TestPolicyCase, self).setUp()
        rules = [
            os_policy.RuleDefault("true", "@"),
            os_policy.RuleDefault("example:allowed", "@"),
            os_policy.RuleDefault("example:denied", "!"),
            os_policy.RuleDefault("example:lowercase_monasca_user",
                                  "role:monasca_user or role:sysadmin"),
            os_policy.RuleDefault("example:uppercase_monasca_user",
                                  "role:MONASCA_USER or role:sysadmin"),
        ]
        policy.reset()
        policy.init()
        policy._ENFORCER.register_defaults(rules)

    def test_authorize_nonexist_action_throws(self):
        """
        Test if axist action_throws an oxist.

        Args:
            self: (todo): write your description
        """
        action = "example:noexist"
        ctx = request.Request(
            testing.create_environ(
                path="/",
                headers={
                    "X_USER_ID": "fake",
                    "X_PROJECT_ID": "fake",
                    "X_ROLES": "member"
                }
            )
        )
        self.assertRaises(os_policy.PolicyNotRegistered, policy.authorize,
                          ctx.context, action, {})

    def test_authorize_bad_action_throws(self):
        """
        Test if the oauth action.

        Args:
            self: (todo): write your description
        """
        action = "example:denied"
        ctx = request.Request(
            testing.create_environ(
                path="/",
                headers={
                    "X_USER_ID": "fake",
                    "X_PROJECT_ID": "fake",
                    "X_ROLES": "member"
                }
            )
        )
        self.assertRaises(os_policy.PolicyNotAuthorized, policy.authorize,
                          ctx.context, action, {})

    def test_authorize_bad_action_no_exception(self):
        """
        Test if an oauth policy.

        Args:
            self: (todo): write your description
        """
        action = "example:denied"
        ctx = request.Request(
            testing.create_environ(
                path="/",
                headers={
                    "X_USER_ID": "fake",
                    "X_PROJECT_ID": "fake",
                    "X_ROLES": "member"
                }
            )
        )
        result = policy.authorize(ctx.context, action, {}, False)
        self.assertFalse(result)

    def test_authorize_good_action(self):
        """
        Test for authorize action.

        Args:
            self: (todo): write your description
        """
        action = "example:allowed"
        ctx = request.Request(
            testing.create_environ(
                path="/",
                headers={
                    "X_USER_ID": "fake",
                    "X_PROJECT_ID": "fake",
                    "X_ROLES": "member"
                }
            )
        )
        result = policy.authorize(ctx.context, action, {}, False)
        self.assertTrue(result)

    def test_ignore_case_role_check(self):
        """
        Test if the case case case.

        Args:
            self: (todo): write your description
        """
        lowercase_action = "example:lowercase_monasca_user"
        uppercase_action = "example:uppercase_monasca_user"

        monasca_user_context = request.Request(
            testing.create_environ(
                path="/",
                headers={
                    "X_USER_ID": "monasca_user",
                    "X_PROJECT_ID": "fake",
                    "X_ROLES": "MONASCA_user"
                }
            )
        )
        self.assertTrue(policy.authorize(monasca_user_context.context,
                                         lowercase_action,
                                         {}))
        self.assertTrue(policy.authorize(monasca_user_context.context,
                                         uppercase_action,
                                         {}))


class RegisteredPoliciesTestCase(base.BaseTestCase):
    def __init__(self, *args, **kwds):
        """
        Initialize roles.

        Args:
            self: (todo): write your description
            kwds: (todo): write your description
        """
        super(RegisteredPoliciesTestCase, self).__init__(*args, **kwds)
        self.agent_roles = ['monasca-agent']
        self.readonly_roles = ['monasca-read-only-user']
        self.default_roles = ['monasca-user']
        self.delegate_roles = ['admin']

    def test_alarms_policies_roles(self):
        """
        Test if the roles are roles.

        Args:
            self: (todo): write your description
        """
        alarms_policies = {
            'api:alarms:definition:post': self.default_roles,
            'api:alarms:definition:get':
                self.default_roles + self.readonly_roles,
            'api:alarms:definition:put': self.default_roles,
            'api:alarms:definition:patch': self.default_roles,
            'api:alarms:definition:delete': self.default_roles,
            'api:alarms:put': self.default_roles,
            'api:alarms:patch': self.default_roles,
            'api:alarms:delete': self.default_roles,
            'api:alarms:get': self.default_roles + self.readonly_roles,
            'api:alarms:count': self.default_roles + self.readonly_roles,
            'api:alarms:state_history': self.default_roles + self.readonly_roles
        }

        self._assert_rules(alarms_policies)

    def test_metrics_policies_roles(self):
        """
        Test for the roles

        Args:
            self: (todo): write your description
        """
        metrics_policies = {
            'api:metrics:get': self.default_roles + self.readonly_roles,
            'api:metrics:post': self.agent_roles + self.default_roles,
            'api:metrics:dimension:values':
                self.default_roles + self.readonly_roles,
            'api:metrics:dimension:names':
                self.default_roles + self.readonly_roles

        }
        self._assert_rules(metrics_policies)

    def test_notifications_policies_roles(self):
        """
        Set the roles for the user.

        Args:
            self: (todo): write your description
        """
        notifications_policies = {
            'api:notifications:put': self.default_roles,
            'api:notifications:patch': self.default_roles,
            'api:notifications:delete': self.default_roles,
            'api:notifications:get': self.default_roles + self.readonly_roles,
            'api:notifications:post': self.default_roles,
            'api:notifications:type': self.default_roles + self.readonly_roles,

        }
        self._assert_rules(notifications_policies)

    def test_versions_policies_roles(self):
        """
        Test if the rules are enabled.

        Args:
            self: (todo): write your description
        """
        versions_policies = {
            'api:versions': ['any_rule!']

        }
        self._assert_rules(versions_policies)

    def test_healthcheck_policies_roles(self):
        """
        Test if the health rules.

        Args:
            self: (todo): write your description
        """
        healthcheck_policies = {
            'api:healthcheck': ['any_rule!']
        }
        self._assert_rules(healthcheck_policies)

    def test_delegate_policies_roles(self):
        """
        Test if the rules are enabled.

        Args:
            self: (todo): write your description
        """
        delegate_policies = {
            'api:delegate': self.delegate_roles
        }
        self._assert_rules(delegate_policies)

    def _assert_rules(self, policies_list):
        """
        Assign policies for the policies.

        Args:
            self: (todo): write your description
            policies_list: (list): write your description
        """
        for policy_name in policies_list:
            registered_rule = policy.get_rules()[policy_name]
            if hasattr(registered_rule, 'rules'):
                self.assertEqual(len(registered_rule.rules),
                                 len(policies_list[policy_name]))
            for role in policies_list[policy_name]:
                ctx = self._get_request_context(role)
                self.assertTrue(policy.authorize(ctx.context,
                                                 policy_name,
                                                 {})
                                )

    @staticmethod
    def _get_request_context(role):
        """
        Return the context for the given role.

        Args:
            role: (str): write your description
        """
        return request.Request(
            testing.create_environ(
                path='/',
                headers={'X_ROLES': role}
            )
        )


class PolicyUtilsTestCase(base.BaseTestCase):
    def test_roles_list_to_check_str(self):
        """
        Convert a list of roles to a string.

        Args:
            self: (todo): write your description
        """
        self.assertEqual(roles_list_to_check_str(['test_role']), 'role:test_role')
        self.assertEqual(roles_list_to_check_str(['role1', 'role2', 'role3']),
                         'role:role1 or role:role2 or role:role3')
        self.assertEqual(roles_list_to_check_str(['@']), '@')
        self.assertEqual(roles_list_to_check_str(['role1', '@', 'role2']),
                         'role:role1 or @ or role:role2')
