# Copyright 2015 Hewlett-Packard
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
import uuid

import MySQLdb
from oslo_utils import timeutils

from monasca.common.repositories import constants
from monasca.common.repositories import exceptions
from monasca.common.repositories.mysql import mysql_repository
from monasca.common.repositories import streams_repository as sdr
from monasca.openstack.common import log


LOG = log.getLogger(__name__)


class StreamsRepository(mysql_repository.MySQLRepository,
                        sdr.StreamsRepository):

    base_query = """
          select sd.id, sd.tenant_id, sd.name, sd.description,
            sd.select_by, sd.group_by, sd.fire_criteria, sd.expiration,
            sd.actions_enabled, sd.created_at,
            sd.updated_at, sd.deleted_at,
            saf.fire_actions, sae.expire_actions
          from stream_definition as sd
          left join (select stream_definition_id,
           group_concat(action_id) as fire_actions
              from stream_actions
              where action_type = 'FIRE'
              group by stream_definition_id) as saf
              on saf.stream_definition_id = sd.id
          left join (select stream_definition_id,
            group_concat(action_id) as expire_actions
              from stream_actions
              where action_type = 'EXPIRE'
              group by stream_definition_id) as sae
              on sae.stream_definition_id = sd.id
        """

    def __init__(self):

        super(StreamsRepository, self).__init__()

    @mysql_repository.mysql_try_catch_block
    def get_stream_definition(self, tenant_id, stream_definition_id):

        parms = [tenant_id, stream_definition_id]

        where_clause = """ where sd.tenant_id = %s
                            and sd.id = %s
                            and deleted_at is NULL """

        query = StreamsRepository.base_query + where_clause

        rows = self._execute_query(query, parms)

        if rows:
            return rows[0]
        else:
            raise exceptions.DoesNotExistException

    @mysql_repository.mysql_try_catch_block
    def get_stream_definitions(self, tenant_id, name, offset, limit):

        parms = [tenant_id]

        select_clause = StreamsRepository.base_query

        where_clause = " where sd.tenant_id = %s and deleted_at is NULL "

        if name:
            where_clause += " and sd.name = %s "
            parms.append(name.encode('utf8'))

        if offset is not None:
            order_by_clause = " order by sd.id, sd.created_at "
            where_clause += " and sd.id > %s "
            parms.append(offset.encode('utf8'))
            limit_clause = " limit %s "
            parms.append(constants.PAGE_LIMIT)
        else:
            order_by_clause = " order by sd.created_at "
            limit_clause = ""

        query = select_clause + where_clause + order_by_clause + limit_clause

        return self._execute_query(query, parms)

    @mysql_repository.mysql_try_catch_block
    def delete_stream_definition(self, tenant_id, stream_definition_id):
        """Delete the stream definition.

        :param tenant_id:
        :param stream_definition_id:
        :returns True: -- if stream definition exists and was deleted.
        :returns False: -- if the stream definition does not exists.
        :raises RepositoryException:
        """

        cnxn, cursor = self._get_cnxn_cursor_tuple()

        with cnxn:

            cursor.execute("""delete from stream_definition
                           where tenant_id = %s and id = %s""",
                           [tenant_id, stream_definition_id])

            if cursor.rowcount < 1:
                return False

            return True

    @mysql_repository.mysql_try_catch_block
    def create_stream_definition(self,
                                 tenant_id,
                                 name,
                                 description,
                                 select,
                                 group_by,
                                 fire_criteria,
                                 expiration,
                                 fire_actions,
                                 expire_actions):
        cnxn, cursor = self._get_cnxn_cursor_tuple()

        with cnxn:

            now = timeutils.utcnow()
            stream_definition_id = str(uuid.uuid1())
            try:
                cursor.execute("""insert into stream_definition(
                                   id,
                                   tenant_id,
                                   name,
                                   description,
                                   select_by,
                                   group_by,
                                   fire_criteria,
                                   expiration,
                                   created_at,
                                   updated_at)
                                   values (%s, %s, %s, %s, %s, %s, %s, %s, %s,
                                   %s)""", (
                    stream_definition_id, tenant_id, name.encode('utf8'),
                    description.encode('utf8'), select.encode('utf8'),
                    group_by.encode('utf8'), fire_criteria.encode('utf8'),
                    expiration, now, now))
            except MySQLdb.IntegrityError as e:
                code, msg = e
                if code == 1062:
                    raise exceptions.AlreadyExistsException(
                        'Stream Definition already '
                        'exists for tenant_id: {0} name: {1}'.format(
                            tenant_id, name.encode('utf8')))
                else:
                    raise e

            self._insert_into_stream_actions(cursor, stream_definition_id,
                                             fire_actions, u"FIRE")
            self._insert_into_stream_actions(cursor, stream_definition_id,
                                             expire_actions,
                                             u"EXPIRE")

            return stream_definition_id

    def _insert_into_stream_actions(self, cursor, stream_definition_id,
                                    actions, action_type):

        if actions is None:
            return

        for action in actions:
            cursor.execute("select id from notification_method where id = %s",
                           (action.encode('utf8'),))
            row = cursor.fetchone()
            if not row:
                raise exceptions.RepositoryException(
                    "Non-existent notification id {} submitted for {} "
                    "notification action".format(action.encode('utf8'),
                                                 action_type.encode('utf8')))
            cursor.execute("""insert into stream_actions(
                               stream_definition_id,
                               action_type,
                               action_id)
                               values(%s,%s,%s)""", (
                stream_definition_id, action_type.encode('utf8'),
                action.encode('utf8')))
