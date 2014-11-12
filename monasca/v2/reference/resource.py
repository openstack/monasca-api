import falcon
from monasca.common.messaging import exceptions
from monasca.common.repositories.exceptions import DoesNotExistException
from monasca.openstack.common import log


LOG = log.getLogger(__name__)


def resource_try_catch_block(fun):

    def try_it(*args, **kwargs):

        try:

            return fun(*args, **kwargs)

        except falcon.HTTPNotFound:
            raise
        except DoesNotExistException:
            raise falcon.HTTPNotFound
        except falcon.HTTPBadRequest:
            raise
        except exceptions.RepositoryException as ex:
            LOG.exception(ex)
            msg = "".join(ex.message.args)
            raise falcon.HTTPInternalServerError('Service unavailable', msg)
        except Exception as ex:
            LOG.exception(ex)
            raise falcon.HTTPInternalServerError('Service unavailable', ex)

    return try_it
