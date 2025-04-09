# Copyright 2014 Hewlett-Packard
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

import falcon
import six

from monasca_api.api import versions_api
from monasca_api.v2.common.exceptions import HTTPUnprocessableEntityError
from monasca_api.v2.reference import helpers

VERSIONS = {
    'v2.0': {
        'id': 'v2.0',
        'links': [{
            'rel': 'self',
            'href': ''
        }],
        'status': 'CURRENT',
        'updated': "2013-03-06T00:00:00.000Z"
    }
}


class Versions(versions_api.VersionsAPI):
    def __init__(self):
        super(Versions, self).__init__()

    def on_get(self, req, res, version_id=None):
        req_uri = req.uri.decode('utf8') if six.PY2 else req.uri
        helpers.validate_authorization(req,
                                       ['api:versions'])
        result = {
            'links': [{
                'rel': 'self',
                'href': req_uri
            }],
            'elements': []
        }
        if version_id is None:
            for version in VERSIONS:
                VERSIONS[version]['links'][0]['href'] = (
                    req_uri + version)
                result['elements'].append(VERSIONS[version])
            res.text = helpers.to_json(result)
            res.status = falcon.HTTP_200
        else:
            if version_id in VERSIONS:
                VERSIONS[version_id]['links'][0]['href'] = req_uri
                res.text = helpers.to_json(VERSIONS[version_id])
                res.status = falcon.HTTP_200
            else:
                raise HTTPUnprocessableEntityError('Invalid version',
                                                   'No versions found matching ' + version_id)
