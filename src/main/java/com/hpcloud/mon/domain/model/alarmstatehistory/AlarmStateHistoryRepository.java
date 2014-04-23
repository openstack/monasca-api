package com.hpcloud.mon.domain.model.alarmstatehistory;

import java.util.List;

import com.hpcloud.mon.domain.exception.EntityNotFoundException;

/**
 * Repository for alarm state history.
 * 
 * @author Jonathan Halterman
 */
public interface AlarmStateHistoryRepository {
  /**
   * @throws EntityNotFoundException if an alarm cannot be found for the {@code alarmId}
   */
  List<AlarmStateHistory> findById(String tenantId, String alarmId);
}
