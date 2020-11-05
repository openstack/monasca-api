# (C) Copyright 2015,2016,2017 Hewlett Packard Enterprise Development Company LP
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

from falcon.http_error import HTTPError


class HTTPUnprocessableEntityError(HTTPError):
    def __init__(self, title, description, **kwargs):
        """
        Initialize the title.

        Args:
            self: (todo): write your description
            title: (str): write your description
            description: (str): write your description
        """
        HTTPError.__init__(self, '422 Unprocessable Entity', title, description, **kwargs)


class HTTPBadRequestError(HTTPError):
    def __init__(self, title, description, **kwargs):
        """
        Initialize the title.

        Args:
            self: (todo): write your description
            title: (str): write your description
            description: (str): write your description
        """
        HTTPError.__init__(self, '400 Bad Request', title, description, **kwargs)
