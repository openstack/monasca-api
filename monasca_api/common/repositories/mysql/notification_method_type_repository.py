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


from monasca_common.repositories.mysql import mysql_repository

from monasca_api.common.repositories import notification_method_type_repository as nr


class NotificationMethodTypeRepository(mysql_repository.MySQLRepository,
                                       nr.NotificationMethodTypeRepository):
    def __init__(self):

        super(NotificationMethodTypeRepository, self).__init__()

    @mysql_repository.mysql_try_catch_block
    def list_notification_method_types(self):
        query = "select name from notification_method_type"
        rows = self._execute_query(query)
        return rows
