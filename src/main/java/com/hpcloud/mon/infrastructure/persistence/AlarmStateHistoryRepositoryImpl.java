/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpcloud.mon.infrastructure.persistence;

import java.sql.Timestamp;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistory;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import com.hpcloud.persistence.BeanMapper;

/**
 * Alarm repository implementation.
 */
public class AlarmStateHistoryRepositoryImpl implements AlarmStateHistoryRepository {
  private final DBI db;

  @Inject
  public AlarmStateHistoryRepositoryImpl(@Named("vertica") DBI db) {
    this.db = db;
  }

  @Override
  public List<AlarmStateHistory> findById(String tenantId, String alarmId) {
    try (Handle h = db.open()) {
      return h.createQuery(
          "select alarm_id, old_state, new_state, reason, reason_data, time_stamp as timestamp from MonAlarms.StateHistory where tenant_id = :tenantId and alarm_id = :alarmId order by time_stamp desc")
          .bind("tenantId", tenantId)
          .bind("alarmId", alarmId)
          .map(new BeanMapper<>(AlarmStateHistory.class))
          .list();
    }
  }

  @Override
  public void create(String tenantId, String alarmId, AlarmState oldState, AlarmState newState,
      String reason, String reasonData, DateTime timestamp) {
    try (Handle h = db.open()) {
      h.insert(
          "insert into MonAlarms.StateHistory (tenant_id, alarm_id, old_state, new_state, reason, reason_data, time_stamp) values (?, ?, ?, ?, ?, ?, ?)",
          tenantId, alarmId, oldState.name(), newState.name(), reason, reasonData, new Timestamp(
              timestamp.getMillis()));
    }
  }
}