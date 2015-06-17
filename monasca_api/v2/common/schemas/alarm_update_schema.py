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

from oslo_log import log
import voluptuous

from monasca_api.v2.common.schemas import exceptions


LOG = log.getLogger(__name__)

alarm_update_schema = {
    voluptuous.Optional('state'): voluptuous.All(
        voluptuous.Any('OK', 'ALARM', 'UNDETERMINED')),
    voluptuous.Optional('lifecycle_state'): voluptuous.All(
        voluptuous.Any(str, unicode), voluptuous.Length(max=50)),
    voluptuous.Optional('link'): voluptuous.All(
        voluptuous.Any(str, unicode), voluptuous.Length(max=512))
}


request_body_schema = voluptuous.Schema(alarm_update_schema, required=True,
                                        extra=True)


def validate(msg):
    try:
        request_body_schema(msg)
    except Exception as ex:
        LOG.debug(ex)
        raise exceptions.ValidationException(str(ex))
