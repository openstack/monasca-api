package com.hpcloud.mon.domain.model.alarm;

import java.util.List;

import com.hpcloud.mon.common.model.alarm.AlarmState;

/**
 * An alarm with additional detail.
 * 
 * @author Jonathan Halterman
 */
public class AlarmDetail extends Alarm {
  private List<String> alarmActions;

  public AlarmDetail() {
  }

  public AlarmDetail(String id, String name, String expression, AlarmState state,
      List<String> alarmActions) {
    super(id, name, expression, state);
    this.alarmActions = alarmActions;
  }

  public List<String> getAlarmActions() {
    return alarmActions;
  }

  public void setAlarmActions(List<String> alarmActions) {
    this.alarmActions = alarmActions;
  }
}
