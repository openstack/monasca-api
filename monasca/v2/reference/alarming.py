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
import json
import falcon
from oslo.config import cfg
from monasca.common import resource_api
from monasca.expression_parser.alarm_expr_parser import AlarmExprParser
from monasca.openstack.common import log
from monasca.common.messaging import exceptions as message_queue_exceptions

LOG = log.getLogger(__name__)


class Alarming(object):
    """Super class for Alarms and AlarmDefinitions.

    Shared attributes and methods for classes Alarms and AlarmDefinitions.

    """

    def __init__(self, global_conf):

        super(Alarming, self).__init__()

        self._message_queue \
                = resource_api.init_driver('monasca.messaging',
                                           cfg.CONF.messaging.driver,
                                           (['events']))

    def _send_alarm_deleted_event(self, tenant_id, alarm_definition_id,
                                  alarm_metric_rows, sub_alarm_rows):

        if not alarm_metric_rows:
            return

        # Build a dict mapping alarm id -> list of sub alarms.
        sub_alarm_dict = {}
        for sub_alarm_row in sub_alarm_rows:
            if sub_alarm_row.alarm_id in sub_alarm_dict:
                sub_alarm_dict[sub_alarm_row.alarm_id] += [sub_alarm_row]
            else:
                sub_alarm_dict[sub_alarm_row.alarm_id] = [sub_alarm_row]

        prev_alarm_id = None
        for alarm_metric_row in alarm_metric_rows:
            if prev_alarm_id != alarm_metric_row.alarm_id:
                if prev_alarm_id is not None:
                    sub_alarms_deleted_event_msg = \
                        self._build_sub_alarm_deleted_event_msg(
                        sub_alarm_dict, prev_alarm_id)
                    alarm_deleted_event_msg[u'alarm-delete'][
                    u'subAlarms': sub_alarms_deleted_event_msg]
                    self._send_event(alarm_deleted_event_msg)

                alarm_metrics_event_msg = []
                alarm_deleted_event_msg = {
                    u'alarm-deleted': {u'tenant_id': tenant_id,
                                       u'alarmDefinitionId':
                                           alarm_definition_id,
                                       u'alarmId': alarm_metric_row.alarm_id,
                                       u'alarmMetrics':
                                           alarm_metrics_event_msg}}

                prev_alarm_id = alarm_metric_row.alarm_id

            dimensions = {}
            metric = {u'name': alarm_metric_row.name,
                      u'dimensions': dimensions}
            for dimension in alarm_metric_row.dimensions.split(','):
                parsed_dimension = dimension.split('=')
                dimensions[parsed_dimension[0]] = parsed_dimension[1]

            alarm_metrics_event_msg.append(metric)

        # Finish last alarm
        sub_alarms_deleted_event_msg = self._build_sub_alarm_deleted_event_msg(
            sub_alarm_dict, prev_alarm_id)
        alarm_deleted_event_msg[u'alarm-deleted'][
            u'subAlarms'] = sub_alarms_deleted_event_msg
        self._send_event(alarm_deleted_event_msg)

    def _build_sub_alarm_deleted_event_msg(self, sub_alarm_dict, alarm_id):

        sub_alarms_deleted_event_msg = {}

        if alarm_id not in sub_alarm_dict:
            return sub_alarms_deleted_event_msg

        for sub_alarm in sub_alarm_dict[alarm_id]:
            # There's only one expr in a sub alarm, so just take the first.
            sub_expr = AlarmExprParser(sub_alarm.expression).sub_expr_list[0]
            dimensions = {}
            sub_alarms_deleted_event_msg[sub_alarm.sub_alarm_id] = {
                u'function': sub_expr.normalized_func,
                u'metricDefinition': {u'name': sub_expr.metric_name,
                                      u'dimensions': dimensions},
                u'operator': sub_expr.normalized_operator,
                u'threshold': sub_expr.threshold, u'period': sub_expr.period,
                u'periods': sub_expr.periods,
                u'expression': sub_expr.fmtd_sub_expr_str}

            for dimension in sub_expr.dimensions_as_list:
                parsed_dimension = dimension.split('=')
                dimensions[parsed_dimension[0]] = parsed_dimension[1]

        return sub_alarms_deleted_event_msg

    def _send_event(self, event_msg):
        try:
            self._message_queue.send_message(
                json.dumps(event_msg, ensure_ascii=False).encode('utf8'))
        except message_queue_exceptions.MessageQueueException as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError(
                'Message queue service unavailable'.encode('utf8'),
                ex.message.encode('utf8'))
