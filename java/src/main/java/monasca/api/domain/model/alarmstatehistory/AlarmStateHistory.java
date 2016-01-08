/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package monasca.api.domain.model.alarmstatehistory;

import java.util.List;

import monasca.common.model.alarm.AlarmTransitionSubAlarm;
import org.joda.time.DateTime;

import monasca.common.model.alarm.AlarmState;
import monasca.common.model.domain.common.AbstractEntity;
import monasca.common.model.metric.MetricDefinition;
import monasca.common.util.Conversions;

public class AlarmStateHistory  extends AbstractEntity {
  private String alarmId;
  private List<MetricDefinition> metrics;
  private AlarmState oldState;
  private AlarmState newState;
  private String reason;
  private String reasonData;
  private DateTime timestamp;
  private List<AlarmTransitionSubAlarm> subAlarms;

  public AlarmStateHistory() {}

  public AlarmStateHistory(
      String alarmId,
      List<MetricDefinition> metrics,
      AlarmState oldState,
      AlarmState newState,
      List<AlarmTransitionSubAlarm> subAlarms,
      String reason,
      String reasonData,
      DateTime timestamp) {
    this.alarmId = alarmId;
    this.setMetrics(metrics);
    this.oldState = oldState;
    this.newState = newState;
    this.subAlarms = subAlarms;
    this.reason = reason;
    this.reasonData = reasonData;
    this.timestamp = Conversions.variantToDateTime(timestamp);
    this.id = timestamp.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof AlarmStateHistory))
      return false;
    AlarmStateHistory other = (AlarmStateHistory) obj;
    if (alarmId == null) {
      if (other.alarmId != null)
        return false;
    } else if (!alarmId.equals(other.alarmId))
      return false;
    if (metrics == null) {
      if (other.metrics != null)
        return false;
    } else if (!metrics.equals(other.metrics))
      return false;
    if (newState != other.newState)
      return false;
    if (oldState != other.oldState)
      return false;
    if (subAlarms == null) {
      if (other.subAlarms != null)
        return false;
      } else if (!subAlarms.equals(other.subAlarms))
      return false;
    if (reason == null) {
      if (other.reason != null)
        return false;
    } else if (!reason.equals(other.reason))
      return false;
    if (reasonData == null) {
      if (other.reasonData != null)
        return false;
    } else if (!reasonData.equals(other.reasonData))
      return false;
    if (timestamp == null) {
      if (other.timestamp != null)
        return false;
    } else if (!timestamp.equals(other.timestamp))
      return false;
    return true;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAlarmId() {
    return alarmId;
  }

  public List<MetricDefinition> getMetrics() {
    return metrics;
  }

  public AlarmState getNewState() {
    return newState;
  }

  public AlarmState getOldState() {
    return oldState;
  }

  public String getReason() {
    return reason;
  }

  public String getReasonData() {
    return reasonData;
  }

  public DateTime getTimestamp() {
    return timestamp;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((alarmId == null) ? 0 : alarmId.hashCode());
    result = prime * result + ((metrics == null) ? 0 : metrics.hashCode());
    result = prime * result + ((newState == null) ? 0 : newState.hashCode());
    result = prime * result + ((oldState == null) ? 0 : oldState.hashCode());
    result = prime * result + ((subAlarms == null) ? 0 : subAlarms.hashCode());
    result = prime * result + ((reason == null) ? 0 : reason.hashCode());
    result = prime * result + ((reasonData == null) ? 0 : reasonData.hashCode());
    result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
    return result;
  }

  public void setAlarmId(String alarmId) {
    this.alarmId = alarmId;
  }

  public void setMetrics(List<MetricDefinition> metrics) {
    this.metrics = metrics;
  }

  public void setNewState(AlarmState newState) {
    this.newState = newState;
  }

  public void setOldState(AlarmState oldState) {
    this.oldState = oldState;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public void setReasonData(String reasonData) {
    this.reasonData = reasonData;
  }

  public List<AlarmTransitionSubAlarm> getSubAlarms() {
    return subAlarms;
  }

  public void setSubAlarms(List<AlarmTransitionSubAlarm> subAlarms) {
    this.subAlarms = subAlarms;
  }

  public void setTimestamp(DateTime timestamp) {
    this.timestamp = Conversions.variantToDateTime(timestamp);
    // Set the id in the AbstractEntity class.
    id = timestamp.toString();
  }

  @Override
  public String toString() {
    return "AlarmStateHistory [alarmId=" + alarmId + ", metrics=" + metrics + ", oldState="
        + oldState + ", newState=" + newState + ", subAlarms=" + subAlarms + ", reason=" + reason + ", reasonData=" + reasonData
        + ", timestamp=" + timestamp + "]";
  }
}
