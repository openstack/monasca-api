package com.hpcloud.mon.infrastructure.persistence;

import java.util.List;

import javax.inject.Inject;

import org.skife.jdbi.v2.DBI;

import com.hpcloud.mon.domain.model.alarmhistory.AlarmHistory;
import com.hpcloud.mon.domain.model.alarmhistory.AlarmHistoryRepository;

/**
 * Alarm repository implementation.
 * 
 * @author Jonathan Halterman
 */
public class AlarmHistoryRepositoryImpl implements AlarmHistoryRepository {
  private final DBI db;

  @Inject
  public AlarmHistoryRepositoryImpl(DBI db) {
    this.db = db;
  }

  @Override
  public List<AlarmHistory> findById(String tenantId, String alarmId) {
    return null;
  }
}