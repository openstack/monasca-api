# (C) Copyright 2015 Hewlett Packard Enterprise Development Company LP
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

from oslo_config import cfg


service_option = cfg.BoolOpt("monasca",
                             default=True,
                             help="Whether or not Monasca is expected to be "
                                  "available")

monitoring_group = cfg.OptGroup(name="monitoring",
                                title="Monitoring Service Options")

MonitoringGroup = [
    cfg.StrOpt("region",
               default="",
               help="The monitoring region name to use. If empty, the value "
                    "of identity.region is used instead. If no such region "
                    "is found in the service catalog, the first found one is "
                    "used."),
    cfg.StrOpt("catalog_type",
               default="monitoring",
               help="Catalog type of the monitoring service."),
    cfg.StrOpt('endpoint_type',
               default='publicURL',
               choices=['public', 'admin', 'internal',
                        'publicURL', 'adminURL', 'internalURL'],
               help="The endpoint type to use for the monitoring service.")
]
