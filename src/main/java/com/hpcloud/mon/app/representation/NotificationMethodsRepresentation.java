package com.hpcloud.mon.app.representation;

import java.util.List;

import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod;

/**
 * Notification methods list response.
 * 
 * @author Jonathan Halterman
 */
public class NotificationMethodsRepresentation {
  public List<NotificationMethod> notificationMethods;

  public NotificationMethodsRepresentation() {
  }

  public NotificationMethodsRepresentation(List<NotificationMethod> notificationMethods) {
    this.notificationMethods = notificationMethods;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((notificationMethods == null) ? 0 : notificationMethods.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    NotificationMethodsRepresentation other = (NotificationMethodsRepresentation) obj;
    if (notificationMethods == null) {
      if (other.notificationMethods != null)
        return false;
    } else if (!notificationMethods.equals(other.notificationMethods))
      return false;
    return true;
  }
}
