/*
 * (C) Copyright 2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package monasca.api.infrastructure.persistence.mysql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import monasca.api.domain.model.notificationmethod.NotificationMethodTypesRepo;
import monasca.api.infrastructure.persistence.PersistUtils;

/**
 * Notification method repository implementation.
 */
public class NotificationMethodTypesMySqlRepoImpl implements NotificationMethodTypesRepo {
  private static final Logger LOG = LoggerFactory
      .getLogger(NotificationMethodTypesMySqlRepoImpl.class);

  private final DBI db;
  private final PersistUtils persistUtils;

  @Inject
  public NotificationMethodTypesMySqlRepoImpl(@Named("mysql") DBI db, PersistUtils persistUtils) {
    this.db = db;
    this.persistUtils = persistUtils;
  }

  @Override
  public List<String> listNotificationMethodTypes() {

    List<String>  notification_method_types = new ArrayList<String>();
    try (Handle h = db.open()) {

      String query = " SELECT name from notification_method_type";

      Query<Map<String, Object>> q  = h.createQuery(query);
      List<Map<String, Object>>  result = q.list();

      for (Map<String, Object> m : result) {
        notification_method_types.add((String)m.get("name"));
      }
      return notification_method_types;

    }
  }

}
