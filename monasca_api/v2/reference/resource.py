# Copyright 2014 Hewlett-Packard
# (C) Copyright 2015 Hewlett Packard Enterprise Development Company LP
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
from oslo_log import log

from monasca_api.common.repositories import exceptions
from monasca_api.v2.common.exceptions import HTTPUnprocessableEntityError

LOG = log.getLogger(__name__)


def resource_try_catch_block(fun):
    """
    Decorator to catch exceptions.

    Args:
        fun: (callable): write your description
    """
    def try_it(*args, **kwargs):
        """
        Try to raise an exception.

        Args:
        """
        try:
            return fun(*args, **kwargs)

        except falcon.HTTPError:
            raise

        except exceptions.DoesNotExistException:
            raise falcon.HTTPNotFound

        except exceptions.MultipleMetricsException as ex:
            raise falcon.HTTPConflict("MultipleMetrics", str(ex))

        except exceptions.AlreadyExistsException as ex:
            raise falcon.HTTPConflict(ex.__class__.__name__, str(ex))

        except exceptions.InvalidUpdateException as ex:
            raise HTTPUnprocessableEntityError(ex.__class__.__name__, str(ex))

        except exceptions.RepositoryException as ex:
            LOG.exception(ex)
            msg = " ".join(map(str, ex.args[0].args))
            raise falcon.HTTPInternalServerError('The repository was unable '
                                                 'to process your request',
                                                 msg)

        except Exception as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable',
                                                 str(ex))

    return try_it
