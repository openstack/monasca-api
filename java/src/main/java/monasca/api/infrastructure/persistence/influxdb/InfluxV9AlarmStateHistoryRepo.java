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

import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;

import monasca.api.ApiConfig;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistory;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistoryRepo;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmTransitionSubAlarm;
import monasca.common.model.metric.MetricDefinition;

import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.WhereClauseBuilder.buildTimePart;
import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.buildAlarmsPart;
import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.findAlarmIds;

public class InfluxV9AlarmStateHistoryRepo implements AlarmStateHistoryRepo {

  private static final Logger logger = LoggerFactory
      .getLogger(InfluxV9AlarmStateHistoryRepo.class);

  private final DBI mysql;
  private final ApiConfig config;
  private final String region;
  private final InfluxV9RepoReader influxV9RepoReader;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final SimpleDateFormat simpleDateFormat =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

  private static final TypeReference<List<MetricDefinition>> METRICS_TYPE =
      new TypeReference<List<MetricDefinition>>() {};

  private static final TypeReference<List<AlarmTransitionSubAlarm>> SUBALARMS_TYPE =
      new TypeReference<List<AlarmTransitionSubAlarm>>() {};

  @Inject
  public InfluxV9AlarmStateHistoryRepo(@Named("mysql") DBI mysql,
                                       ApiConfig config,
                                       InfluxV9RepoReader influxV9RepoReader) {

    this.mysql = mysql;
    this.config = config;
    this.region = config.region;
    this.influxV9RepoReader = influxV9RepoReader;

  }

  @Override
  public List<AlarmStateHistory> findById(String tenantId, String alarmId, String offset)
      throws Exception {

    String q = String.format("select alarm_id, metrics, old_state, new_state, sub_alarms, reason, reason_data "
                             + "from alarm_state_history where tenant_id = '%1$s' and alarm_id = '%2$s'",
                             InfluxV8Utils.SQLSanitizer.sanitize(tenantId),
                             InfluxV8Utils.SQLSanitizer.sanitize(alarmId));

    logger.debug("Alarm state history query: {}", q);

    String r = this.influxV9RepoReader.read(q);

    Series series = this.objectMapper.readValue(r, Series.class);

    List<AlarmStateHistory> alarmStateHistoryList = alarmStateHistoryList(series);

    logger.debug("Found {} alarm state transitions matching query", alarmStateHistoryList.size());

    return alarmStateHistoryList;
  }

  @Override
  public List<AlarmStateHistory> find(String tenantId, Map<String, String> dimensions,
                                      DateTime startTime, @Nullable DateTime endTime,
                                      @Nullable String offset) throws Exception {

    List<String> alarmIdList = findAlarmIds(this.mysql, tenantId, dimensions);

    if (alarmIdList == null || alarmIdList.isEmpty()) {
      return new ArrayList<>();
    }

    String timePart = buildTimePart(startTime, endTime);
    String alarmsPart = buildAlarmsPart(alarmIdList);

    String q = String.format("select alarm_id, metrics, old_state, new_state, sub_alarms, reason, reason_data "
                             + "from alarm_state_history where tenant_id = '%1$s' %2$s %3$s",
                             InfluxV8Utils.SQLSanitizer.sanitize(tenantId), timePart, alarmsPart);

    logger.debug("Alarm state history list query: {}", q);

    String r = this.influxV9RepoReader.read(q);

    Series series = this.objectMapper.readValue(r, Series.class);

    List<AlarmStateHistory> alarmStateHistoryList = alarmStateHistoryList(series);

    logger.debug("Found {} alarm state transitions matching query", alarmStateHistoryList.size());

    return alarmStateHistoryList;

  }

  private List<AlarmStateHistory> alarmStateHistoryList(Series series) {

    List<AlarmStateHistory> alarmStateHistoryList = new LinkedList<>();

    if (!series.isEmpty()) {

      for (Serie serie : series.getSeries()) {

        for (String[] values : serie.getValues()) {

          AlarmStateHistory alarmStateHistory = new AlarmStateHistory();

          Date date;
          try {
            date = this.simpleDateFormat.parse(values[0]);
          } catch (ParseException e) {
            logger.error("Failed to parse time", e);
            continue;
          }

          DateTime dateTime = new DateTime(date.getTime(), DateTimeZone.UTC);
          alarmStateHistory.setTimestamp(dateTime);

          alarmStateHistory.setAlarmId(values[1]);

          List<MetricDefinition> metricDefinitionList;
          try {
            metricDefinitionList = this.objectMapper.readValue(values[2], METRICS_TYPE);
          } catch (IOException e) {
            logger.error("Failed to parse metrics", e);
            continue;
          }

          alarmStateHistory.setMetrics(metricDefinitionList);

          alarmStateHistory.setOldState(AlarmState.valueOf(values[3]));
          alarmStateHistory.setNewState(AlarmState.valueOf(values[4]));
          alarmStateHistory.setReason(values[5]);
          alarmStateHistory.setReasonData(values[6]);

          List<AlarmTransitionSubAlarm> subAlarmList;
          try {
              subAlarmList = this.objectMapper.readValue(values[7], SUBALARMS_TYPE);
          } catch (IOException e) {
            logger.error("Failed to parse sub-alarms", e);
            continue;
          }

          alarmStateHistory.setSubAlarms(subAlarmList);

          alarmStateHistoryList.add(alarmStateHistory);
        }
      }

    }
      return alarmStateHistoryList;
  }
}
