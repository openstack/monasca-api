# Copyright 2016 Hewlett Packard Enterprise Development Company, L.P.
# Copyright 2018 OP5 AB
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

from oslo_utils import encodeutils

from monasca_api.v2.reference import helpers


class Version2(object):
    def __init__(self):
        super(Version2, self).__init__()

    def on_get(self, req, res):
        helpers.validate_authorization(req,
                                       ['api:versions'])
        result = {
            'id': 'v2.0',
            'links': [{
                'rel': 'self',
                'href': encodeutils.safe_decode(req.uri, 'utf-8')
            }],
            'status': 'CURRENT',
            'updated': "2013-03-06T00:00:00.000Z"
        }
        res.body = helpers.to_json(result)
