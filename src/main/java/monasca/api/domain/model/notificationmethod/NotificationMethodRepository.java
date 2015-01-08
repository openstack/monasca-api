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
package monasca.api.domain.model.notificationmethod;

import java.util.List;

import monasca.api.domain.exception.EntityNotFoundException;

/**
 * Repository for notification methods.
 */
public interface NotificationMethodRepository {
  NotificationMethod create(String tenantId, String name, NotificationMethodType type,
      String address);

  /**
   * @throws EntityNotFoundException if a notification method cannot be found for the
   *         {@code notificationMethodId}
   */
  void deleteById(String tenantId, String notificationMethodId);

  /** Returns whether the {@code notificationMethodId} exists for the {@code tenantId}. */
  boolean exists(String tenantId, String notificationMethodId);

  /**
   * @throws EntityNotFoundException if a notification method cannot be found for the
   *         {@code notificationMethodId}
   */
  NotificationMethod findById(String tenantId, String notificationMethodId);

  /**
   * @throws EntityNotFoundException if a notification method cannot be found for the
   *         {@code notificationMethodId}
   */
  NotificationMethod update(String tenantId, String notificationMethodId, String name,
      NotificationMethodType type, String address);

  List<NotificationMethod> find(String tenantId, String offset);
}
