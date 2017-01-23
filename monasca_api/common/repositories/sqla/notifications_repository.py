# (C) Copyright 2014,2016 Hewlett Packard Enterprise Development Company LP
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

from oslo_utils import uuidutils

from monasca_api.common.repositories import exceptions
from monasca_api.common.repositories import notifications_repository as nr
from monasca_api.common.repositories.sqla import models
from monasca_api.common.repositories.sqla import sql_repository
from sqlalchemy import MetaData, update, insert, delete
from sqlalchemy import select, bindparam, func, and_, literal_column


class NotificationsRepository(sql_repository.SQLRepository,
                              nr.NotificationsRepository):
    def __init__(self):

        super(NotificationsRepository, self).__init__()

        metadata = MetaData()
        self.nm = models.create_nm_model(metadata)

        nm = self.nm

        self._select_nm_count_name_query = (select([func.count()])
                                            .select_from(nm)
                                            .where(
                                                and_(nm.c.tenant_id == bindparam('b_tenant_id'),
                                                     nm.c.name == bindparam('b_name'))))

        self._select_nm_count_id_query = (select([func.count()])
                                          .select_from(nm)
                                          .where(
                                              and_(nm.c.tenant_id == bindparam('b_tenant_id'),
                                                   nm.c.id == bindparam('b_id'))))

        self._insert_nm_query = (insert(nm)
                                 .values(
                                     id=bindparam('b_id'),
                                     tenant_id=bindparam('b_tenant_id'),
                                     name=bindparam('b_name'),
                                     type=bindparam('b_type'),
                                     address=bindparam('b_address'),
                                     period=bindparam('b_period'),
                                     created_at=bindparam('b_created_at'),
                                     updated_at=bindparam('b_updated_at')))

        self._delete_nm_query = (delete(nm)
                                 .where(nm.c.tenant_id == bindparam('b_tenant_id'))
                                 .where(nm.c.id == bindparam('b_id')))

        self._update_nm_query = (update(nm)
                                 .where(nm.c.tenant_id == bindparam('b_tenant_id'))
                                 .where(nm.c.id == bindparam('b_id'))
                                 .values(
                                     name=bindparam('b_name'),
                                     type=bindparam('b_type'),
                                     address=bindparam('b_address'),
                                     period=bindparam('b_period'),
                                     updated_at=bindparam('b_updated_at')))

        self._select_nm_id_query = (select([nm])
                                    .where(
                                        and_(nm.c.tenant_id == bindparam('b_tenant_id'),
                                             nm.c.id == bindparam('b_id'))))

        self._select_nm_name_query = (select([nm])
                                      .where(
                                          and_(nm.c.tenant_id == bindparam('b_tenant_id'),
                                               nm.c.name == bindparam('b_name'))))

    def create_notification(self, tenant_id, name,
                            notification_type, address, period):

        with self._db_engine.connect() as conn:
            row = conn.execute(self._select_nm_count_name_query,
                               b_tenant_id=tenant_id,
                               b_name=name.encode('utf8')).fetchone()

            if int(row[0]) > 0:
                raise exceptions.AlreadyExistsException('Notification already '
                                                        'exists')

            now = datetime.datetime.utcnow()
            notification_id = uuidutils.generate_uuid()

            conn.execute(self._insert_nm_query,
                         b_id=notification_id,
                         b_tenant_id=tenant_id,
                         b_name=name.encode('utf8'),
                         b_type=notification_type.encode('utf8'),
                         b_address=address.encode('utf8'),
                         b_period=period,
                         b_created_at=now,
                         b_updated_at=now)

        return notification_id

    @sql_repository.sql_try_catch_block
    def list_notifications(self, tenant_id, sort_by, offset, limit):

        rows = []

        with self._db_engine.connect() as conn:
            nm = self.nm

            select_nm_query = (select([nm])
                               .where(nm.c.tenant_id == bindparam('b_tenant_id')))

            parms = {'b_tenant_id': tenant_id}

            if sort_by is not None:
                order_columns = [literal_column(col) for col in sort_by]
                if 'id' not in sort_by:
                    order_columns.append(nm.c.id)
            else:
                order_columns = [nm.c.id]

            select_nm_query = select_nm_query.order_by(*order_columns)

            select_nm_query = (select_nm_query
                               .order_by(nm.c.id)
                               .limit(bindparam('b_limit')))

            parms['b_limit'] = limit + 1

            if offset:
                select_nm_query = select_nm_query.offset(bindparam('b_offset'))
                parms['b_offset'] = offset

            rows = conn.execute(select_nm_query, parms).fetchall()

        return [dict(row) for row in rows]

    @sql_repository.sql_try_catch_block
    def delete_notification(self, tenant_id, _id):

        with self._db_engine.connect() as conn:

            row = conn.execute(self._select_nm_count_id_query,
                               b_tenant_id=tenant_id,
                               b_id=_id).fetchone()

            if int(row[0]) < 1:
                raise exceptions.DoesNotExistException

            conn.execute(self._delete_nm_query,
                         b_tenant_id=tenant_id,
                         b_id=_id)

    @sql_repository.sql_try_catch_block
    def list_notification(self, tenant_id, notification_id):

        with self._db_engine.connect() as conn:

            row = conn.execute(self._select_nm_id_query,
                               b_tenant_id=tenant_id,
                               b_id=notification_id).fetchone()

            if row is not None:
                return dict(row)
            else:
                raise exceptions.DoesNotExistException

    @sql_repository.sql_try_catch_block
    def find_notification_by_name(self, tenant_id, name):
        with self._db_engine.connect() as conn:
            return conn.execute(self._select_nm_name_query,
                                b_tenant_id=tenant_id,
                                b_name=name.encode('utf8')).fetchone()

    @sql_repository.sql_try_catch_block
    def update_notification(self, notification_id, tenant_id, name, notification_type, address, period):
        with self._db_engine.connect() as conn:
            now = datetime.datetime.utcnow()

            cursor = conn.execute(self._update_nm_query,
                                  b_id=notification_id,
                                  b_tenant_id=tenant_id,
                                  b_name=name.encode('utf8'),
                                  b_type=notification_type.encode('utf8'),
                                  b_address=address.encode('utf8'),
                                  b_period=period,
                                  b_updated_at=now)

            if cursor.rowcount < 1:
                raise exceptions.DoesNotExistException('Not Found')
