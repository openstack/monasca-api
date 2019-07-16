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
from monasca_common.policy import policy_engine as policy
from monasca_log_api.app.base import request
from monasca_log_api.policies import roles_list_to_check_str
from monasca_log_api.tests import base
from oslo_context import context
from oslo_policy import policy as os_policy


class TestPolicyFileCase(base.BaseTestCase):
    def setUp(self):
        super(TestPolicyFileCase, self).setUp()
        self.context = context.RequestContext(user='fake',
                                              tenant='fake',
                                              roles=['fake'])
        self.target = {'tenant_id': 'fake'}

    def test_modified_policy_reloads(self):
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
        super(RegisteredPoliciesTestCase, self).__init__(*args, **kwds)
        self.default_roles = ['monasca-user', 'admin']

    def test_healthchecks_policies_roles(self):
        healthcheck_policies = {
            'log_api:healthcheck:head': ['any_role'],
            'log_api:healthcheck:get': ['any_role']
        }

        self._assert_rules(healthcheck_policies)

    def test_versions_policies_roles(self):
        versions_policies = {
            'log_api:versions:get': ['any_role']
        }

        self._assert_rules(versions_policies)

    def test_logs_policies_roles(self):

        logs_policies = {
            'log_api:logs:post': self.default_roles
        }

        self._assert_rules(logs_policies)

    def _assert_rules(self, policies_list):
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
        return request.Request(
            testing.create_environ(
                path='/',
                headers={'X_ROLES': role}
            )
        )


class PolicyUtilsTestCase(base.BaseTestCase):
    def test_roles_list_to_check_str(self):
        self.assertEqual(roles_list_to_check_str(['test_role']), 'role:test_role')
        self.assertEqual(roles_list_to_check_str(['role1', 'role2', 'role3']),
                         'role:role1 or role:role2 or role:role3')
        self.assertEqual(roles_list_to_check_str(['@']), '@')
        self.assertEqual(roles_list_to_check_str(['role1', '@', 'role2']),
                         'role:role1 or @ or role:role2')
        self.assertIsNone(roles_list_to_check_str(None))
