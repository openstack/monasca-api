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
package com.hpcloud.mon.infrastructure.persistence.influxdb;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.util.StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistory;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import com.hpcloud.mon.infrastructure.persistence.DimensionQueries;
import com.hpcloud.mon.infrastructure.persistence.SubAlarmQueries;

public class AlarmStateHistoryInfluxDBRepositoryImpl implements AlarmStateHistoryRepository {

  private static final Logger logger = LoggerFactory
      .getLogger(AlarmStateHistoryInfluxDBRepositoryImpl.class);

  private final MonApiConfiguration config;
  private final InfluxDB influxDB;
  private final DBI mysql;

  private static final String FIND_ALARMS_SQL = "select distinct a.id from alarm as a " + "join"
      + " sub_alarm sa on a.id = sa.alarm_id " + "left outer join sub_alarm_dimension dim on "
      + "sa.id = dim.sub_alarm_id%s " + "where a.tenant_id = :tenantId and a.deleted_at is "
      + "NULL";

  @Inject
  public AlarmStateHistoryInfluxDBRepositoryImpl(@Named("mysql") DBI mysql,
      MonApiConfiguration config, InfluxDB influxDB) {
    this.mysql = mysql;
    this.config = config;
    this.influxDB = influxDB;
  }

  @Override
  public List<AlarmStateHistory> findById(String tenantId, String alarmId) throws Exception {

    // InfluxDB orders queries by time stamp desc by default.
    String query = buildQueryForFindById(tenantId, alarmId);
    return queryInfluxDBForAlarmStateHistory(query);
  }

  String buildQueryForFindById(String tenantId, String alarmId) throws Exception {

    return String.format("select alarm_id, old_state, new_state, reason, reason_data "
        + "from alarm_state_history " + "where tenant_id = '%1$s' and alarm_id = '%2$s'",
        Utils.SQLSanitizer.sanitize(tenantId), Utils.SQLSanitizer.sanitize(alarmId));
  }

  @Override
  public Collection<AlarmStateHistory> find(String tenantId, Map<String, String> dimensions,
      DateTime startTime, @Nullable DateTime endTime) throws Exception {

    List<String> alarmIds = null;
    // Find alarm Ids for dimensions
    try (Handle h = mysql.open()) {
      String sql = String.format(FIND_ALARMS_SQL, SubAlarmQueries.buildJoinClauseFor(dimensions));
      Query<Map<String, Object>> query = h.createQuery(sql).bind("tenantId", tenantId);
      DimensionQueries.bindDimensionsToQuery(query, dimensions);
      alarmIds = query.map(StringMapper.FIRST).list();
    }

    if (alarmIds == null || alarmIds.isEmpty()) {
      return Collections.emptyList();
    }

    String timePart = buildTimePart(startTime, endTime);
    String alarmsPart = buildAlarmsPart(alarmIds);

    String query = buildQueryForFind(tenantId, timePart, alarmsPart);

    return queryInfluxDBForAlarmStateHistory(query);

  }

  String buildTimePart(DateTime startTime, DateTime endTime) {
    return Utils.WhereClauseBuilder.buildTimePart(startTime, endTime);
  }

  String buildQueryForFind(String tenantId, String timePart, String alarmsPart) throws Exception {
    return String.format("select alarm_id, old_state, new_state, reason, reason_data "
        + "from alarm_state_history " + "where tenant_id = '%1$s' %2$s %3$s",
        Utils.SQLSanitizer.sanitize(tenantId), timePart, alarmsPart);
  }

  String buildAlarmsPart(List<String> alarmIds) {

    StringBuilder sb = new StringBuilder();
    for (String alarmId : alarmIds) {
      if (sb.length() > 0) {
        sb.append(" or ");
      }
      sb.append(String.format(" alarm_id = '%1$s' ", alarmId));
    }

    if (sb.length() > 0) {
      sb.insert(0, " and (");
      sb.insert(sb.length(), ")");
    }
    return sb.toString();
  }

  private List<AlarmStateHistory> queryInfluxDBForAlarmStateHistory(String query) {

    logger.debug("Query string: {}", query);

    List<Serie> result =
        this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.MILLISECONDS);

    List<AlarmStateHistory> alarmStateHistoryList = new LinkedList<>();

    // Should only be one serie -- alarm_state_history.
    for (Serie serie : result) {
      Object[][] valObjArryArry = serie.getPoints();
      for (int i = 0; i < valObjArryArry.length; i++) {

        AlarmStateHistory alarmStateHistory = new AlarmStateHistory();
        // Time is always in position 0.
        Double timeDouble = (Double) valObjArryArry[i][0];
        alarmStateHistory.setTimestamp(new DateTime(timeDouble.longValue(), DateTimeZone.UTC));
        // Sequence_number is always in position 1.
        alarmStateHistory.setAlarmId((String) valObjArryArry[i][2]);
        alarmStateHistory.setNewState(AlarmState.valueOf((String) valObjArryArry[i][3]));
        alarmStateHistory.setOldState(AlarmState.valueOf((String) valObjArryArry[i][4]));
        alarmStateHistory.setReason((String) valObjArryArry[i][5]);
        alarmStateHistory.setReasonData((String) valObjArryArry[i][6]);

        alarmStateHistoryList.add(alarmStateHistory);
      }
    }

    return alarmStateHistoryList;
  }
}
