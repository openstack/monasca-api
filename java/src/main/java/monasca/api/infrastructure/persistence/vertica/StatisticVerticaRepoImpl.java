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
      List<String> groupBy) throws MultipleMetricsException {

    Map<String, Statistics> statisticsMap = new HashMap<>();

    // Sort the column names so that they match the order of the statistics in the results.
    List<String> statisticsColumns = createColumnsList(statisticsCols);

    try (Handle h = db.open()) {

      if (groupBy.isEmpty() && !Boolean.TRUE.equals(mergeMetricsFlag)) {

        MetricQueries.checkForMultipleDefinitions(h, tenantId, name, dimensions);

      }

      String sql = createQuery(name, dimensions, period, startTime, endTime, offset,
                               statisticsCols, mergeMetricsFlag, groupBy);

      logger.debug("vertica sql: {}", sql);

      Query<Map<String, Object>>
          query =
          h.createQuery(sql)
              .bind("tenantId", tenantId)
              .bind("start_time", startTime)
              .bind("end_time", endTime)
              .bind("limit", limit + 1);

      if (name != null && !name.isEmpty()) {
        query.bind("name", name);
      }

      MetricQueries.bindDimensionsToQuery(query, dimensions);

      if (!groupBy.isEmpty()) {
        MetricQueries.bindGroupBy(query, groupBy);
      }

      if (offset != null && !offset.isEmpty()) {
        logger.debug("binding offset: {}", offset);

        MetricQueries.bindOffsetToQuery(query, offset);
      }

      List<Map<String, Object>> rows = query.list();

      if (rows.size() == 0) {
        return new ArrayList<>();
      }

      if (!groupBy.isEmpty() && groupBy.contains("*")) {

        String currentDefId = null;

        for (Map<String, Object> row : rows) {

          List<Object> statisticsRow = parseRow(row);

          String defDimsId = (String) row.get("id");

          if (defDimsId != null && !defDimsId.equals(currentDefId)) {
            Statistics newStats = new Statistics();
            newStats.setColumns(statisticsColumns);

            statisticsMap.put(defDimsId, newStats);
            currentDefId = defDimsId;
          }

          statisticsMap.get(defDimsId).addMeasurement(statisticsRow);

        }

        MetricQueries.addDefsToResults(statisticsMap, h, this.dbHint);

      } else if (!groupBy.isEmpty()) {

        String currentId = null;

        for (Map<String, Object> row : rows) {

          String dimensionValues = (String) row.get("dimension_values");

          if (dimensionValues != null && !dimensionValues.equals(currentId)) {
            currentId = dimensionValues;

            Statistics tmp = new Statistics();
            tmp.setId(dimensionValues);
            tmp.setName(name);
            tmp.setDimensions(MetricQueries.combineGroupByAndValues(groupBy, dimensionValues));

            statisticsMap.put(dimensionValues, tmp);
          }

          List<Object> statisticsRow = parseRow(row);

          statisticsMap.get(dimensionValues).addMeasurement(statisticsRow);

        }

      } else {

        Statistics statistics = new Statistics();

        statistics.setId("");

        statistics.setName(name);

        statistics.setColumns(statisticsColumns);

        String firstDefId = (String) rows.get(0).get("id");

        for (Map<String, Object> row : rows) {

          List<Object> statisticsRow = parseRow(row);

          statistics.addMeasurement(statisticsRow);

        }

        statisticsMap.put(firstDefId, statistics);

        if (!Boolean.TRUE.equals(mergeMetricsFlag)) {
          statistics.setId(firstDefId);
          MetricQueries.addDefsToResults(statisticsMap, h, this.dbHint);
        } else {
          if (dimensions == null) {
            dimensions = new HashMap<>();
          }
          statistics.setDimensions(dimensions);
        }
      }

    }

    List<Statistics> results = new ArrayList<>(statisticsMap.values());

    Collections.sort(results);

    return results;
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
      String name,
      Map<String, String> dimensions,
      int period,
      DateTime startTime,
      DateTime endTime,
      String offset,
      List<String> statistics,
      Boolean mergeMetricsFlag,
      List<String> groupBy) {

    StringBuilder sb = new StringBuilder();

    sb.append("SELECT ").append(this.dbHint).append(" ");
    if (!groupBy.isEmpty() && !groupBy.contains("*")) {

      sb.append(MetricQueries.buildGroupByConcatString(groupBy));
      sb.append(" as dimension_values, ");

    }
    sb.append(" max(to_hex(definition_dimensions_id)) AS id, ");
    sb.append(createColumnsStr(statistics));

    if (period >= 1) {
      sb.append("Time_slice(time_stamp, ").append(period);
      sb.append(", 'SECOND', 'START') AS time_interval");
    }

    sb.append(" FROM MonMetrics.Measurements as mes ");
    if (!groupBy.isEmpty() && !groupBy.contains("*")) {

      sb.append(MetricQueries.buildGroupBySql(groupBy));

    }

    sb.append("WHERE TO_HEX(definition_dimensions_id) IN (")
        .append(MetricQueries.buildMetricDefinitionSubSql(name, dimensions, null, null))
        .append(") ");
    sb.append(createWhereClause(startTime, endTime, offset, groupBy));

    if (period >= 1) {
      sb.append(" group by ");
      if (!groupBy.isEmpty() && groupBy.contains("*")) {

        sb.append("definition_dimensions_id, ");

      } else if (!groupBy.isEmpty()) {

        for (int i = 0; i < groupBy.size(); i++) {
          sb.append("gb").append(i).append(".value,");
        }

      }
      sb.append("time_interval ");

      sb.append(" order by ");
      if (!groupBy.isEmpty() && groupBy.contains("*")) {

        sb.append("to_hex(definition_dimensions_id),");

      } else {

        sb.append(MetricQueries.buildGroupByCommaString(groupBy));
        if (!groupBy.isEmpty())
          sb.append(',');

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
      List<String> groupBy) {

    String s = "";

    if (startTime != null && endTime != null) {
      s = "AND time_stamp >= :start_time AND time_stamp <= :end_time ";
    } else if (startTime != null) {
      s = "AND time_stamp >= :start_time ";
    }

    if (offset != null && !offset.isEmpty()) {

      if (!groupBy.isEmpty()) {
        s += " AND (TO_HEX(definition_dimensions_id) > :offset_id "
             + "OR (TO_HEX(definition_dimensions_id) = :offset_id AND time_stamp > :offset_timestamp)) ";
      } else if (!groupBy.isEmpty()){

        String concatGroupByString = MetricQueries.buildGroupByConcatString(groupBy);

        s += " AND (" + concatGroupByString + " > :offset_id" +
              " OR (" + concatGroupByString + " = :offset_id AND mes.time_stamp > :offset_timestamp)) ";

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

        sb.append(statistic + "(mes.value) as " + statistic + ", ");
    }

    return sb.toString();
  }

}
