# Copyright 2015 kornicameister@gmail.com
# Copyright 2015-2017 FUJITSU LIMITED
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

import falcon
from falcon import testing
import fixtures
from monasca_common.policy import policy_engine as policy
from oslo_config import cfg
from oslo_config import fixture as oo_cfg
from oslo_context import fixture as oo_ctx
from oslo_serialization import jsonutils
from oslotest import base as oslotest_base
import testtools.matchers as matchers

from monasca_api.api.core import request
from monasca_api import conf
from monasca_api import config
from monasca_api import policies


policy.POLICIES = policies


class ConfigFixture(oo_cfg.Config):
    """Mocks configuration"""

    def __init__(self):
        super(ConfigFixture, self).__init__(config.CONF)

    def setUp(self):
        super(ConfigFixture, self).setUp()
        self.addCleanup(self._clean_config_loaded_flag)
        conf.register_opts()
        self._set_defaults()
        config.parse_args(argv=[])  # prevent oslo from parsing test args

    @staticmethod
    def _clean_config_loaded_flag():
        config._CONF_LOADED = False

    def _set_defaults(self):
        self.conf.set_default('user', 'monasca', 'influxdb')


class BaseTestCase(oslotest_base.BaseTestCase):

    def setUp(self):
        super(BaseTestCase, self).setUp()
        self.useFixture(ConfigFixture())
        self.useFixture(oo_ctx.ClearRequestContext())
        self.useFixture(PolicyFixture())

    @staticmethod
    def conf_override(**kw):
        """Override flag variables for a test."""
        group = kw.pop('group', None)
        for k, v in kw.items():
            cfg.CONF.set_override(k, v, group)

    @staticmethod
    def conf_default(**kw):
        """Override flag variables for a test."""
        group = kw.pop('group', None)
        for k, v in kw.items():
            cfg.CONF.set_default(k, v, group)


class BaseApiTestCase(BaseTestCase, testing.TestCase):

    def setUp(self):
        super(BaseApiTestCase, self).setUp()
        # TODO(dszumski): Loading the app from api/server.py seems to make
        # more sense here so that we don't have to manually keep the tests in
        # sync with it.
        self.app = falcon.API(request_type=request.Request)
        # NOTE(dszumski): Falcon 2.0.0 switches the default for this from True
        # to False so we explicitly set it here to prevent the behaviour
        # changing between versions.
        self.app.req_options.strip_url_path_trailing_slash = True

    @staticmethod
    def create_environ(*args, **kwargs):
        return testing.create_environ(
            *args,
            **kwargs
        )


class PolicyFixture(fixtures.Fixture):
    """Override the policy with a completely new policy file.

    This overrides the policy with a completely fake and synthetic
    policy file.

     """

    def setUp(self):
        super(PolicyFixture, self).setUp()
        self._prepare_policy()
        policy.reset()
        policy.init()

    def _prepare_policy(self):
        policy_dir = self.useFixture(fixtures.TempDir())
        policy_file = os.path.join(policy_dir.path, 'policy.yaml')
        # load the fake_policy data and add the missing default rules.
        policy_rules = jsonutils.loads('{}')
        self.add_missing_default_rules(policy_rules)
        with open(policy_file, 'w') as f:
            jsonutils.dump(policy_rules, f)

        BaseTestCase.conf_override(policy_file=policy_file,
                                   group='oslo_policy')
        BaseTestCase.conf_override(policy_dirs=[], group='oslo_policy')

    @staticmethod
    def add_missing_default_rules(rules):
        for rule in policies.list_rules():
            if rule.name not in rules:
                rules[rule.name] = rule.check_str


class RESTResponseEquals(object):
    """Match if the supplied data contains a single string containing a JSON
    object which decodes to match expected_data, excluding the contents of
    the 'links' key.
    """

    def __init__(self, expected_data):
        self.expected_data = expected_data

        if u"links" in expected_data:
            del expected_data[u"links"]

    def __str__(self):
        return 'RESTResponseEquals(%s)' % (self.expected_data,)

    def match(self, actual):
        response_data = actual.json

        if u"links" in response_data:
            del response_data[u"links"]

        return matchers.Equals(self.expected_data).match(response_data)
