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

import falcon
from oslo.config import cfg

from monasca.common.messaging import exceptions as message_queue_exceptions
from monasca.common import resource_api
import monasca.expression_parser.alarm_expr_parser
from monasca.openstack.common import log
from monasca.v2.reference import helpers


LOG = log.getLogger(__name__)


class Alarming(object):
    """Super class for Alarms and AlarmDefinitions.

    Shared attributes and methods for classes Alarms and AlarmDefinitions.
    """

    def __init__(self, global_conf):

        super(Alarming, self).__init__()

        self.events_message_queue = (
            resource_api.init_driver('monasca.messaging',
                                     cfg.CONF.messaging.driver,
                                     (['events'])))

        self.alarm_state_transitions_message_queue = (
            resource_api.init_driver('monasca.messaging',
                                     cfg.CONF.messaging.driver,
                                     (['alarm-state-transitions'])))

    def _send_alarm_transitioned_event(self, tenant_id, alarm_id,
                                       alarm_definition_row,
                                       alarm_metric_rows,
                                       old_state, new_state):

        metrics = []
        alarm_transitioned_event_msg = {u'alarm-transitioned': {
            u'tenantId': tenant_id,
            u'alarmId': alarm_id,
            u'alarmDefinitionId': alarm_definition_row['id'],
            u'alarmName': alarm_definition_row['name'],
            u'alarmDescription': alarm_definition_row['description'],
            u'actionsEnabled': alarm_definition_row['actions_enabled'] == 1,
            u'stateChangeReason': 'Alarm state updated via API',
            u'severity': alarm_definition_row['severity'],
            u'oldState': old_state,
            u'newState': new_state,
            u'metrics': metrics}
        }

        for alarm_metric_row in alarm_metric_rows:
            metric = self._build_metric(alarm_metric_row)
            metrics.append(metric)

        self.send_event(self.alarm_state_transitions_message_queue,
                        alarm_transitioned_event_msg)

    def _build_metric(self, alarm_metric_row):

        dimensions = {}

        metric = {u'name': alarm_metric_row['name'],
                  u'dimensions': dimensions}

        for dimension in alarm_metric_row['dimensions'].split(','):
            parsed_dimension = dimension.split('=')
            dimensions[parsed_dimension[0]] = parsed_dimension[1]

        return metric

    def _send_alarm_event(self, event_type, tenant_id, alarm_definition_id,
                          alarm_metric_rows, sub_alarm_rows, extra_info=None):

        if not alarm_metric_rows:
            return

        # Build a dict mapping alarm id -> list of sub alarms.
        sub_alarm_dict = {}
        for sub_alarm_row in sub_alarm_rows:
            if sub_alarm_row['alarm_id'] in sub_alarm_dict:
                sub_alarm_dict[sub_alarm_row['alarm_id']] += [sub_alarm_row]
            else:
                sub_alarm_dict[sub_alarm_row['alarm_id']] = [sub_alarm_row]

        # Forward declaration.
        alarm_event_msg = {}
        prev_alarm_id = None
        for alarm_metric_row in alarm_metric_rows:
            if prev_alarm_id != alarm_metric_row['alarm_id']:
                if prev_alarm_id is not None:
                    sub_alarms_event_msg = (
                        self._build_sub_alarm_event_msg(sub_alarm_dict,
                                                        prev_alarm_id))
                    alarm_event_msg[event_type][
                        u'subAlarms': sub_alarms_event_msg]
                    self.send_event(self.events_message_queue,
                                    alarm_event_msg)

                alarm_metrics_event_msg = []
                alarm_event_msg = {event_type: {u'tenant_id': tenant_id,
                                                u'alarmDefinitionId':
                                                    alarm_definition_id,
                                                u'alarmId': alarm_metric_row[
                                                    'alarm_id'],
                                                u'alarmMetrics':
                                                    alarm_metrics_event_msg}}
                if extra_info:
                    alarm_event_msg[event_type].update(extra_info)

                prev_alarm_id = alarm_metric_row['alarm_id']

            metric = self._build_metric(alarm_metric_row)
            alarm_metrics_event_msg.append(metric)

        # Finish last alarm
        sub_alarms_event_msg = self._build_sub_alarm_event_msg(sub_alarm_dict,
                                                               prev_alarm_id)
        alarm_event_msg[event_type][u'subAlarms'] = sub_alarms_event_msg

        self.send_event(self.events_message_queue,
                        alarm_event_msg)

    def _build_sub_alarm_event_msg(self, sub_alarm_dict, alarm_id):

        sub_alarms_event_msg = {}

        if alarm_id not in sub_alarm_dict:
            return sub_alarms_event_msg

        for sub_alarm in sub_alarm_dict[alarm_id]:
            # There's only one expr in a sub alarm, so just take the first.
            sub_expr = (
                monasca.expression_parser.alarm_expr_parser.AlarmExprParser(
                    sub_alarm['expression']).sub_expr_list[0])
            dimensions = {}
            sub_alarms_event_msg[sub_alarm['sub_alarm_id']] = {
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

        return sub_alarms_event_msg

    def send_event(self, message_queue, event_msg):
        try:
            message_queue.send_message(
                helpers.dumpit_utf8(event_msg))
        except message_queue_exceptions.MessageQueueException as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError(
                'Message queue service unavailable'.encode('utf8'),
                ex.message.encode('utf8'))
