package com.hpcloud.mon.domain.model.alarmhistory;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author Jonathan Halterman
 */
public enum AlarmHistoryType {
  CONFIGURATION_UPDATE, STATE_UPDATE, ACTION_UPDATE;

  @JsonCreator
  public static AlarmHistoryType fromJson(String text) {
    return valueOf(text.toUpperCase());
  }
}
