# coding=utf-8
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

import codecs
import os
import random
import string

import falcon
from falcon import testing
import fixtures
from monasca_api.common.policy import policy_engine as policy
from oslo_config import cfg
from oslo_config import fixture as oo_cfg
from oslo_context import fixture as oo_ctx
from oslo_serialization import jsonutils
from oslotest import base as oslotest_base
import six
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


def generate_unique_message(size):
    letters = string.ascii_letters

    def rand(amount, space=True):
        space = ' ' if space else ''
        return ''.join((random.choice(letters + space) for _ in range(amount)))

    return rand(size)


def _hex_to_unicode(hex_raw):
    hex_raw = six.b(hex_raw.replace(' ', ''))
    hex_str_raw = codecs.getdecoder('hex')(hex_raw)[0]
    hex_str = hex_str_raw.decode('utf-8', 'replace')
    return hex_str


# NOTE(trebskit) => http://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt
UNICODE_MESSAGES = [
    # Unicode is evil...
    {'case': 'arabic', 'input': 'يونيكود هو الشر'},
    {'case': 'polish', 'input': 'Unicode to zło'},
    {'case': 'greek', 'input': 'Unicode είναι κακό'},
    {'case': 'portuguese', 'input': 'Unicode é malvado'},
    {'case': 'lao', 'input': 'unicode ເປັນຄວາມຊົ່ວຮ້າຍ'},
    {'case': 'german', 'input': 'Unicode ist böse'},
    {'case': 'japanese', 'input': 'ユニコードは悪です'},
    {'case': 'russian', 'input': 'Unicode - зло'},
    {'case': 'urdu', 'input': 'یونیسیڈ برائی ہے'},
    {'case': 'weird', 'input': '🆄🅽🅸🅲🅾🅳🅴 🅸🆂 🅴🆅🅸🅻...'},  # funky, huh ?
    # conditions from link above
    # 2.3  Other boundary conditions
    {'case': 'stress_2_3_1', 'input': _hex_to_unicode('ed 9f bf')},
    {'case': 'stress_2_3_2', 'input': _hex_to_unicode('ee 80 80')},
    {'case': 'stress_2_3_3', 'input': _hex_to_unicode('ef bf bd')},
    {'case': 'stress_2_3_4', 'input': _hex_to_unicode('f4 8f bf bf')},
    {'case': 'stress_2_3_5', 'input': _hex_to_unicode('f4 90 80 80')},
    # 3.5 Impossible byes
    {'case': 'stress_3_5_1', 'input': _hex_to_unicode('fe')},
    {'case': 'stress_3_5_2', 'input': _hex_to_unicode('ff')},
    {'case': 'stress_3_5_3', 'input': _hex_to_unicode('fe fe ff ff')},
    # 4.1 Examples of an overlong ASCII character
    {'case': 'stress_4_1_1', 'input': _hex_to_unicode('c0 af')},
    {'case': 'stress_4_1_2', 'input': _hex_to_unicode('e0 80 af')},
    {'case': 'stress_4_1_3', 'input': _hex_to_unicode('f0 80 80 af')},
    {'case': 'stress_4_1_4', 'input': _hex_to_unicode('f8 80 80 80 af')},
    {'case': 'stress_4_1_5', 'input': _hex_to_unicode('fc 80 80 80 80 af')},
    # 4.2 Maximum overlong sequences
    {'case': 'stress_4_2_1', 'input': _hex_to_unicode('c1 bf')},
    {'case': 'stress_4_2_2', 'input': _hex_to_unicode('e0 9f bf')},
    {'case': 'stress_4_2_3', 'input': _hex_to_unicode('f0 8f bf bf')},
    {'case': 'stress_4_2_4', 'input': _hex_to_unicode('f8 87 bf bf bf')},
    {'case': 'stress_4_2_5', 'input': _hex_to_unicode('fc 83 bf bf bf bf')},
    # 4.3  Overlong representation of the NUL character
    {'case': 'stress_4_3_1', 'input': _hex_to_unicode('c0 80')},
    {'case': 'stress_4_3_2', 'input': _hex_to_unicode('e0 80 80')},
    {'case': 'stress_4_3_3', 'input': _hex_to_unicode('f0 80 80 80')},
    {'case': 'stress_4_3_4', 'input': _hex_to_unicode('f8 80 80 80 80')},
    {'case': 'stress_4_3_5', 'input': _hex_to_unicode('fc 80 80 80 80 80')},
    # and some cheesy example from polish novel 'Pan Tadeusz'
    {'case': 'mr_t', 'input': 'Hajże na Soplicę!'},
    # it won't be complete without that one
    {'case': 'mr_b', 'input': 'Grzegorz Brzęczyszczykiewicz, '
                              'Chrząszczyżewoszyce, powiat Łękołody'},
    # great success, christmas time
    {'case': 'olaf', 'input': '☃'}
]
