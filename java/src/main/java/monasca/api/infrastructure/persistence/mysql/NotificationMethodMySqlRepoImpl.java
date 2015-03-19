/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
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
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import monasca.api.domain.exception.EntityExistsException;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.notificationmethod.NotificationMethod;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepo;
import monasca.api.domain.model.notificationmethod.NotificationMethodType;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.common.persistence.BeanMapper;

/**
 * Notification method repository implementation.
 */
public class NotificationMethodMySqlRepoImpl implements NotificationMethodRepo {
  private static final Logger LOG = LoggerFactory
      .getLogger(NotificationMethodMySqlRepoImpl.class);
  private final DBI db;
  private final PersistUtils persistUtils;

  @Inject
  public NotificationMethodMySqlRepoImpl(@Named("mysql") DBI db, PersistUtils persistUtils) {
    this.db = db;
    this.persistUtils = persistUtils;
  }

  @Override
  public NotificationMethod create(String tenantId, String name, NotificationMethodType type,
      String address) {
    if (exists(tenantId, name, type, address))
      throw new EntityExistsException("Notification method %s \"%s\" %s \"%s\" already exists.",
          tenantId, name, type, address);

    try (Handle h = db.open()) {
      String id = UUID.randomUUID().toString();
      h.insert(
          "insert into notification_method (id, tenant_id, name, type, address, created_at, updated_at) values (?, ?, ?, ?, ?, NOW(), NOW())",
          id, tenantId, name, type.toString(), address);
      LOG.debug("Creating notification method {} for {}", name, tenantId);
      return new NotificationMethod(id, name, type, address);
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

  public boolean exists(String tenantId, String name, NotificationMethodType type, String address) {
    try (Handle h = db.open()) {
      return h
          .createQuery(
              "select exists(select 1 from notification_method where tenant_id = :tenantId and name = :name and type = :type and address = :address)")
          .bind("tenantId", tenantId).bind("name", name).bind("type", type.toString())
          .bind("address", address).mapTo(Boolean.TYPE).first();
    }
  }

  @Override
  public List<NotificationMethod> find(String tenantId, String offset, int limit) {

    try (Handle h = db.open()) {

      String rawQuery =
          "  SELECT nm.id, nm.tenant_id, nm.name, nm.type, nm.address, nm.created_at, nm.updated_at "
          + "FROM notification_method as nm "
          + "WHERE tenant_id = :tenantId %1$s order by nm.id asc %2$s";

      String offsetPart = "";
      if (offset != null) {
        offsetPart = "and nm.id > :offset";
      }

      String limitPart = "";
      if (limit > 0) {
        limitPart = " limit :limit";
      }

      String query = String.format(rawQuery, offsetPart, limitPart);

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
      NotificationMethodType type, String address) {
    try (Handle h = db.open()) {
      if (h
          .update(
              "update notification_method set name = ?, type = ?, address = ? where tenant_id = ? and id = ?",
              name, type.name(), address, tenantId, notificationMethodId) == 0)
        throw new EntityNotFoundException("No notification method exists for %s",
            notificationMethodId);
      return new NotificationMethod(notificationMethodId, name, type, address);
    }
  }
}
