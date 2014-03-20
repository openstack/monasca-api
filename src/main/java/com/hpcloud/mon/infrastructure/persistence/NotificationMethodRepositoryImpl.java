package com.hpcloud.mon.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hpcloud.mon.domain.exception.EntityExistsException;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodType;
import com.hpcloud.persistence.BeanMapper;

/**
 * Notification method repository implementation.
 * 
 * @author Jonathan Halterman
 */
public class NotificationMethodRepositoryImpl implements NotificationMethodRepository {
  private static final Logger LOG = LoggerFactory.getLogger(NotificationMethodRepositoryImpl.class);
  private final DBI db;

  @Inject
  public NotificationMethodRepositoryImpl(DBI db) {
    this.db = db;
  }

  @Override
  public NotificationMethod create(String tenantId, String name, NotificationMethodType type,
      String address) {
    if (exists(tenantId, name, type, address))
      throw new EntityExistsException("Notification method %s \"%s\" %s \"%s\" already exists.",
          tenantId, name, type, address);

    Handle h = db.open();

    try {
      String id = UUID.randomUUID().toString();
      h.begin();
      h.insert(
          "insert into notification_method (id, tenant_id, name, type, address, created_at, updated_at) values (?, ?, ?, ?, ?, NOW(), NOW())",
          id, tenantId, name, type.toString(), address);
      h.commit();
      LOG.debug("Creating notification method {} for {}", name, tenantId);
      return new NotificationMethod(id, name, type, address);
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }

  @Override
  public void deleteById(String tenantId, String notificationMethodId) {
    Handle h = db.open();

    try {
      if (h.update("delete from notification_method where tenant_id = ? and id = ?", tenantId,
          notificationMethodId) == 0)
        throw new EntityNotFoundException("No notification method exists for %s",
            notificationMethodId);
    } finally {
      h.close();
    }
  }

  @Override
  public boolean exists(String tenantId, String notificationMethodId) {
    Handle h = db.open();

    try {
      return h.createQuery(
          "select exists(select 1 from notification_method where tenant_id = :tenantId and id = :notificationMethodId)")
          .bind("tenantId", tenantId)
          .bind("notificationMethodId", notificationMethodId)
          .mapTo(Boolean.TYPE)
          .first();
    } finally {
      h.close();
    }
  }

  public boolean exists(String tenantId, String name, NotificationMethodType type, String address) {
    Handle h = db.open();

    try {
      return h.createQuery(
          "select exists(select 1 from notification_method where tenant_id = :tenantId and name = :name and type = :type and address = :address)")
          .bind("tenantId", tenantId)
          .bind("name", name)
          .bind("type", type.toString())
          .bind("address", address)
          .mapTo(Boolean.TYPE)
          .first();
    } finally {
      h.close();
    }
  }

  @Override
  public List<NotificationMethod> find(String tenantId) {
    Handle h = db.open();

    try {
      return h.createQuery("select * from notification_method where tenant_id = :tenantId")
          .bind("tenantId", tenantId)
          .map(new BeanMapper<NotificationMethod>(NotificationMethod.class))
          .list();
    } finally {
      h.close();
    }
  }

  @Override
  public NotificationMethod findById(String tenantId, String notificationMethodId) {
    Handle h = db.open();

    try {
      NotificationMethod notificationMethod = h.createQuery(
          "select * from notification_method where tenant_id = :tenantId and id = :id")
          .bind("tenantId", tenantId)
          .bind("id", notificationMethodId)
          .map(new BeanMapper<NotificationMethod>(NotificationMethod.class))
          .first();

      if (notificationMethod == null)
        throw new EntityNotFoundException("No notification method exists for %s",
            notificationMethodId);

      return notificationMethod;
    } finally {
      h.close();
    }
  }

  @Override
  public NotificationMethod update(String tenantId, String notificationMethodId, String name,
      NotificationMethodType type, String address) {
    Handle h = db.open();

    try {
      if (h.update(
          "update notification_method set name = ?, type = ?, address = ? where tenant_id = ? and id = ?",
          name, type.name(), address, tenantId, notificationMethodId) == 0)
        throw new EntityNotFoundException("No notification method exists for %s",
            notificationMethodId);
      return new NotificationMethod(notificationMethodId, name, type, address);
    } finally {
      h.close();
    }
  }
}
