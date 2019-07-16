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

LOGS_RECEIVED_METRIC = 'log.in_logs'
"""Metrics sent with amount of logs (not requests) API receives"""

LOGS_REJECTED_METRIC = 'log.in_logs_rejected'
"""Metric sent with amount of logs that were rejected
(i.e. invalid dimension)"""

LOGS_BULKS_REJECTED_METRIC = 'log.in_bulks_rejected'
"""Metric sent with amount of bulk packages that were rejected due
to early stage validation (content-length, content-type).
Only valid for v3.0.
"""

LOGS_RECEIVED_BYTE_SIZE_METRICS = 'log.in_logs_bytes'
"""Metric sent with size of payloads(a.k.a. Content-Length)
 (in bytes) API receives"""

LOGS_PROCESSING_TIME_METRIC = 'log.processing_time_ms'
"""Metric sent with time that log-api needed to process each received log.
Metric does not include time needed to authorize requests."""

LOGS_PUBLISHED_METRIC = 'log.out_logs'
"""Metric sent with amount of logs published to kafka"""

LOGS_PUBLISHED_LOST_METRIC = 'log.out_logs_lost'
"""Metric sent with amount of logs that were lost due to critical error in
publish phase."""

LOGS_PUBLISH_TIME_METRIC = 'log.publish_time_ms'
"""Metric sent with time that publishing took"""

LOGS_TRUNCATED_METRIC = 'log.out_logs_truncated_bytes'
"""Metric sent with amount of truncated bytes from log message"""
