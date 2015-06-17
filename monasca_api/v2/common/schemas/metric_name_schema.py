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

from oslo_log import log
import voluptuous

from monasca_api.v2.common.schemas import exceptions

LOG = log.getLogger(__name__)

metric_name_schema = voluptuous.Schema(
    voluptuous.All(voluptuous.Any(str, unicode), voluptuous.Length(max=64)))


def validate(name):
    try:
        metric_name_schema(name)
    except Exception as ex:
        LOG.debug(ex)
        raise exceptions.ValidationException(str(ex))
