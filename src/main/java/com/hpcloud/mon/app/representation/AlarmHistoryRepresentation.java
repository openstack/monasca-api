package com.hpcloud.mon.app.representation;

import com.hpcloud.mon.domain.model.alarm.AlarmDetail;

/**
 * Alarm history response.
 * 
 * @author Jonathan Halterman
 */
public class AlarmHistoryRepresentation {
  public AlarmDetail alarm;

  public AlarmHistoryRepresentation() {
  }

  public AlarmHistoryRepresentation(AlarmDetail alarm) {
    this.alarm = alarm;
  }
}
