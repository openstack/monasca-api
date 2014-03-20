package com.hpcloud.mon.domain.model.notificationmethod;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author Jonathan Halterman
 */
public enum NotificationMethodType {
  EMAIL, SMS;

  @JsonCreator
  public static NotificationMethodType fromJson(String text) {
    return valueOf(text.toUpperCase());
  }
}
