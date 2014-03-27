package com.hpcloud.mon.domain.model.alarmhistory;

import java.util.List;

import com.hpcloud.mon.domain.exception.EntityNotFoundException;

/**
 * Repository for alarm history.
 * 
 * @author Jonathan Halterman
 */
public interface AlarmHistoryRepository {
  /**
   * @throws EntityNotFoundException if an alarm cannot be found for the {@code alarmId}
   */
  List<AlarmHistory> findById(String tenantId, String alarmId);
}
