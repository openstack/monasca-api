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

import mock

from monasca_log_api import config
from monasca_log_api.tests import base


class TestConfig(base.BaseTestCase):

    @mock.patch('monasca_log_api.config.sys')
    def test_should_return_true_if_runs_under_gunicorn(self, sys_patch):
        sys_patch.argv = [
            '/bin/gunicorn',
            '--capture-output',
            '--paste',
            'etc/monasca/log-api-paste.ini',
            '--workers',
            '1'
        ]
        sys_patch.executable = '/bin/python'
        self.assertTrue(config._is_running_under_gunicorn())

    @mock.patch('monasca_log_api.config.sys')
    def test_should_return_false_if_runs_without_gunicorn(self, sys_patch):
        sys_patch.argv = ['/bin/monasca-log-api']
        sys_patch.executable = '/bin/python'
        self.assertFalse(config._is_running_under_gunicorn())
