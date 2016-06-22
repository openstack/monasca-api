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
package monasca.api.infrastructure.persistence.vertica;

import monasca.api.domain.exception.MultipleMetricsException;
import monasca.api.domain.model.statistic.StatisticRepo;
import monasca.api.domain.model.statistic.Statistics;
import monasca.api.ApiConfig;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

public class StatisticVerticaRepoImpl implements StatisticRepo {

  private static final Logger logger =
      LoggerFactory.getLogger(StatisticVerticaRepoImpl.class);

  public static final DateTimeFormatter DATETIME_FORMATTER =
      ISODateTimeFormat.dateTime().withZoneUTC();

  private final DBI db;
  private final String dbHint;

  @Inject
  public StatisticVerticaRepoImpl(@Named("vertica") DBI db,
                                  ApiConfig config)
  {
    this.db = db;
    this.dbHint = config.vertica.dbHint;
  }

  @Override
  public List<Statistics> find(
      String tenantId,
      String name,
      Map<String, String> dimensions,
      DateTime startTime,
      DateTime endTime,
      List<String> statisticsCols,
      int period,
      String offset,
      int limit,
      Boolean mergeMetricsFlag,
      String groupBy) throws MultipleMetricsException {

    List<Statistics> statisticsList = new ArrayList<>();

    // Sort the column names so that they match the order of the statistics in the results.
    List<String> statisticsColumns = createColumnsList(statisticsCols);

    try (Handle h = db.open()) {

      Map<String, Statistics> byteMap = findDefIds(h, tenantId, name, dimensions);

      if (byteMap.isEmpty()) {

        return statisticsList;

      }

      if (!"*".equals(groupBy) && !Boolean.TRUE.equals(mergeMetricsFlag) && byteMap.keySet().size() > 1) {

        throw new MultipleMetricsException(name, dimensions);

      }

      List<List<Object>> statisticsListList = new ArrayList<>();

      String sql = createQuery(byteMap.keySet(), period, startTime, endTime, offset, statisticsCols,
                               groupBy, mergeMetricsFlag);

      logger.debug("vertica sql: {}", sql);

      Query<Map<String, Object>>
          query =
          h.createQuery(sql)
              .bind("start_time", startTime)
              .bind("end_time", endTime)
              .bind("limit", limit + 1);

      if (offset != null && !offset.isEmpty()) {
        logger.debug("binding offset: {}", offset);

        MetricQueries.bindOffsetToQuery(query, offset);
      }

      List<Map<String, Object>> rows = query.list();

      if ("*".equals(groupBy)) {

        for (Map<String, Object> row : rows) {

          List<Object> statisticsRow = parseRow(row);

          String defDimsId = (String) row.get("id");

          byteMap.get(defDimsId).addStatistics(statisticsRow);

        }

        for (Map.Entry<String, Statistics> entry : byteMap.entrySet()) {

          Statistics statistics = entry.getValue();

          statistics.setColumns(statisticsColumns);

          if (statistics.getStatistics().size() > 0) {
            statisticsList.add(statistics);
          }

        }

      } else {

        for (Map<String, Object> row : rows) {

          List<Object> statisticsRow = parseRow(row);

          statisticsListList.add(statisticsRow);

        }

        // Just use the first entry in the byteMap to get the def name and dimensions.
        Statistics statistics = byteMap.entrySet().iterator().next().getValue();

        statistics.setColumns(statisticsColumns);

        if (Boolean.TRUE.equals(mergeMetricsFlag) && byteMap.keySet().size() > 1) {

          // Wipe out the dimensions.
          statistics.setDimensions(new HashMap<String, String>());

        }

        statistics.setStatistics(statisticsListList);

        statisticsList.add(statistics);
      }

    }

    return statisticsList;
  }

  private List<Object> parseRow(Map<String, Object> row) {

    List<Object> statisticsRow = new ArrayList<>();

    Double sum = (Double) row.get("sum");
    Double average = (Double) row.get("avg");
    Double min = (Double) row.get("min");
    Double max = (Double) row.get("max");
    Long count = (Long) row.get("count");
    Timestamp time_stamp = (Timestamp) row.get("time_interval");

    if (time_stamp != null) {
      statisticsRow.add(DATETIME_FORMATTER.print(time_stamp.getTime()));
    }

    if (average != null) {
      statisticsRow.add(average);
    }

    if (count != null) {
      statisticsRow.add(count);
    }

    if (max != null) {
      statisticsRow.add(max);
    }

    if (min != null) {
      statisticsRow.add(min);
    }

    if (sum != null) {
      statisticsRow.add(sum);
    }

    return statisticsRow;
  }

  private Map<String, Statistics> findDefIds(
      Handle h,
      String tenantId,
      String name,
      Map<String, String> dimensions) {

    String sql = String.format(
        MetricQueries.FIND_METRIC_DEFS_SQL,
        this.dbHint,
        MetricQueries.buildMetricDefinitionSubSql(name, dimensions));

    Query<Map<String, Object>> query =
        h.createQuery(sql)
            .bind("tenantId", tenantId);

    if (name != null && !name.isEmpty()) {

      logger.debug("binding name: {}", name);

      query.bind("name", name);

    }

    MetricQueries.bindDimensionsToQuery(query, dimensions);

    List<Map<String, Object>> rows = query.list();

    Map<String, Statistics> byteIdMap = new HashMap<>();

    String currentDefDimId = null;

    Map<String, String> dims = null;

    for (Map<String, Object> row : rows) {

      String defDimId = (String) row.get("defDimsId");

      String defName = (String) row.get("name");

      String dimName = (String) row.get("dName");

      String dimValue = (String) row.get("dValue");

      if (defDimId == null || !defDimId.equals(currentDefDimId)) {

        currentDefDimId = defDimId;

        dims = new HashMap<>();

        dims.put(dimName, dimValue);

        Statistics statistics = new Statistics();

        statistics.setId(defDimId);

        statistics.setName(defName);

        statistics.setDimensions(dims);

        byteIdMap.put(currentDefDimId, statistics);

      } else {

        dims.put(dimName, dimValue);

      }
    }

    return byteIdMap;
  }

  List<String> createColumnsList(
      List<String> list) {

    List<String> copy = new ArrayList<>();
    for (String string : list) {
      copy.add(string);
    }
    Collections.sort(copy);
    copy.add(0, "timestamp");

    return copy;
  }

  private String createQuery(
      Set<String> defDimIdSet,
      int period,
      DateTime startTime,
      DateTime endTime,
      String offset,
      List<String> statistics,
      String groupBy,
      Boolean mergeMetricsFlag) {

    StringBuilder sb = new StringBuilder();

    sb.append("SELECT "  + this.dbHint + " ");
    if (groupBy != null && !groupBy.isEmpty()) {
      sb.append(" to_hex(definition_dimensions_id) AS id, ");
    }
    sb.append(createColumnsStr(statistics));

    if (period >= 1) {
      sb.append("Time_slice(time_stamp, ").append(period);
      sb.append(", 'SECOND', 'START') AS time_interval");
    }

    sb.append(" FROM MonMetrics.Measurements ");
    String inClause = MetricQueries.createDefDimIdInClause(defDimIdSet);
    sb.append("WHERE to_hex(definition_dimensions_id) ").append(inClause);
    sb.append(createWhereClause(startTime, endTime, offset, mergeMetricsFlag));

    if (period >= 1) {
      sb.append(" group by ");
      if (groupBy != null && !groupBy.isEmpty()) {
        sb.append("definition_dimensions_id,");
      }
      sb.append("time_interval ");
      sb.append(" order by ");
      if (groupBy != null && !groupBy.isEmpty()) {
        sb.append("to_hex(definition_dimensions_id),");
      }
      sb.append("time_interval ");
    }

    sb.append(" limit :limit");

    return sb.toString();
  }

  private String createWhereClause(
      DateTime startTime,
      DateTime endTime,
      String offset,
      Boolean mergeMetricsFlag) {

    String s = "";

    if (startTime != null && endTime != null) {
      s = "AND time_stamp >= :start_time AND time_stamp <= :end_time ";
    } else if (startTime != null) {
      s = "AND time_stamp >= :start_time ";
    }

    if (offset != null && !offset.isEmpty()) {

      if (Boolean.FALSE.equals(mergeMetricsFlag)) {
        s += " AND (TO_HEX(definition_dimensions_id) > :offset_id "
             + "OR (TO_HEX(definition_dimensions_id) = :offset_id AND time_stamp > :offset_timestamp)) ";
      } else {
        s += " AND time_stamp > :offset_timestamp ";
      }

    }

    return s;
  }

  private String createColumnsStr(
      List<String> statistics) {

    StringBuilder sb = new StringBuilder();

    for (String statistic : statistics) {

        sb.append(statistic + "(value) as " + statistic + ", ");
    }

    return sb.toString();
  }

}
