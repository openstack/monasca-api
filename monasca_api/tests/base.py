# Copyright 2015 kornicameister@gmail.com
# Copyright 2015-2017 FUJITSU LIMITED
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
from falcon import testing
from oslo_config import cfg
from oslo_config import fixture as oo_cfg
from oslo_context import fixture as oo_ctx
from oslotest import base as oslotest_base

from monasca_api.api.core import request
from monasca_api import conf
from monasca_api import config


class MockedAPI(falcon.API):
    """MockedAPI

    Subclasses :py:class:`falcon.API` in order to overwrite
    request_type property with custom :py:class:`request.Request`

    """

    def __init__(self):
        super(MockedAPI, self).__init__(
            media_type=falcon.DEFAULT_MEDIA_TYPE,
            request_type=request.Request,
            response_type=falcon.Response,
            middleware=None,
            router=None
        )


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


class BaseApiTestCase(BaseTestCase, testing.TestBase):
    api_class = MockedAPI

    @staticmethod
    def create_environ(*args, **kwargs):
        return testing.create_environ(
            *args,
            **kwargs
        )
