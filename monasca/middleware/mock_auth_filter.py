# Copyright 2014 Hewlett-Packard
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


class MockAuthFilter(object):
    """Authorization filter.

    This authorization filter doesn't do any authentication, it just copies the
    auth token to the tenant ID and supplies the 'admin' role and is meant for
    testing purposes only.
    """

    def __init__(self, app, conf):
        self.app = app
        self.conf = conf

    def __call__(self, env, start_response):
        env['HTTP_X_TENANT_ID'] = env['HTTP_X_AUTH_TOKEN']
        env['HTTP_X_ROLES'] = 'admin'
        return self.app(env, start_response)


def filter_factory(global_conf, **local_conf):
    def validator_filter(app):
        return MockAuthFilter(app, local_conf)
    return validator_filter
