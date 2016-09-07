# (C) Copyright 2016 Hewlett Packard Enterprise Development LP
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


from monasca_api.common.repositories import notification_method_type_repository as nr
from monasca_api.common.repositories.sqla import models
from monasca_api.common.repositories.sqla import sql_repository
from sqlalchemy import MetaData
from sqlalchemy import select


class NotificationMethodTypeRepository(sql_repository.SQLRepository,
                                       nr.NotificationMethodTypeRepository):
    def __init__(self):

        super(NotificationMethodTypeRepository, self).__init__()

        metadata = MetaData()
        self.nmt = models.create_nmt_model(metadata)

        nmt = self.nmt
        self._nmt_query = select([nmt.c.name])

    @sql_repository.sql_try_catch_block
    def list_notification_method_types(self):

        with self._db_engine.connect() as conn:
            notification_method_types = conn.execute(self._nmt_query).fetchall()

            return [row[0] for row in notification_method_types]
