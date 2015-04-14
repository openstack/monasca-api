# Copyright 2015 Hewlett-Packard
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

import voluptuous

from monasca.openstack.common import log
from monasca.v2.common.schemas import exceptions


LOG = log.getLogger(__name__)

MILLISEC_PER_DAY = 86400000
MILLISEC_PER_WEEK = MILLISEC_PER_DAY * 7

stream_definition_schema = {
    voluptuous.Required('name'): voluptuous.All(voluptuous.Any(str, unicode),
                                                voluptuous.Length(max=140)),
    voluptuous.Required('select'): voluptuous.All(
        voluptuous.Any(list)),
    voluptuous.Required('group_by'): voluptuous.All(
        voluptuous.Any(list)),
    voluptuous.Required('fire_criteria'): voluptuous.All(
        voluptuous.Any(list)),
    voluptuous.Required('expiration'): voluptuous.All(
        voluptuous.Any(int), voluptuous.Range(min=0, max=MILLISEC_PER_WEEK)),

    voluptuous.Optional('fire_actions'): voluptuous.All(
        voluptuous.Any([str], [unicode]), voluptuous.Length(max=400)),
    voluptuous.Optional('expire_actions'): voluptuous.All(
        voluptuous.Any([str], [unicode]), voluptuous.Length(max=400)),
    voluptuous.Optional('actions_enabled'): bool}

request_body_schema = voluptuous.Schema(stream_definition_schema,
                                        required=True, extra=True)


def validate(msg):
    try:
        request_body_schema(msg)
    except Exception as ex:
        LOG.debug(ex)
        raise exceptions.ValidationException(str(ex))
