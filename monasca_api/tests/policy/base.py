# Copyright 2017 OP5 AB
# Copyright 2010 United States Government as represented by the
# Administrator of the National Aeronautics and Space Administration.
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

"""Base classes for policy unit tests."""

import os

import fixtures

from oslo_config import cfg
from oslo_config import fixture as config_fixture
from oslo_policy import opts as policy_opts
from oslo_serialization import jsonutils
from oslotest import base

from monasca_api.common.policy import policy_engine

CONF = cfg.CONF


class FakePolicy(object):
    def list_rules(self):
        return []


class ConfigFixture(config_fixture.Config):

    def setUp(self):
        super(ConfigFixture, self).setUp()
        CONF(args=[],
             prog='api',
             project='monasca',
             version=0,
             description='Testing monasca-api.common')
        policy_opts.set_defaults(CONF)


class BaseTestCase(base.BaseTestCase):
    def setUp(self):
        super(BaseTestCase, self).setUp()
        self.useFixture(ConfigFixture(CONF))
        self.useFixture(EmptyPolicyFixture())

    @staticmethod
    def conf_override(**kw):
        """Override flag variables for a test."""
        group = kw.pop('group', None)
        for k, v in kw.items():
            CONF.set_override(k, v, group)


class EmptyPolicyFixture(fixtures.Fixture):
    """Override the policy with an empty policy file.

    This overrides the policy with a completely fake and synthetic
    policy file.

    """
    def setUp(self):
        super(EmptyPolicyFixture, self).setUp()
        self._prepare_policy()
        policy_engine.POLICIES = FakePolicy()
        policy_engine.reset()
        policy_engine.init()
        self.addCleanup(policy_engine.reset)

    def _prepare_policy(self):

        policy_dir = self.useFixture(fixtures.TempDir())
        policy_file = os.path.join(policy_dir.path, 'policy.yaml')

        policy_rules = jsonutils.loads('{}')

        self.add_missing_default_rules(policy_rules)

        with open(policy_file, 'w') as f:
            jsonutils.dump(policy_rules, f)

        BaseTestCase.conf_override(policy_file=policy_file,
                                   group='oslo_policy')
        BaseTestCase.conf_override(policy_dirs=[], group='oslo_policy')

    def add_missing_default_rules(self, rules):
        policies = FakePolicy()

        for rule in policies.list_rules():
            if rule.name not in rules:
                rules[rule.name] = rule.check_str
