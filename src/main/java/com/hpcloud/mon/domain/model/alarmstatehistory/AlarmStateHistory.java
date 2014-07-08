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
package com.hpcloud.mon.domain.model.alarmstatehistory;

import org.joda.time.DateTime;

import com.hpcloud.mon.common.model.alarm.AlarmState;

public class AlarmStateHistory {
  private String alarmId;
  private AlarmState oldState;
  private AlarmState newState;
  private String reason;
  private String reasonData;
  private DateTime timestamp;

  public AlarmStateHistory() {}

  public AlarmStateHistory(String alarmId, AlarmState oldState, AlarmState newState, String reason,
      String reasonData, DateTime timestamp) {
    this.alarmId = alarmId;
    this.oldState = oldState;
    this.newState = newState;
    this.reason = reason;
    this.reasonData = reasonData;
    this.timestamp = timestamp;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AlarmStateHistory other = (AlarmStateHistory) obj;
    if (alarmId == null) {
      if (other.alarmId != null)
        return false;
    } else if (!alarmId.equals(other.alarmId))
      return false;
    if (newState != other.newState)
      return false;
    if (oldState != other.oldState)
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

  public String getAlarmId() {
    return alarmId;
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
    result = prime * result + ((newState == null) ? 0 : newState.hashCode());
    result = prime * result + ((oldState == null) ? 0 : oldState.hashCode());
    result = prime * result + ((reason == null) ? 0 : reason.hashCode());
    result = prime * result + ((reasonData == null) ? 0 : reasonData.hashCode());
    result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
    return result;
  }

  public void setAlarmId(String alarmId) {
    this.alarmId = alarmId;
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

  public void setTimestamp(DateTime timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return String
        .format(
            "AlarmStateHistory [alarmId=%s, oldState=%s, newState=%s, reason=%s, reasonData=%s, timestamp=%s]",
            alarmId, oldState, newState, reason, reasonData, timestamp);
  }
}
