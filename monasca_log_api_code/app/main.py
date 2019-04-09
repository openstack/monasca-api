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

"""
Allows to run monasca-log-api from within local [dev] environment.
Primarily used for development.
"""

import sys
from monasca_log_api import version
from paste import deploy
from paste import httpserver


def get_wsgi_app():
    config_dir = 'etc/monasca'

    return deploy.loadapp(
        'config:%s/log-api-paste.ini' % config_dir,
        relative_to='./',
        name='main'
    )


def main():
    wsgi_app = get_wsgi_app()
    server_version = 'log-api/%s' % version.version_str

    server = httpserver.serve(application=wsgi_app, host='127.0.0.1',
                              port=5607, server_version=server_version)
    return server


if __name__ == '__main__':
    sys.exit(main())
