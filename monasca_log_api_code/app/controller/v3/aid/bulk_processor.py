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

from monasca_log_api import conf
from monasca_log_api.app.base import log_publisher
from monasca_log_api.app.base import model
from monasca_log_api.app.base import validation
from oslo_log import log

LOG = log.getLogger(__name__)
CONF = conf.CONF


class BulkProcessor(log_publisher.LogPublisher):
    """BulkProcessor for effective log processing and publishing.

    BulkProcessor is customized version of
    :py:class:`monasca_log_api.app.base.log_publisher.LogPublisher`
    that utilizes processing of bulk request inside single loop.

    """

    def __init__(self, logs_in_counter=None, logs_rejected_counter=None):
        """Initializes BulkProcessor.

        :param logs_in_counter: V3 received logs counter
        :param logs_rejected_counter: V3 rejected logs counter
        """
        super(BulkProcessor, self).__init__()

        if CONF.monitoring.enable:
            assert logs_in_counter is not None
            assert logs_rejected_counter is not None
            self._logs_in_counter = logs_in_counter
            self._logs_rejected_counter = logs_rejected_counter

        self.service_region = CONF.service.region

    def send_message(self, logs, global_dimensions=None, log_tenant_id=None):
        """Sends bulk package to kafka

        :param list logs: received logs
        :param dict global_dimensions: global dimensions for each log
        :param str log_tenant_id: tenant who sent logs
        """

        num_of_msgs = len(logs) if logs else 0
        sent_count = 0
        to_send_msgs = []

        LOG.debug('Bulk package <logs=%d, dimensions=%s, tenant_id=%s>',
                  num_of_msgs, global_dimensions, log_tenant_id)

        try:
            for log_el in logs:
                t_el = self._transform_message(log_el,
                                               global_dimensions,
                                               log_tenant_id)
                if t_el:
                    to_send_msgs.append(t_el)
            if CONF.monitoring.enable:
                with self._publish_time_ms.time(name=None):
                    self._publish(to_send_msgs)
            else:
                self._publish(to_send_msgs)

            sent_count = len(to_send_msgs)

        except Exception as ex:
            LOG.error('Failed to send bulk package <logs=%d, dimensions=%s>',
                      num_of_msgs, global_dimensions)
            LOG.exception(ex)
            raise ex
        finally:
            if CONF.monitoring.enable:
                self._update_counters(len(to_send_msgs), num_of_msgs)
            self._after_publish(sent_count, len(to_send_msgs))

    def _update_counters(self, in_counter, to_send_counter):
        rejected_counter = to_send_counter - in_counter

        self._logs_in_counter.increment(value=in_counter)
        self._logs_rejected_counter.increment(value=rejected_counter)

    def _transform_message(self, log_element, *args):
        try:
            validation.validate_log_message(log_element)

            log_envelope = model.Envelope.new_envelope(
                log=log_element,
                tenant_id=args[1],
                region=self.service_region,
                dimensions=self._get_dimensions(log_element,
                                                global_dims=args[0])
            )

            msg_payload = (super(BulkProcessor, self)
                           ._transform_message(log_envelope))

            return msg_payload
        except Exception as ex:
            LOG.error('Log transformation failed, rejecting log')
            LOG.exception(ex)

            return None

    def _create_envelope(self, log_element, tenant_id, dimensions=None):
        """Create a log envelope.

        :param dict log_element: raw log element
        :param str tenant_id: tenant who sent logs
        :param dict dimensions: log dimensions
        :return: log envelope
        :rtype: model.Envelope

        """
        return

    def _get_dimensions(self, log_element, global_dims=None):
        """Get the dimensions of log element.

        If global dimensions are specified and passed to this method,
        both instances are merged with each other.

        If neither is specified empty dictionary is returned.

        If only local dimensions are specified they are returned without any
        additional operations. The last statement applies also
        to global dimensions.

        :param dict log_element: raw log instance
        :param dict global_dims: global dimensions or None
        :return: local dimensions merged with global dimensions
        :rtype: dict
        """
        local_dims = log_element.get('dimensions', {})

        if not global_dims:
            global_dims = {}
        if local_dims:
            validation.validate_dimensions(local_dims)

        dimensions = global_dims.copy()
        dimensions.update(local_dims)

        return dimensions
