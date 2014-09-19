# Copyright 2013 IBM Corp
#
# Author: Tong Li <litong01@us.ibm.com>
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


class SimpleLogin(object):
    """Example middleware that demostrates how a login middleware should work.

    In this example, the middleware checks if a request path starts with
    string '/datapoints/', if it does, the request gets pass this
    middleware, otherwise, a 401 response is returned.
    """
    def __init__(self, app, conf):
        self.app = app
        self.conf = conf

    def __call__(self, environ, start_response):
        # if request starts with /datapoints/, then let it go on.
        # this login middle
        if environ.get('PATH_INFO', '').startswith('/datapoints/'):
            return self.app(environ, start_response)
        # otherwise, send something else, request stops here.
        else:
            status = "401 Unauthorized"
            response_headers = [("content-type", "text/plain")]
            start_response(status, response_headers, "please login first")
            return ['Please log in!']


def filter_factory(global_conf, **local_conf):

    def login_filter(app):
        return SimpleLogin(app, local_conf)

    return login_filter
