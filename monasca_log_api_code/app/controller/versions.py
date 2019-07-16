# Copyright 2015 kornicameister@gmail.com
# Copyright 2016 FUJITSU LIMITED
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
import six

from monasca_common.rest import utils as rest_utils

from monasca_log_api.app.base.validation import validate_authorization
from monasca_log_api.app.controller.api import versions_api

_VERSIONS_TPL_DICT = {
    'v2.0': {
        'id': 'v2.0',
        'links': [
            {
                'rel': 'logs',
                'href': '/log/single'
            }
        ],
        'status': 'DEPRECATED',
        'updated': "2015-09-01T00:00:00Z"
    },
    'v3.0': {
        'id': 'v3.0',
        'links': [
            {
                'rel': 'logs',
                'href': '/logs'
            }
        ],
        'status': 'CURRENT',
        'updated': "2016-03-01T00:00:00Z"
    }
}


class Versions(versions_api.VersionsAPI):
    """Versions Api"""

    @staticmethod
    def handle_none_version_id(req, res, result):
        for version in _VERSIONS_TPL_DICT:
            selected_version = _parse_version(version, req)
            result['elements'].append(selected_version)
        res.body = rest_utils.as_json(result, sort_keys=True)
        res.status = falcon.HTTP_200

    @staticmethod
    def handle_version_id(req, res, result, version_id):
        if version_id in _VERSIONS_TPL_DICT:
            result['elements'].append(_parse_version(version_id, req))
            res.body = rest_utils.as_json(result, sort_keys=True)
            res.status = falcon.HTTP_200
        else:
            error_body = {'message': '%s is not valid version' % version_id}
            res.body = rest_utils.as_json(error_body)
            res.status = falcon.HTTP_400

    def on_get(self, req, res, version_id=None):
        validate_authorization(req, ['log_api:versions:get'])
        result = {
            'links': _get_common_links(req),
            'elements': []
        }
        if version_id is None:
            self.handle_none_version_id(req, res, result)
        else:
            self.handle_version_id(req, res, result, version_id)


def _get_common_links(req):
    self_uri = req.uri
    if six.PY2:
        self_uri = self_uri.decode(rest_utils.ENCODING)
    base_uri = self_uri.replace(req.path, '')
    return [
        {
            'rel': 'self',
            'href': self_uri
        },
        {
            'rel': 'version',
            'href': '%s/version' % base_uri
        },
        {
            'rel': 'healthcheck',
            'href': '%s/healthcheck' % base_uri
        }
    ]


def _parse_version(version_id, req):
    self_uri = req.uri
    if six.PY2:
        self_uri = self_uri.decode(rest_utils.ENCODING)
    base_uri = self_uri.replace(req.path, '')

    # need to get template dict, consecutive calls
    # needs to operate on unmodified instance

    selected_version = _VERSIONS_TPL_DICT[version_id].copy()
    raw_links = selected_version['links']
    links = []

    for link in raw_links:
        raw_link_href = link.get('href')
        raw_link_rel = link.get('rel')
        link_href = base_uri + '/' + version_id + raw_link_href
        links.append({
            'href': link_href,
            'rel': raw_link_rel
        })
    selected_version['links'] = links

    return selected_version
