# Copyright 2015 kornicameister@gmail.com
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

import datetime
from monasca_common.rest import utils as rest_utils
from monasca_log_api import conf
from monasca_log_api.app.base import exceptions
from monasca_log_api.app.base import model
from monasca_log_api.app.base import validation
from oslo_config import cfg
from oslo_log import log





LOG = log.getLogger(__name__)
CONF = conf.CONF

EPOCH_START = datetime.datetime(1970, 1, 1)


class LogCreator(object):
    """Transforms logs,

    Takes care of transforming information received via
    HTTP requests into log and log envelopes objects.

    For more details see following:

    * :py:func:`LogCreator.new_log`
    * :py:func:`LogCreator.new_log_envelope`

    """

    def __init__(self):
        self._log = log.getLogger('service.LogCreator')
        self._log.info('Initializing LogCreator')

    @staticmethod
    def _create_meta_info(tenant_id):
        """Creates meta block for log envelope.

        Additionally method accesses oslo configuration,
        looking for *service.region* configuration property.

        For more details see :py:data:`service_opts`

        :param tenant_id: ID of the tenant
        :type tenant_id: str
        :return: meta block
        :rtype: dict

        """
        return {
            'tenantId': tenant_id,
            'region': cfg.CONF.service.region
        }

    def new_log(self,
                application_type,
                dimensions,
                payload,
                content_type='application/json',
                validate=True):
        """Creates new log object.

        :param str application_type: origin of the log
        :param dict dimensions: dictionary of dimensions (any data sent to api)
        :param stream payload: stream to read log entry from
        :param str content_type: actual content type used to send data to
                                 server
        :param bool validate: by default True, marks if log should be validated
        :return: log object
        :rtype: dict

        :keyword: log_object
        """

        payload = rest_utils.read_body(payload, content_type)
        if not payload:
            return None

        # normalize_yet_again
        application_type = parse_application_type(application_type)
        dimensions = parse_dimensions(dimensions)

        if validate:
            self._log.debug('Validation enabled, proceeding with validation')
            validation.validate_application_type(application_type)
            validation.validate_dimensions(dimensions)

        self._log.debug(
            'application_type=%s,dimensions=%s' % (
                application_type, dimensions)
        )

        log_object = {}
        if content_type == 'application/json':
            log_object.update(payload)
        else:
            log_object.update({'message': payload})

        validation.validate_log_message(log_object)

        dimensions['component'] = application_type
        log_object.update({'dimensions': dimensions})

        return log_object

    def new_log_envelope(self, log_object, tenant_id):
        return model.Envelope(
            log=log_object,
            meta=self._create_meta_info(tenant_id)
        )


def parse_application_type(app_type):
    if app_type:
        app_type = app_type.strip()
    return app_type if app_type else None


def parse_dimensions(dimensions):
    if not dimensions:
        raise exceptions.HTTPUnprocessableEntity('Dimension are required')

    new_dimensions = {}
    dimensions = map(str.strip, dimensions.split(','))

    for dim in dimensions:
        if not dim:
            raise exceptions.HTTPUnprocessableEntity(
                'Dimension cannot be empty')
        elif ':' not in dim:
            raise exceptions.HTTPUnprocessableEntity(
                '%s is not a valid dimension' % dim)

        dim = dim.split(':')
        name = str(dim[0].strip()) if dim[0] else None
        value = str(dim[1].strip()) if dim[1] else None
        if name and value:
            new_dimensions.update({name: value})

    return new_dimensions
