package com.hpcloud.mon.app.representation;

import com.hpcloud.mon.domain.model.alarm.AlarmDetail;

/**
 * Alarm response.
 * 
 * @author Jonathan Halterman
 */
public class AlarmRepresentation {
  public AlarmDetail alarm;

  public AlarmRepresentation() {
  }

  public AlarmRepresentation(AlarmDetail alarm) {
    this.alarm = alarm;
  }
}
