# Copyright 2013 IBM Corp
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


class Inspector(object):
    """The middleware that logs the request body and header

    This is a middleware for debug purposes. To enable this middleware, add
    the following lines into the configuration file, for example:

        [pipeline:main]
        pipeline = inspector api

        [filter:inspector]
        use = egg: monasca_api_server#inspector
    """
    def __init__(self, app, conf):
        """Inspect each request

        :param app: OptionParser to use. If not sent one will be created.
        :param conf: Override sys.argv; used in testing
        """
        self.app = app
        self.conf = conf
        print('Inspect config:', self.conf)

    def __call__(self, environ, start_response):

        print('environ is ', environ)
        return self.app(environ, start_response)


def filter_factory(global_conf, **local_conf):

    def login_filter(app):
        return Inspector(app, local_conf)

    return login_filter
