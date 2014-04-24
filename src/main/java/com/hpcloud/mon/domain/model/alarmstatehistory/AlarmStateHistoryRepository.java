package com.hpcloud.mon.domain.model.alarmstatehistory;

import java.util.List;

import org.joda.time.DateTime;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;

/**
 * Repository for alarm state history.
 * 
 * @author Jonathan Halterman
 */
public interface AlarmStateHistoryRepository {
  /**
   * Creates a new AlarmStateHistory record.
   */
  void create(String tenantId, String alarmId, AlarmState oldState, AlarmState newState,
      String reason, String reasonData, DateTime timestamp);

  /**
   * @throws EntityNotFoundException if an alarm cannot be found for the {@code alarmId}
   */
  List<AlarmStateHistory> findById(String tenantId, String alarmId);
}
