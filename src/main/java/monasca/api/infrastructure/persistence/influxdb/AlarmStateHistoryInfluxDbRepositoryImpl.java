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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.inject.Inject;

import monasca.api.MonApiConfiguration;
import monasca.api.domain.model.common.Paged;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.metric.MetricDefinition;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistory;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import monasca.api.infrastructure.persistence.DimensionQueries;

public class AlarmStateHistoryInfluxDbRepositoryImpl implements AlarmStateHistoryRepository {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<MetricDefinition>> METRICS_TYPE =
      new TypeReference<List<MetricDefinition>>() {};
  private static final Logger logger = LoggerFactory
      .getLogger(AlarmStateHistoryInfluxDbRepositoryImpl.class);
  private static final String FIND_ALARMS_SQL =
      "select distinct a.id from alarm as a "  +
      "join alarm_definition as ad on a.alarm_definition_id=ad.id " + 
      "%s " +
      "where ad.tenant_id = :tenantId and ad.deleted_at is NULL order by ad.created_at";
  private final MonApiConfiguration config;
  private final InfluxDB influxDB;
  private final DBI mysql;

  static {
    OBJECT_MAPPER
        .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
  }

  @Inject
  public AlarmStateHistoryInfluxDbRepositoryImpl(@Named("mysql") DBI mysql,
      MonApiConfiguration config, InfluxDB influxDB) {
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
    String offsetPart = Utils.buildOffsetPart(offset);
    return String.format("select alarm_id, metrics, old_state, new_state, reason, reason_data "
        + "from alarm_state_history where tenant_id = '%1$s' and alarm_id = '%2$s' %3$s",
        Utils.SQLSanitizer.sanitize(tenantId), Utils.SQLSanitizer.sanitize(alarmId), offsetPart);
  }

  @Override
  public List<AlarmStateHistory> find(String tenantId, Map<String, String> dimensions,
      DateTime startTime, @Nullable DateTime endTime, String offset) throws Exception {

    List<String> alarmIds = null;
    // Find alarm Ids for dimensions
    try (Handle h = mysql.open()) {
      final String sql =
          String.format(FIND_ALARMS_SQL, buildJoinClauseFor(dimensions));

      Query<Map<String, Object>> query = h.createQuery(sql).bind("tenantId", tenantId);
      logger.debug("AlarmStateHistory query '{}'", sql);
      DimensionQueries.bindDimensionsToQuery(query, dimensions);
      alarmIds = query.map(StringMapper.FIRST).list();
    }

    if (alarmIds == null || alarmIds.isEmpty()) {
      logger.debug("AlarmStateHistory no alarmIds");
      return Collections.emptyList();
    }

    logger.debug("AlarmStateHistory alarmIds {}", alarmIds);
    String timePart = buildTimePart(startTime, endTime);
    String alarmsPart = buildAlarmsPart(alarmIds);
    String offsetPart = Utils.buildOffsetPart(offset);

    String query = buildQueryForFind(tenantId, timePart, alarmsPart, offsetPart);
    logger.debug("AlarmStateHistory query for influxdb '{}'", query);

    return queryInfluxDBForAlarmStateHistory(query);

  }

  private String buildJoinClauseFor(Map<String, String> dimensions) {
    if ((dimensions == null) || dimensions.isEmpty()) {
      return "";
    }
    final StringBuilder sbJoin = new StringBuilder("join alarm_metric as am on a.id=am.alarm_id ");
    sbJoin
        .append("join metric_definition_dimensions as mdd on am.metric_definition_dimensions_id=mdd.id ");
    for (int i = 0; i < dimensions.size(); i++) {
      final String tableAlias = "md" + i;
      sbJoin.append(" inner join metric_dimension ").append(tableAlias).append(" on ")
          .append(tableAlias).append(".name = :dname").append(i).append(" and ")
          .append(tableAlias).append(".value = :dvalue").append(i)
          .append(" and mdd.metric_dimension_set_id = ")
          .append(tableAlias).append(".dimension_set_id");
    }

    return sbJoin.toString();
  }

  String buildTimePart(DateTime startTime, DateTime endTime) {
    return Utils.WhereClauseBuilder.buildTimePart(startTime, endTime);
  }

  String buildQueryForFind(String tenantId, String timePart, String alarmsPart, String offsetPart) throws Exception {
    return String.format("select alarm_id, metrics, old_state, new_state, reason, reason_data "
        + "from alarm_state_history where tenant_id = '%1$s' %2$s %3$s %4$s",
        Utils.SQLSanitizer.sanitize(tenantId), timePart, alarmsPart, offsetPart);
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

  @SuppressWarnings("unchecked")
  private List<AlarmStateHistory> queryInfluxDBForAlarmStateHistory(String query) {
    logger.debug("Query string: {}", query);

    List<Serie> result;
    try {
      result = this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      if (e.getMessage().startsWith(Utils.COULD_NOT_LOOK_UP_COLUMNS_EXC_MSG)) {
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

        alarmStateHistoryList.add(alarmStateHistory);
      }
    }

    return alarmStateHistoryList;
  }
}
