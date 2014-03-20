package com.hpcloud.mon.domain.model.alarm;

import java.util.Collections;
import java.util.List;

import com.hpcloud.mon.common.model.alarm.AlarmState;

/**
 * An alarm with additional detail.
 * 
 * @author Jonathan Halterman
 */
public class AlarmDetail extends Alarm {
  private List<String> alarmActions;
  private List<String> okActions;
  private List<String> undeterminedActions;

  public AlarmDetail() {
  }

  public AlarmDetail(String id, String name, String description, String expression,
      AlarmState state, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions) {
    super(id, name, description, expression, state);
    this.alarmActions = alarmActions;
    setOkActions(okActions);
    setUndeterminedActions(undeterminedActions);
  }

  public List<String> getAlarmActions() {
    return alarmActions;
  }

  public List<String> getOkActions() {
    return okActions;
  }

  public List<String> getUndeterminedActions() {
    return undeterminedActions;
  }

  public void setAlarmActions(List<String> alarmActions) {
    this.alarmActions = alarmActions;
  }

  public void setOkActions(List<String> okActions) {
    this.okActions = okActions == null ? Collections.<String>emptyList() : okActions;
  }

  public void setUndeterminedActions(List<String> undeterminedActions) {
    this.undeterminedActions = undeterminedActions == null ? Collections.<String>emptyList()
        : undeterminedActions;
  }
}
