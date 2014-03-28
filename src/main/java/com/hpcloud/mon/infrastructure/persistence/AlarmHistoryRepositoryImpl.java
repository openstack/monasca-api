package com.hpcloud.mon.infrastructure.persistence;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

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
  public AlarmHistoryRepositoryImpl(@Named("vertica") DBI db) {
    this.db = db;
  }

  @Override
  public List<AlarmHistory> findById(String tenantId, String alarmId) {
    Handle h = db.open();

    try {
      return null;
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }
}