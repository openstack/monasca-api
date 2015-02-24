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
package monasca.api.infrastructure.persistence.influxdb;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.inject.Inject;

import monasca.api.ApiConfig;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmTransitionSubAlarm;
import monasca.common.model.metric.MetricDefinition;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistory;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistoryRepo;
import monasca.api.infrastructure.persistence.DimensionQueries;

import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.buildAlarmsPart;
import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.findAlarmIds;

public class InfluxV8AlarmStateHistoryRepo implements AlarmStateHistoryRepo {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<MetricDefinition>> METRICS_TYPE =
      new TypeReference<List<MetricDefinition>>() {};
  private static final TypeReference<List<AlarmTransitionSubAlarm>> SUBALARMS_TYPE =
      new TypeReference<List<AlarmTransitionSubAlarm>>() {};
  private static final Logger logger = LoggerFactory
      .getLogger(InfluxV8AlarmStateHistoryRepo.class);


  private final ApiConfig config;
  private final InfluxDB influxDB;
  private final DBI mysql;

  static {
    OBJECT_MAPPER
        .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
  }

  @Inject
  public InfluxV8AlarmStateHistoryRepo(@Named("mysql") DBI mysql, ApiConfig config,
                                       InfluxDB influxDB) {
    this.mysql = mysql;
    this.config = config;
    this.influxDB = influxDB;
  }

  @Override
  public List<AlarmStateHistory> findById(String tenantId, String alarmId, String offset) throws Exception {
    // InfluxDB orders queries by time stamp desc by default.
    String query = buildQueryForFindById(tenantId, alarmId, offset);
    return queryInfluxDBForAlarmStateHistory(query);
  }

  String buildQueryForFindById(String tenantId, String alarmId, String offset) throws Exception {
    String offsetPart = InfluxV8Utils.buildOffsetPart(offset);
    return String.format("select alarm_id, metrics, old_state, new_state, sub_alarms, reason, reason_data "
        + "from alarm_state_history where tenant_id = '%1$s' and alarm_id = '%2$s' %3$s",
        InfluxV8Utils.SQLSanitizer.sanitize(tenantId), InfluxV8Utils.SQLSanitizer.sanitize(alarmId), offsetPart);
  }

  @Override
  public List<AlarmStateHistory> find(String tenantId, Map<String, String> dimensions,
      DateTime startTime, @Nullable DateTime endTime, String offset) throws Exception {

    List<String> alarmIdList = findAlarmIds(this.mysql, tenantId, dimensions);

    if (alarmIdList == null || alarmIdList.isEmpty()) {
      logger.debug("AlarmStateHistory no alarmIds");
      return Collections.emptyList();
    }

    logger.debug("AlarmStateHistory alarmIds {}", alarmIdList);
    String timePart = buildTimePart(startTime, endTime);
    String alarmsPart = buildAlarmsPart(alarmIdList);
    String offsetPart = InfluxV8Utils.buildOffsetPart(offset);

    String query = buildQueryForFind(tenantId, timePart, alarmsPart, offsetPart);
    logger.debug("AlarmStateHistory query for influxdb '{}'", query);

    return queryInfluxDBForAlarmStateHistory(query);

  }

  String buildTimePart(DateTime startTime, DateTime endTime) {
    return InfluxV8Utils.WhereClauseBuilder.buildTimePart(startTime, endTime);
  }

  String buildQueryForFind(String tenantId, String timePart, String alarmsPart, String offsetPart) throws Exception {
    return String.format("select alarm_id, metrics, old_state, new_state, sub_alarms, reason, reason_data "
        + "from alarm_state_history where tenant_id = '%1$s' %2$s %3$s %4$s",
        InfluxV8Utils.SQLSanitizer.sanitize(tenantId), timePart, alarmsPart, offsetPart);
  }


  @SuppressWarnings("unchecked")
  private List<AlarmStateHistory> queryInfluxDBForAlarmStateHistory(String query) {
    logger.debug("Query string: {}", query);

    List<Serie> result;
    try {
      result = this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      if (e.getMessage().startsWith(InfluxV8Utils.COULD_NOT_LOOK_UP_COLUMNS_EXC_MSG)) {
        return new LinkedList<>();
      } else {
        logger.error("Failed to get data from InfluxDB", e);
        throw e;
      }
    }

    List<AlarmStateHistory> alarmStateHistoryList = new LinkedList<>();

    // Should only be one serie -- alarm_state_history.
    for (Serie serie : result) {
      final String[] colNames = serie.getColumns();
      final List<Map<String, Object>> rows = serie.getRows();

      for (Map<String, Object> row : rows) {
        AlarmStateHistory alarmStateHistory = new AlarmStateHistory();
        // Time is always in position 0.
        Double timeDouble = (Double) row.get(colNames[0]);
        alarmStateHistory.setTimestamp(new DateTime(timeDouble.longValue(), DateTimeZone.UTC));
        // Sequence_number is always in position 1.
        alarmStateHistory.setAlarmId((String) row.get(colNames[2]));
        try {
          alarmStateHistory.setMetrics((List<MetricDefinition>) OBJECT_MAPPER.readValue(
              (String) row.get(colNames[3]), METRICS_TYPE));
        } catch (Exception ignore) {
          alarmStateHistory.setMetrics(Collections.<MetricDefinition>emptyList());
        }

        alarmStateHistory.setNewState(AlarmState.valueOf((String) row.get(colNames[4])));
        alarmStateHistory.setOldState(AlarmState.valueOf((String) row.get(colNames[5])));

        alarmStateHistory.setReason((String) row.get(colNames[6]));
        alarmStateHistory.setReasonData((String) row.get(colNames[7]));

        try {
          alarmStateHistory.setSubAlarms((List<AlarmTransitionSubAlarm>) OBJECT_MAPPER.readValue(
              (String) row.get(colNames[8]), SUBALARMS_TYPE));
        } catch (Exception ignore) {
          alarmStateHistory.setSubAlarms(Collections.<AlarmTransitionSubAlarm>emptyList());
        }

        alarmStateHistoryList.add(alarmStateHistory);
      }
    }

    return alarmStateHistoryList;
  }
}
