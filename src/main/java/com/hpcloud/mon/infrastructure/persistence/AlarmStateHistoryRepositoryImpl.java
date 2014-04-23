package com.hpcloud.mon.infrastructure.persistence;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistory;

/**
 * Alarm repository implementation.
 * 
 * @author Jonathan Halterman
 */
public class AlarmStateHistoryRepositoryImpl implements AlarmStateHistoryRepository {
  private final DBI db;

  @Inject
  public AlarmStateHistoryRepositoryImpl(@Named("vertica") DBI db) {
    this.db = db;
  }

  @Override
  public List<AlarmStateHistory> findById(String tenantId, String alarmId) {
    Handle h = db.open();

    try {
      return h.createQuery(
          "select * from MonAlarms.StateHistory where tenant_id = :tenantId and id = :alarmId")
          .bind("tenantId", tenantId)
          .bind("alarmId", alarmId)
          .map(AlarmStateHistory.class)
          .list();
    } finally {
      h.close();
    }
  }
}