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

import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistory;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import com.hpcloud.persistence.BeanMapper;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.util.StringMapper;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Alarm repository implementation.
 */
public class AlarmStateHistoryVerticaRepositoryImpl implements AlarmStateHistoryRepository {
  public static final DateTimeFormatter DATETIME_FORMATTER = ISODateTimeFormat.dateTimeNoMillis()
      .withZoneUTC();
  private static final String FIND_ALARMS_SQL = "select distinct a.id from alarm as a "
      + "join sub_alarm sa on a.id = sa.alarm_id "
      + "left outer join sub_alarm_dimension dim on sa.id = dim.sub_alarm_id%s "
      + "where a.tenant_id = :tenantId and a.deleted_at is NULL";
  private static final String FIND_BY_ALARM_DEF_SQL = "select *, time_stamp as timestamp from MonAlarms.StateHistory "
      + "where tenant_id = :tenantId%s order by time_stamp desc";

  private final DBI mysql;
  private final DBI vertica;

  @Inject
  public AlarmStateHistoryVerticaRepositoryImpl(@Named("mysql") DBI mysql, @Named("vertica") DBI vertica) {
    this.mysql = mysql;
    this.vertica = vertica;
  }

  @Override
  public List<AlarmStateHistory> findById(String tenantId, String alarmId) {
    try (Handle h = vertica.open()) {
      return h.createQuery(
          "select alarm_id, old_state, new_state, reason, reason_data, time_stamp as timestamp from MonAlarms.StateHistory where tenant_id = :tenantId and alarm_id = :alarmId order by time_stamp desc")
          .bind("tenantId", tenantId)
          .bind("alarmId", alarmId)
          .map(new BeanMapper<>(AlarmStateHistory.class))
          .list();
    }
  }

  @Override
  public Collection<AlarmStateHistory> find(String tenantId, Map<String, String> dimensions,
      DateTime startTime, @Nullable DateTime endTime) {
    List<String> alarmIds = null;

    // Find alarm Ids for dimensions
    try (Handle h = mysql.open()) {
      String sql = String.format(FIND_ALARMS_SQL, Utils.SubAlarmQueries.buildJoinClauseFor(dimensions));
      Query<Map<String, Object>> query = h.createQuery(sql).bind("tenantId", tenantId);
      Utils.DimensionQueries.bindDimensionsToQuery(query, dimensions);
      alarmIds = query.map(StringMapper.FIRST).list();
    }

    if (alarmIds == null || alarmIds.isEmpty())
      return Collections.emptyList();

    // Find alarm state history for alarm Ids
    try (Handle h = vertica.open()) {
      // Build sql
      StringBuilder sbWhere = new StringBuilder();
      sbWhere.append(" and alarm_id in (");
      for (int i = 0; i < alarmIds.size(); i++) {
        if (i > 0)
          sbWhere.append(", ");
        sbWhere.append('\'').append(alarmIds.get(i)).append('\'');
      }
      sbWhere.append(')');
      if (startTime != null)
        sbWhere.append(" and time_stamp >= :startTime");
      if (endTime != null)
        sbWhere.append(" and time_stamp <= :endTime");
      String sql = String.format(FIND_BY_ALARM_DEF_SQL, sbWhere);

      // Build query
      Query<Map<String, Object>> query = h.createQuery(sql).bind("tenantId", tenantId);
      if (startTime != null)
        query.bind("startTime", new Timestamp(startTime.getMillis()));
      if (endTime != null)
        query.bind("endTime", new Timestamp(endTime.getMillis()));
      Utils.DimensionQueries.bindDimensionsToQuery(query, dimensions);
      return query.map(new BeanMapper<>(AlarmStateHistory.class)).list();
    }
  }
}