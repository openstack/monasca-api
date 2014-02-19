package com.hpcloud.mon.app.representation;

import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod;

/**
 * Notification method response.
 * 
 * @author Jonathan Halterman
 */
public class NotificationMethodRepresentation {
  public NotificationMethod notificationMethod;

  public NotificationMethodRepresentation() {
  }

  public NotificationMethodRepresentation(NotificationMethod notificationMethod) {
    this.notificationMethod = notificationMethod;
  }
}
