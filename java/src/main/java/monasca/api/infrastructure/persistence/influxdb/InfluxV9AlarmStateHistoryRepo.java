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

import monasca.api.domain.model.alarmstatehistory.AlarmStateHistory;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistoryRepo;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class InfluxV9AlarmStateHistoryRepo implements AlarmStateHistoryRepo {

  private static final Logger logger = LoggerFactory
      .getLogger(InfluxV9AlarmStateHistoryRepo.class);

  private final Utils utils;
  private final InfluxV9RepoReader influxV9RepoReader;
  private final InfluxV9Utils influxV9Utils;
  private final PersistUtils persistUtils;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper
        .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
  }

  private static final TypeReference<List<MetricDefinition>> METRICS_TYPE =
      new TypeReference<List<MetricDefinition>>() {};

  private static final TypeReference<List<AlarmTransitionSubAlarm>> SUB_ALARMS_TYPE =
      new TypeReference<List<AlarmTransitionSubAlarm>>() {};

  @Inject
  public InfluxV9AlarmStateHistoryRepo(Utils utils,
                                       InfluxV9RepoReader influxV9RepoReader,
                                       InfluxV9Utils influxV9Utils,
                                       PersistUtils persistUtils) {

    this.utils = utils;
    this.influxV9RepoReader = influxV9RepoReader;
    this.influxV9Utils = influxV9Utils;
    this.persistUtils = persistUtils;

  }

  @Override
  public List<AlarmStateHistory> findById(String tenantId, String alarmId, String offset,
                                          int limit)
      throws Exception {

    String q = String.format("select alarm_id, metrics, old_state, new_state, "
                             + "reason, reason_data, sub_alarms "
                             + "from alarm_state_history "
                             + "where %1$s %2$s %3$s %4$s",
                             this.influxV9Utils.publicTenantIdPart(tenantId),
                             this.influxV9Utils.alarmIdPart(alarmId),
                             this.influxV9Utils.timeOffsetPart(offset),
                             this.influxV9Utils.limitPart(limit));

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
                                      @Nullable String offset, int limit) throws Exception {

    List<String> alarmIdList = this.utils.findAlarmIds(tenantId, dimensions);

    if (alarmIdList == null || alarmIdList.isEmpty()) {
      return new ArrayList<>();
    }

    String q = String.format("select alarm_id, metrics, old_state, new_state, "
                             + "reason, reason_data, sub_alarms "
                             + "from alarm_state_history "
                             + "where %1$s %2$s %3$s %4$s %5$s",
                             this.influxV9Utils.publicTenantIdPart(tenantId),
                             this.influxV9Utils.startTimeEndTimePart(startTime, endTime),
                             this.influxV9Utils.alarmIdsPart(alarmIdList),
                             this.influxV9Utils.timeOffsetPart(offset),
                             this.influxV9Utils.limitPart(limit));

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
            date = this.persistUtils.parseTimestamp(values[0]);
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
            subAlarmList = this.objectMapper.readValue(values[7], SUB_ALARMS_TYPE);
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
