# Copyright 2015 Hewlett-Packard
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

from monasca.common.repositories import constants
from monasca.common.repositories import events_repository as er
from monasca.common.repositories.mysql import mysql_repository
from monasca.openstack.common import log


LOG = log.getLogger(__name__)


class EventsRepository(mysql_repository.MySQLRepository,
                       er.EventsRepository):

    def __init__(self):
        super(EventsRepository, self).__init__()

        self.database_name = "winchester"
        self._base_query = """
            select event.message_id,
                   event.generated,
                   event_type.desc,
                   trait.name,
                   trait.t_string,
                   trait.t_float,
                   trait.t_int,
                   trait.t_datetime
            from event
            inner join event_type on event.event_type_id=event_type.id
            inner join trait on event.id=trait.event_id"""

    @mysql_repository.mysql_try_catch_block
    def list_event(self, tenant_id, event_id):
        query = self._base_query + " where event.message_id=%s"
        rows = self._execute_query(query, [event_id])
        return rows

    @mysql_repository.mysql_try_catch_block
    def list_events(self, tenant_id, offset, limit):
        where_clause = ""
        order_by_clause = " order by event.generated asc"

        event_ids = self._find_event_ids(offset, limit)

        if event_ids:
            ids = ",".join([str(event_id['id']) for event_id in event_ids])

            where_clause = """
                where trait.event_id
                IN ({})""".format(ids)

        query = self._base_query + where_clause + order_by_clause

        rows = self._execute_query(query, [])

        return rows

    def _find_event_ids(self, offset, limit):
        if not limit:
            limit = constants.PAGE_LIMIT

        parameters = []

        if offset:
            parameters.append(offset.encode('utf8'))
            offset_clause = """
                where generated > (select generated
                                   from event
                                   where message_id = %s)"""
        else:
            offset_clause = ""

        parameters.append(int(limit))
        limit_clause = " limit %s"

        id_query = ('select id from event ' +
                    offset_clause +
                    ' order by generated ' +
                    limit_clause)

        return self._execute_query(id_query, parameters)
