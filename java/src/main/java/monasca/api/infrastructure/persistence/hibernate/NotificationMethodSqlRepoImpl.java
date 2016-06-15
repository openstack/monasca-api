/*
 * Copyright 2015 FUJITSU LIMITED
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
package monasca.api.infrastructure.persistence.hibernate;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import monasca.api.domain.exception.EntityExistsException;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.notificationmethod.NotificationMethod;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepo;
import monasca.common.hibernate.db.NotificationMethodDb;
import monasca.common.model.alarm.AlarmNotificationMethodType;

/**
 * Notification method repository implementation.
 */
public class NotificationMethodSqlRepoImpl
    extends BaseSqlRepo
    implements NotificationMethodRepo {
  private static final Joiner COMMA_JOINER = Joiner.on(',');
  private static final Logger LOG = LoggerFactory.getLogger(NotificationMethodSqlRepoImpl.class);

  @Inject
  public NotificationMethodSqlRepoImpl(@Named("orm") SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  @Override
  public NotificationMethod create(String tenantId, String name, String notificationMethodType,
                                   String address, int period) {
    Transaction tx = null;
    Session session = null;
    try {
      session = sessionFactory.openSession();
      tx = session.beginTransaction();

      if (byTenantIdAndName(session, tenantId, name) != null) {
        throw new EntityExistsException("Notification method %s \"%s\" already exists.", tenantId,
            name);
      }

      final String id = UUID.randomUUID().toString();
      final DateTime now = this.getUTCNow();
      final NotificationMethodDb db = new NotificationMethodDb(
          id,
          tenantId,
          name,
          AlarmNotificationMethodType.valueOf(notificationMethodType),
          address,
          period,
          now,
          now
      );
      session.save(db);

      LOG.debug("Creating notification method {} for {}", name, tenantId);
      tx.commit();
      tx = null;

      return this.convertToNotificationMethod(db);
    } catch (RuntimeException e) {
      this.rollbackIfNotNull(tx);
      throw e;
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Override
  public void deleteById(String tenantId, String notificationMethodId) {
    Session session = null;
    Transaction tx = null;
    try {
      if (!exists(tenantId, notificationMethodId)) {
        throw new EntityNotFoundException("No notification exists for %s", notificationMethodId);
      }
      session = sessionFactory.openSession();
      tx = session.beginTransaction();

      // delete notification
      session
          .getNamedQuery(NotificationMethodDb.Queries.DELETE_BY_ID)
          .setString("id", notificationMethodId)
          .executeUpdate();

      tx.commit();
      tx = null;
    } catch (RuntimeException e) {
      this.rollbackIfNotNull(tx);
      throw e;
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Override
  public boolean exists(String tenantId, String notificationMethodId) {
    Session session = null;
    try {
      session = sessionFactory.openSession();
      return this.getByTenantIdAndId(session, tenantId, notificationMethodId) != null;
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Override
  public NotificationMethod findById(String tenantId, String notificationMethodId) {
    Session session = null;
    try {
      session = sessionFactory.openSession();

      final NotificationMethodDb result = this.getByTenantIdAndId(session, tenantId, notificationMethodId);

      if (result == null) {
        throw new EntityNotFoundException("No notification method exists for %s",
            notificationMethodId);
      }

      return this.convertToNotificationMethod(result);
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Override
  public NotificationMethod update(String tenantId, String notificationMethodId, String name,
                                   String notificationMethodType, String address, int period) {
    Session session = null;
    Transaction tx = null;
    try {
      session = sessionFactory.openSession();
      final NotificationMethodDb result = this.byTenantIdAndName(session, tenantId, name);

      if (result != null && !result.getId().equalsIgnoreCase(notificationMethodId)) {
        throw new EntityExistsException("Notification method %s \"%s\" already exists.", tenantId,
            name);
      }

      tx = session.beginTransaction();

      NotificationMethodDb db;
      if ((db = session.get(NotificationMethodDb.class, notificationMethodId)) == null) {
        throw new EntityNotFoundException("No notification method exists for %s",
            notificationMethodId);
      }
      db.setName(name);
      db.setType(AlarmNotificationMethodType.valueOf(notificationMethodType));
      db.setAddress(address);
      db.setPeriod(period);
      db.setUpdatedAt(this.getUTCNow());

      session.save(db);
      tx.commit();
      tx = null;

      return this.convertToNotificationMethod(db);

    } catch (RuntimeException e) {
      this.rollbackIfNotNull(tx);
      throw e;
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<NotificationMethod> find(String tenantId, List<String> sortBy, String offset,
                                       int limit) {
    Session session = null;
    List<NotificationMethodDb> resultList;
    List<NotificationMethod> notificationList = Lists.newArrayList();
    final String rawQuery = "from NotificationMethodDb where tenant_id = :tenantId %1$s";

    try {
      session = sessionFactory.openSession();

      final StringBuilder orderByPart = new StringBuilder();
      if (sortBy != null && !sortBy.isEmpty()) {
        orderByPart.append(" order by ").append(COMMA_JOINER.join(sortBy));
        if (!sortBy.contains("id")) {
          orderByPart.append(",id");
        }
      } else {
        orderByPart.append(" order by id ");
      }

      final String queryHql = String.format(rawQuery, orderByPart);
      final Query query = session.createQuery(queryHql).setString("tenantId", tenantId);

      if (limit > 0) {
        query.setMaxResults(limit + 1);
      }

      if (offset != null && !offset.isEmpty()) {
        query.setFirstResult(Integer.parseInt(offset));
      }

      resultList = query.list();

      if (CollectionUtils.isEmpty(resultList)) {
        return notificationList;
      }

      for (NotificationMethodDb item : resultList) {
        notificationList.add(this.convertToNotificationMethod(item));
      }

      return notificationList;

    } finally {
      if (session != null) {
        session.close();
      }
    }

  }

  protected NotificationMethodDb byTenantIdAndName(final Session session,
                                                   final String tenantId,
                                                   final String name) {

    return (NotificationMethodDb) session
        .getNamedQuery(NotificationMethodDb.Queries.NOTIFICATION_BY_TENANT_ID_AND_NAME)
        .setString("tenantId", tenantId)
        .setString("name", name)
        .uniqueResult();
  }

  protected NotificationMethodDb getByTenantIdAndId(final Session session,
                                                    final String tenantId,
                                                    final String id) {

    return (NotificationMethodDb) session
        .getNamedQuery(NotificationMethodDb.Queries.FIND_BY_TENANT_ID_AND_ID)
        .setString("tenantId", tenantId)
        .setString("id", id)
        .uniqueResult();
  }

  protected NotificationMethod convertToNotificationMethod(final NotificationMethodDb db) {
    return db == null ? null : new NotificationMethod(
        db.getId(),
        db.getName(),
        db.getType().toString(),
        db.getAddress(),
        db.getPeriod()
    );
  }
}
