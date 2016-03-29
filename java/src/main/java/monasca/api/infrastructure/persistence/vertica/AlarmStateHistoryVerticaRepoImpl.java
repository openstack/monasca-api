/*
 * Copyright (c) 2014,2016 Hewlett Packard Enterprise Development Company, L.P.
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
package monasca.api.infrastructure.persistence.vertica;

import monasca.api.domain.model.alarmstatehistory.AlarmStateHistory;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistoryRepo;
import monasca.api.infrastructure.persistence.DimensionQueries;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.api.infrastructure.persistence.Utils;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmTransitionSubAlarm;
import monasca.common.model.metric.MetricDefinition;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

public class AlarmStateHistoryVerticaRepoImpl implements AlarmStateHistoryRepo {

  private static final Logger logger =
      LoggerFactory.getLogger(AlarmStateHistoryVerticaRepoImpl.class);

  private static final String FIND_BY_ALARM_IDS_SQL =
      "select alarm_id, metrics, old_state, new_state, reason, reason_data, sub_alarms, time_stamp as timestamp "
      + "from MonAlarms.StateHistory "
      + "where tenant_id = :tenantId %s "
      + "order by time_stamp asc "
      + "limit :limit";

  private static final String FIND_BY_ALARM_ID_SQL =
      "select alarm_id, metrics, old_state, new_state, reason, reason_data, sub_alarms, time_stamp as timestamp "
      + "from MonAlarms.StateHistory "
      + "where tenant_id = :tenantId and alarm_id = :alarmId %s "
      + "order by time_stamp asc "
      + "limit :limit";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper
        .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
  }

  private static final TypeReference<List<MetricDefinition>> METRICS_TYPE =
      new TypeReference<List<MetricDefinition>>() {};

  private static final TypeReference<List<AlarmTransitionSubAlarm>> SUB_ALARMS_TYPE =
      new TypeReference<List<AlarmTransitionSubAlarm>>() {};

  private final DBI vertica;
  private final Utils utils;
  private final PersistUtils persistUtils;

  private final ThreadLocal<SimpleDateFormat> simpleDateFormatter;

  @Inject
  public AlarmStateHistoryVerticaRepoImpl(
      @Named("vertica") DBI vertica,
      Utils utils,
      PersistUtils persistUtils) {

    this.vertica = vertica;
    this.utils = utils;
    this.persistUtils = persistUtils;
    this.simpleDateFormatter = new ThreadLocal<>();
  }

  @Override
  public List<AlarmStateHistory> findById(
      String tenantId,
      String alarmId,
      String offset,
      int limit) {

    String offsetPart = "";

    if (offset != null && !offset.isEmpty()) {

      offsetPart = (" and time_stamp > :offset");

    }

    String sql = String.format(FIND_BY_ALARM_ID_SQL, offsetPart);

    logger.debug("vertica sql: {}", sql);

    List<AlarmStateHistory> alarmStateHistoryList = new ArrayList<>();

    try (Handle h = vertica.open()) {

      Query<Map<String, Object>> verticaQuery =
          h.createQuery(sql)
              .bind("tenantId", tenantId)
              .bind("alarmId", alarmId)
              .bind("limit", limit + 1);

      if (offset != null && !offset.isEmpty()) {

        DateTime offset_dt = new DateTime(offset);
        verticaQuery.bind("offset", formatDateFromMillis(offset_dt.getMillis()));

      }

      for (Map<String, Object> row : verticaQuery.list()) {

        alarmStateHistoryList.add(getAlarmStateHistory(row));

      }

      return alarmStateHistoryList;
    }

  }

  private String formatDateFromMillis(final long millis) {
    if (simpleDateFormatter.get() == null) {
      simpleDateFormatter.set(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));
      simpleDateFormatter.get().setTimeZone(TimeZone.getTimeZone("GMT-0"));
    }
    return simpleDateFormatter.get().format(new Date(millis));
  }

  @Override
  public List<AlarmStateHistory> find(
      String tenantId,
      Map<String, String> dimensions,
      DateTime startTime,
      @Nullable DateTime endTime,
      @Nullable String offset,
      int limit) {

    List<String> alarmIds = this.utils.findAlarmIds(tenantId, dimensions);

    if (alarmIds == null || alarmIds.isEmpty()) {

      logger.debug("list of alarm ids is empty");

      return Collections.emptyList();

    }

    StringBuilder sb = new StringBuilder();

    sb.append(" and alarm_id in (");

    for (int i = 0; i < alarmIds.size(); i++) {

      if (i > 0) {

        sb.append(", ");
      }

      sb.append('\'').append(alarmIds.get(i)).append('\'');

    }

    sb.append(')');

    if (startTime != null) {

      sb.append(" and time_stamp >= :startTime");

    }

    if (endTime != null) {

      sb.append(" and time_stamp <= :endTime");

    }

    if (offset != null && !offset.isEmpty()) {

      sb.append(" and time_stamp > :offset");

    }

    String sql = String.format(FIND_BY_ALARM_IDS_SQL, sb);

    logger.debug("vertica sql: {}", sql);

    List<AlarmStateHistory> alarmStateHistoryList = new ArrayList<>();

    try (Handle h = vertica.open()) {

      Query<Map<String, Object>> verticaQuery =
          h.createQuery(sql)
              .bind("tenantId", tenantId)
              .bind("limit", limit + 1);

      if (startTime != null) {

        logger.debug("binding startime: {}", startTime);

        // Timestamp will not work in this query for some unknown reason.
        verticaQuery.bind("startTime", formatDateFromMillis(startTime.getMillis()));

      }

      if (endTime != null) {

        logger.debug("binding endtime: {}", endTime);

        // Timestamp will not work in this query for some unknown reason.
        verticaQuery.bind("endTime", formatDateFromMillis(endTime.getMillis()));

      }

      if (offset != null && !offset.isEmpty()) {

        logger.debug("binding offset: {}", offset);

        DateTime offset_dt = new DateTime(offset);
        verticaQuery.bind("offset", formatDateFromMillis(offset_dt.getMillis()));
        
      }

      DimensionQueries.bindDimensionsToQuery(verticaQuery, dimensions);

      for (Map<String, Object> row : verticaQuery.list()) {

        alarmStateHistoryList.add(getAlarmStateHistory(row));

      }

    }

    return alarmStateHistoryList;

  }

  private AlarmStateHistory getAlarmStateHistory(Map<String, Object> row) {

    AlarmStateHistory alarmStateHistory = new AlarmStateHistory();

    Date date;

    try {

      date = this.persistUtils.parseTimestamp(row.get("timestamp").toString() + "Z");

    } catch (ParseException e) {

      logger.error("Failed to parse time", e);

      return null;
    }

    DateTime dateTime = new DateTime(date.getTime(), DateTimeZone.UTC);
    alarmStateHistory.setTimestamp(dateTime);

    alarmStateHistory.setAlarmId((String) row.get("alarm_id"));

    List<MetricDefinition> metricDefinitionList;
    try {

      metricDefinitionList = objectMapper.readValue((String) row.get("metrics"), METRICS_TYPE);

    } catch (IOException e) {

      logger.error("Failed to parse metrics", e);

      metricDefinitionList = new ArrayList<>();
    }

    alarmStateHistory.setMetrics(metricDefinitionList);

    alarmStateHistory.setOldState(AlarmState.valueOf((String) row.get("old_state")));
    alarmStateHistory.setNewState(AlarmState.valueOf((String) row.get("new_state")));
    alarmStateHistory.setReason((String) row.get("reason"));
    alarmStateHistory.setReasonData((String) row.get("reason_data"));

    List<AlarmTransitionSubAlarm> subAlarmList;
    try {

      subAlarmList = objectMapper.readValue((String) row.get("sub_alarms"), SUB_ALARMS_TYPE);

    } catch (IOException e) {

      logger.error("Failed to parse sub-alarms", e);

      subAlarmList = new ArrayList<>();
    }

    alarmStateHistory.setSubAlarms(subAlarmList);

    return alarmStateHistory;
  }

}
