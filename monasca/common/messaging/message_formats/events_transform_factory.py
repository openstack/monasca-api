# Copyright 2014 Hewlett-Packard
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

from oslo.config import cfg

import monasca.common.messaging.message_formats.cadf.events as cadf_events
import monasca.common.messaging.message_formats.identity.events as ident_events
import monasca.common.messaging.message_formats.reference.events as ref_events


def create_events_transform():
    message_format = cfg.CONF.messaging.events_message_format
    if message_format == 'reference':
        return ref_events.transform
    elif message_format == 'cadf':
        return cadf_events.transform
    else:
        return ident_events.transform