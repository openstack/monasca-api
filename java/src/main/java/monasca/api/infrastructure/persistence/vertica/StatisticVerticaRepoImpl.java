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
import monasca.api.infrastructure.persistence.DimensionQueries;

import org.apache.commons.codec.binary.Hex;
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
import java.util.Arrays;
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

  private static final String FIND_BY_METRIC_DEF_SQL =
      "select defdims.id, def.name, d.name as dname, d.value as dvalue "
      + "from MonMetrics.Definitions def, MonMetrics.DefinitionDimensions defdims "
      + "left outer join MonMetrics.Dimensions d on d.dimension_set_id = defdims.dimension_set_id "
      + "where def.id = defdims.definition_id and def.tenant_id = :tenantId "
      + "%s " // metric name here
      + "%s " // dimension and clause here
      + "order by defdims.id ASC";

  private static final String TABLE_TO_JOIN_DIMENSIONS_ON = "defdims";

  private final DBI db;

  @Inject
  public StatisticVerticaRepoImpl(@Named("vertica") DBI db) {

    this.db = db;

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
      Boolean mergeMetricsFlag) throws MultipleMetricsException {

    List<Statistics> statisticsList = new ArrayList<>();

    // Sort the column names so that they match the order of the statistics in the results.
    List<String> statisticsColumns = createColumnsList(statisticsCols);

    try (Handle h = db.open()) {

      Map<byte[], Statistics> byteMap = findDefIds(h, tenantId, name, dimensions);

      if (byteMap.isEmpty()) {

        return statisticsList;

      }

      if (!Boolean.TRUE.equals(mergeMetricsFlag) && byteMap.keySet().size() > 1) {

        throw new MultipleMetricsException(name, dimensions);

      }

      List<List<Object>> statisticsListList = new ArrayList<>();

      String sql = createQuery(byteMap.keySet(), period, startTime, endTime, offset, statisticsCols);

      logger.debug("vertica sql: {}", sql);

      Query<Map<String, Object>>
          query =
          h.createQuery(sql)
              .bind("start_time", startTime)
              .bind("end_time", endTime)
              .bind("limit", limit + 1);

      if (offset != null && !offset.isEmpty()) {
        logger.debug("binding offset: {}", offset);
        query.bind("offset", new Timestamp(DateTime.parse(offset).getMillis()));
      }

      List<Map<String, Object>> rows = query.list();

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

  private Map<byte[], Statistics> findDefIds(
      Handle h,
      String tenantId,
      String name,
      Map<String, String> dimensions) {

    List<byte[]> bytes = new ArrayList<>();

    StringBuilder sb = new StringBuilder();

    if (name != null && !name.isEmpty()) {

      sb.append(" and def.name = :name");

    }

    String sql =
        String
            .format(FIND_BY_METRIC_DEF_SQL,
                    sb,
                    MetricQueries.buildDimensionAndClause(dimensions, TABLE_TO_JOIN_DIMENSIONS_ON));

    Query<Map<String, Object>> query =
        h.createQuery(sql)
            .bind("tenantId", tenantId);

    if (name != null && !name.isEmpty()) {

      logger.debug("binding name: {}", name);

      query.bind("name", name);

    }

    DimensionQueries.bindDimensionsToQuery(query, dimensions);

    List<Map<String, Object>> rows = query.list();

    Map<byte[], Statistics> byteIdMap = new HashMap<>();

    byte[] currentDefDimId = null;

    Map<String, String> dims = null;

    for (Map<String, Object> row : rows) {

      byte[] defDimId = (byte[]) row.get("id");

      String defName = (String) row.get("name");

      String dimName = (String) row.get("dname");

      String dimValue = (String) row.get("dvalue");

      if (defDimId == null || !Arrays.equals(currentDefDimId, defDimId)) {

        currentDefDimId = defDimId;

        dims = new HashMap<>();

        dims.put(dimName, dimValue);

        Statistics statistics = new Statistics();

        statistics.setName(defName);

        statistics.setDimensions(dims);

        byteIdMap.put(currentDefDimId, statistics);

      } else {

        dims.put(dimName, dimValue);

      }
    }

    bytes.add(currentDefDimId);

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
      Set<byte[]> defDimIdSet,
      int period,
      DateTime startTime,
      DateTime endTime,
      String offset,
      List<String> statistics) {

    StringBuilder sb = new StringBuilder();

    sb.append("SELECT " + createColumnsStr(statistics));

    if (period >= 1) {
      sb.append("Time_slice(time_stamp, " + period);
      sb.append(", 'SECOND', 'END') AS time_interval");
    }

    sb.append(" FROM MonMetrics.Measurements ");
    String inClause = MetricQueries.createDefDimIdInClause(defDimIdSet);
    sb.append("WHERE to_hex(definition_dimensions_id) " + inClause);
    sb.append(createWhereClause(startTime, endTime, offset));

    if (period >= 1) {
      sb.append("group by Time_slice(time_stamp, " + period);
      sb.append(", 'SECOND', 'END') order by time_interval");
    }

    sb.append(" limit :limit");

    return sb.toString();
  }

  private String createWhereClause(
      DateTime startTime,
      DateTime endTime,
      String offset) {

    String s = "";

    if (startTime != null && endTime != null) {
      s = "AND time_stamp >= :start_time AND time_stamp <= :end_time ";
    } else if (startTime != null) {
      s = "AND time_stamp >= :start_time ";
    }

    if (offset != null && !offset.isEmpty()) {

      s += " and time_stamp > :offset ";

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
