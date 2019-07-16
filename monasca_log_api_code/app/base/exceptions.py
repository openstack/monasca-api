# Copyright 2015 kornicameister@gmail.com
# Copyright 2015 FUJITSU LIMITED
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


HTTP_422 = '422 Unprocessable Entity'


class HTTPUnprocessableEntity(falcon.OptionalRepresentation, falcon.HTTPError):
    """HTTPUnprocessableEntity http error.

    HTTPError that comes with '422 Unprocessable Entity' status

    :argument: message(str) - meaningful description of what caused an error
    :argument: kwargs - any other option defined in
                        :py:class:`falcon.OptionalRepresentation` and
                        :py:class:`falcon.HTTPError`
    """
    def __init__(self, message, **kwargs):
        falcon.HTTPError.__init__(self,
                                  HTTP_422,
                                  'unprocessable_entity',
                                  message,
                                  **kwargs
                                  )
