/*
 * (C) Copyright 2014-2016 Hewlett Packard Enterprise Development LP
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import monasca.api.domain.exception.EntityExistsException;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.notificationmethod.NotificationMethod;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepo;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.common.persistence.BeanMapper;

/**
 * Notification method repository implementation.
 */
public class NotificationMethodMySqlRepoImpl implements NotificationMethodRepo {
  private static final Logger LOG = LoggerFactory
      .getLogger(NotificationMethodMySqlRepoImpl.class);
  private static final Joiner COMMA_JOINER = Joiner.on(',');
  private final DBI db;
  private final PersistUtils persistUtils;

  @Inject
  public NotificationMethodMySqlRepoImpl(@Named("mysql") DBI db, PersistUtils persistUtils) {
    this.db = db;
    this.persistUtils = persistUtils;
  }

  @Override
  public NotificationMethod create(String tenantId, String name,
      String notificationMethodType, String address, int period) {
    Handle h = db.open();
    try {
      h.begin();
      if (getNotificationIdForTenantIdAndName(h,tenantId, name) != null)
        throw new EntityExistsException(
            "Notification method %s \"%s\" already exists.", tenantId, name);

      if (!isValidNotificationMethodType(h, notificationMethodType)) {
        throw new EntityNotFoundException("Not a valid notification method type %s ", notificationMethodType);
      }

      String id = UUID.randomUUID().toString();
      h.insert(
          "insert into notification_method (id, tenant_id, name, type, address, period, created_at, updated_at) values (?, ?, ?, ?, ?, ?, NOW(), NOW())",
          id, tenantId, name, notificationMethodType, address, period);
      LOG.debug("Creating notification method {} for {}", name, tenantId);
      h.commit();
      return new NotificationMethod(id, name, notificationMethodType, address, period);
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }

  @Override
  public void deleteById(String tenantId, String notificationMethodId) {
    try (Handle h = db.open()) {
      if (h.update("delete from notification_method where tenant_id = ? and id = ?", tenantId,
          notificationMethodId) == 0)
        throw new EntityNotFoundException("No notification method exists for %s",
            notificationMethodId);
    }
  }

  @Override
  public boolean exists(String tenantId, String notificationMethodId) {
    try (Handle h = db.open()) {
      return h
          .createQuery(
              "select exists(select 1 from notification_method where tenant_id = :tenantId and id = :notificationMethodId)")
          .bind("tenantId", tenantId).bind("notificationMethodId", notificationMethodId)
          .mapTo(Boolean.TYPE).first();
    }
  }

  private String getNotificationIdForTenantIdAndName(Handle h,String tenantId, String name) {
    Map<String, Object> map = h
        .createQuery(
            "select id from notification_method where tenant_id = :tenantId and name = :name")
        .bind("tenantId", tenantId).bind("name", name).first();
    if (map != null && !map.isEmpty()) {
      return map.get("id").toString();
    }
    else {
      return null;
    }
  }

  private boolean isValidNotificationMethodType(Handle h ,String notificationMethod){

   String query = "  SELECT name from notification_method_type";

   Query<Map<String, Object>> q  = h.createQuery(query);
   List<Map<String, Object>>  result = q.list();


   for (Map<String, Object> m : result) {
     String method = (String)m.get("name");
     if (method.equalsIgnoreCase(notificationMethod))
       return true;
     }
   return false;
  }


  @Override
  public List<NotificationMethod> find(String tenantId, List<String> sortBy, String offset,
                                       int limit) {

    try (Handle h = db.open()) {

      String rawQuery =
          "  SELECT nm.id, nm.tenant_id, nm.name, nm.type, nm.address, nm.period, nm.created_at, nm.updated_at "
          + "FROM notification_method as nm "
          + "WHERE tenant_id = :tenantId %1$s %2$s %3$s";

      String offsetPart = "";
      if (offset != null) {
        offsetPart = "and nm.id > :offset";
      }

      String orderByPart = "";
      if (sortBy != null && !sortBy.isEmpty()) {
        orderByPart = " order by " + COMMA_JOINER.join(sortBy);
        if (!orderByPart.contains("id")) {
          orderByPart = orderByPart + ",id";
        }
      } else {
        orderByPart = " order by id ";
      }

      String limitPart = "";
      if (limit > 0) {
        limitPart = " limit :limit";
      }

      String query = String.format(rawQuery, offsetPart, orderByPart, limitPart);

      Query<?> q = h.createQuery(query);

      q.bind("tenantId", tenantId);

      if (offset != null) {
        q.bind("offset", offset);
      }

      if (limit > 0) {
        q.bind("limit", limit + 1);
      }

      return q.map(new BeanMapper<>(NotificationMethod.class)).list();

    }
  }

  @Override
  public NotificationMethod findById(String tenantId, String notificationMethodId) {
    try (Handle h = db.open()) {
      NotificationMethod notificationMethod =
          h.createQuery(
              "select * from notification_method where tenant_id = :tenantId and id = :id")
              .bind("tenantId", tenantId).bind("id", notificationMethodId)
              .map(new BeanMapper<NotificationMethod>(NotificationMethod.class)).first();

      if (notificationMethod == null)
        throw new EntityNotFoundException("No notification method exists for %s",
            notificationMethodId);

      return notificationMethod;
    }
  }

  @Override
  public NotificationMethod update(String tenantId, String notificationMethodId, String name,
      String notificationMethodType, String address, int period) {
    Handle h = db.open();
    try {
      h.begin();
      String notificationID = getNotificationIdForTenantIdAndName(h,tenantId, name);
      if (notificationID != null && !notificationID.equalsIgnoreCase(notificationMethodId)) {
        throw new EntityExistsException("Notification method %s \"%s\" already exists.",
                  tenantId, name);
      }
      if (!isValidNotificationMethodType(h, notificationMethodType)) {
        throw new EntityNotFoundException("Not a valid notification method type %s ", notificationMethodType);
      }

      if (h
          .update(
              "update notification_method set name = ?, type = ?, address = ?, period = ?, updated_at = NOW() "
              + "where tenant_id = ? and id = ?",
              name, notificationMethodType, address, period, tenantId, notificationMethodId) == 0)
        throw new EntityNotFoundException("No notification method exists for %s",
            notificationMethodId);
      h.commit();
      return new NotificationMethod(notificationMethodId, name, notificationMethodType, address, period);
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }
}
