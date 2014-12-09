# Copyright 2014 Hewlett-Packard
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

alarm_definition_schema = {
    voluptuous.Required('name'): voluptuous.All(voluptuous.Any(str, unicode),
                                                voluptuous.Length(max=250)),
    voluptuous.Required('expression'): voluptuous.All(
        voluptuous.Any(str, unicode), voluptuous.Length(max=4096)),
    voluptuous.Optional('description'): voluptuous.All(
        voluptuous.Any(str, unicode), voluptuous.Length(max=250)),
    voluptuous.Optional('severity'): voluptuous.All(
        voluptuous.Any('low', 'medium', 'high', 'critical', 'LOW', "MEDIUM",
                       'HIGH', 'CRITICAL')),
    voluptuous.Optional('match_by'): voluptuous.All(
        voluptuous.Any([unicode], [str]), voluptuous.Length(max=255)),
    voluptuous.Optional('ok_actions'): voluptuous.All(
        voluptuous.Any([str], [unicode]), voluptuous.Length(max=400)),
    voluptuous.Optional('alarm_actions'): voluptuous.All(
        voluptuous.Any([str], [unicode]), voluptuous.Length(max=400)),
    voluptuous.Optional('undetermined_actions'): voluptuous.All(
        voluptuous.Any([str], [unicode]), voluptuous.Length(max=400)),
    voluptuous.Optional('actions_enabled'): bool}

request_body_schema = voluptuous.Schema(alarm_definition_schema, required=True,
                                        extra=True)


def validate(msg):
    try:
        request_body_schema(msg)
    except Exception as ex:
        LOG.debug(ex)
        raise exceptions.ValidationException(str(ex))
