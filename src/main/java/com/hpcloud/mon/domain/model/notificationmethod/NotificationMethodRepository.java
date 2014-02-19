package com.hpcloud.mon.domain.model.notificationmethod;

import java.util.List;

import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod.NotificationMethodType;

/**
 * Repository for notification methods.
 * 
 * @author Jonathan Halterman
 */
public interface NotificationMethodRepository {
  NotificationMethod create(String tenantId, String name, NotificationMethodType type,
      String address);

  /**
   * @throws EntityNotFoundException if a notification method cannot be found for the
   *           {@code notificationMethodId}
   */
  void deleteById(String tenantId, String notificationMethodId);

  boolean exists(String tenantId, String notificationMethodId);

  /**
   * @throws EntityNotFoundException if a notification method cannot be found for the
   *           {@code notificationMethodId}
   */
  NotificationMethod findById(String tenantId, String notificationMethodId);

  List<NotificationMethod> find(String tenantId);
}
