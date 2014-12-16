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

from monasca.common.repositories import exceptions
from monasca.openstack.common import log


LOG = log.getLogger(__name__)


def resource_try_catch_block(fun):

    def try_it(*args, **kwargs):

        try:

            return fun(*args, **kwargs)

        except falcon.HTTPNotFound:
            raise
        except exceptions.DoesNotExistException:
            raise falcon.HTTPNotFound
        except falcon.HTTPBadRequest:
            raise
        except exceptions.AlreadyExistsException as ex:
            raise falcon.HTTPConflict(ex.__class__.__name__, ex.message)
        except exceptions.RepositoryException as ex:
            LOG.exception(ex)
            msg = " ".join(map(str, ex.message.args))
            raise falcon.HTTPInternalServerError('Service unavailable', msg)
        except Exception as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable', ex)

    return try_it
