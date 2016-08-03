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
package monasca.api.domain.model.notificationmethod;

import java.util.List;

import monasca.api.domain.exception.EntityNotFoundException;

/**
 * Repository for notification methods.
 */
public interface NotificationMethodRepo {
  NotificationMethod create(String tenantId, String name, String type,
      String address, int period);

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
      String type, String address, int period);

  List<NotificationMethod> find(String tenantId, List<String> sortBy, String offset, int limit);
}
