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
      AlarmState state, boolean enabled, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions) {
    super(id, name, description, expression, state, enabled);
    this.alarmActions = alarmActions;
    setOkActions(okActions);
    setUndeterminedActions(undeterminedActions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    AlarmDetail other = (AlarmDetail) obj;
    if (alarmActions == null) {
      if (other.alarmActions != null)
        return false;
    } else if (!alarmActions.equals(other.alarmActions))
      return false;
    if (okActions == null) {
      if (other.okActions != null)
        return false;
    } else if (!okActions.equals(other.okActions))
      return false;
    if (undeterminedActions == null) {
      if (other.undeterminedActions != null)
        return false;
    } else if (!undeterminedActions.equals(other.undeterminedActions))
      return false;
    return true;
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((alarmActions == null) ? 0 : alarmActions.hashCode());
    result = prime * result + ((okActions == null) ? 0 : okActions.hashCode());
    result = prime * result + ((undeterminedActions == null) ? 0 : undeterminedActions.hashCode());
    return result;
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
