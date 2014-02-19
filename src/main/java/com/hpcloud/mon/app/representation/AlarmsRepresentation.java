package com.hpcloud.mon.app.representation;

import java.util.List;

import com.hpcloud.mon.domain.model.alarm.Alarm;

/**
 * Alarms list response.
 * 
 * @author Jonathan Halterman
 */
public class AlarmsRepresentation {
  public List<Alarm> alarms;

  public AlarmsRepresentation() {
  }

  public AlarmsRepresentation(List<Alarm> alarms) {
    this.alarms = alarms;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((alarms == null) ? 0 : alarms.hashCode());
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
    AlarmsRepresentation other = (AlarmsRepresentation) obj;
    if (alarms == null) {
      if (other.alarms != null)
        return false;
    } else if (!alarms.equals(other.alarms))
      return false;
    return true;
  }
}
