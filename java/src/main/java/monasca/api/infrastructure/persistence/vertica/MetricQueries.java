/*
 * (C) Copyright 2014,2016 Hewlett Packard Enterprise Development LP
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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.joda.time.DateTime;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;

import monasca.api.domain.exception.MultipleMetricsException;
import monasca.api.domain.model.measurement.Measurements;

/**
 * Vertica utilities for building metric queries.
 */
final class MetricQueries {
  private static final Splitter BAR_SPLITTER = Splitter.on('|').omitEmptyStrings().trimResults();
  private static final Splitter UNDERSCORE_SPLITTER = Splitter.on('_').omitEmptyStrings().trimResults();
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

  static final String FIND_METRIC_DEFS_SQL =
      "SELECT %s TO_HEX(defDims.id) as defDimsId, def.name, dims.name as dName, dims.value AS dValue "
      + "FROM MonMetrics.Definitions def "
      + "JOIN MonMetrics.DefinitionDimensions defDims ON def.id = defDims.definition_id "
      // Outer join needed in case there are no dimensions for a definition.
      + "LEFT OUTER JOIN MonMetrics.Dimensions dims ON dims.dimension_set_id = defDims"
      + ".dimension_set_id "
      + "WHERE TO_HEX(defDims.id) in (%s) "
      + "ORDER BY defDims.id ASC";

  static final String METRIC_DEF_SUB_SQL =
      "SELECT TO_HEX(defDimsSub.id) as id "
      + "FROM MonMetrics.Definitions as defSub "
      + "JOIN MonMetrics.DefinitionDimensions as defDimsSub ON defDimsSub.definition_id = defSub.id "
      + "%s " // possible measurements time join here
      + "WHERE defSub.tenant_id = :tenantId "
      + "%s " // metric name here
      + "%s " // dimension and clause here
      + "%s " // possible time and clause here
      + "GROUP BY defDimsSub.id";

  private static final String MEASUREMENT_AND_CLAUSE =
      "AND time_stamp >= :startTime "; // start or start and end time here

  private static final String MEASUREMENT_JOIN =
      "JOIN MonMetrics.Measurements AS meas ON defDimsSub.id = meas.definition_dimensions_id";

  private static final String TABLE_TO_JOIN_ON = "defDimsSub";

  private MetricQueries() {}

  static String buildMetricDefinitionSubSql(String name, Map<String, String> dimensions,
                                            DateTime startTime, DateTime endTime) {

    String namePart = "";

    if (name != null && !name.isEmpty()) {
      namePart = "AND defSub.name = :name ";
    }

    return String.format(METRIC_DEF_SUB_SQL,
                         buildTimeJoin(startTime),
                         namePart,
                         buildDimensionAndClause(dimensions, TABLE_TO_JOIN_ON),
                         buildTimeAndClause(startTime, endTime));
  }

  static String buildDimensionAndClause(Map<String, String> dimensions,
                                        String tableToJoinName) {

    if (dimensions == null || dimensions.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append(" and ").append(tableToJoinName).append(
              ".id in ( "
              + "SELECT defDimsSub2.id FROM MonMetrics.Dimensions AS dimSub " +
                "JOIN MonMetrics.DefinitionDimensions AS defDimsSub2 " +
                "ON defDimsSub2.dimension_set_id = dimSub.dimension_set_id" +
                " WHERE (");

    int i = 0;
    for (Iterator<Map.Entry<String, String>> it = dimensions.entrySet().iterator(); it.hasNext(); i++) {
      Map.Entry<String, String> entry = it.next();

      sb.append("(name = :dname").append(i);

      String dim_value = entry.getValue();
      if (!Strings.isNullOrEmpty(dim_value)) {
        List<String> values = BAR_SPLITTER.splitToList(dim_value);

        if (values.size() > 1) {
          sb.append(" and ( ");

          for (int j = 0; j < values.size(); j++) {
            sb.append("value = :dvalue").append(i).append('_').append(j);

            if (j < values.size() - 1) {
              sb.append(" or ");
            }
          }
          sb.append(")");

        } else {
          sb.append(" and value = :dvalue").append(i);
        }
      }
      sb.append(")");

      if (it.hasNext()) {
        sb.append(" or ");
      }
    }

    sb.append(") GROUP BY defDimsSub2.id,dimSub.dimension_set_id HAVING count(*) = ").append(dimensions.size()).append(") ");


    return sb.toString();
  }

  static String buildTimeAndClause(
      DateTime startTime,
      DateTime endTime)
  {
    if (startTime == null) {
      return "";
    }

    StringBuilder timeAndClause = new StringBuilder();

    timeAndClause.append(MEASUREMENT_AND_CLAUSE);

    if (endTime != null) {
      timeAndClause.append("AND time_stamp <= :endTime ");
    }

    return timeAndClause.toString();
  }

  static String buildTimeJoin(DateTime startTime)
  {
    if (startTime == null) {
      return "";
    }

    return MEASUREMENT_JOIN;
  }

  static void bindDimensionsToQuery(Query<?> query, Map<String, String> dimensions) {
    if (dimensions != null) {
      int i = 0;
      for (Iterator<Map.Entry<String, String>> it = dimensions.entrySet().iterator(); it.hasNext(); i++) {
        Map.Entry<String, String> entry = it.next();
        query.bind("dname" + i, entry.getKey());
        if (!Strings.isNullOrEmpty(entry.getValue())) {
          List<String> values = BAR_SPLITTER.splitToList(entry.getValue());
          if (values.size() > 1) {
            for (int j = 0; j < values.size(); j++) {
              query.bind("dvalue" + i + '_' + j, values.get(j));
            }
          }
          else {
            query.bind("dvalue" + i, entry.getValue());
          }
        }
      }
    }
  }

  static void bindOffsetToQuery(Query<Map<String, Object>> query, String offset) {
    List<String> offsets =  UNDERSCORE_SPLITTER.splitToList(offset);
    if (offsets.size() > 1) {
      query.bind("offset_id", offsets.get(0));
      query.bind("offset_timestamp",
                 new Timestamp(DateTime.parse(offsets.get(1)).getMillis()));
    } else {
      query.bind("offset_timestamp",
                 new Timestamp(DateTime.parse(offsets.get(0)).getMillis()));
    }
  }

  static void checkForMultipleDefinitions(Handle h, String tenantId, String name, Map<String, String> dimensions)
      throws MultipleMetricsException {

    String namePart = "";
    if (name != null && !name.isEmpty()) {
      namePart = "AND name = :name ";
    }

    String sql = String.format(METRIC_DEF_SUB_SQL,
                               "",
                               namePart,
                               buildDimensionAndClause(dimensions,
                                                 TABLE_TO_JOIN_ON),
                               "") + " limit 2";

    Query<Map<String, Object>> query = h.createQuery(sql);

    query.bind("tenantId", tenantId);

    if (name != null) {
      query.bind("name", name);
    }

    bindDimensionsToQuery(query, dimensions);

    List<Map<String, Object>> rows = query.list();

    if (rows.size() > 1) {
      throw new MultipleMetricsException(name, dimensions);
    }
  }

  static void addDefsToResults(Map<String, ? extends Measurements> results, Handle h, String dbHint) {

    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String id : results.keySet()) {
      if (first) {
        sb.append("'").append(id).append("'");
        first = false;
      } else {
        sb.append(',').append("'").append(id).append("'");
      }
    }

    String defDimSql = String.format(MetricQueries.FIND_METRIC_DEFS_SQL,
                                     dbHint,
                                     sb.toString());

    Query<Map<String, Object>> query = h.createQuery(defDimSql);

    List<Map<String, Object>> rows = query.list();

    String currentDefDimId = null;

    Map<String, String> dims = null;

    for (Map<String, Object> row : rows) {

      String defDimId = (String) row.get("defDimsId");

      String defName = (String) row.get("name");

      String dimName = (String) row.get("dName");

      String dimValue = (String) row.get("dValue");

      if (defDimId != null && !defDimId.equals(currentDefDimId)) {

        currentDefDimId = defDimId;

        dims = new HashMap<>();

        if (dimName != null && dimValue != null)
          dims.put(dimName, dimValue);

        results.get(defDimId).setId(defDimId);

        results.get(defDimId).setName(defName);

        results.get(defDimId).setDimensions(dims);

      } else {

        if (dimName != null && dimValue != null)
          dims.put(dimName, dimValue);

      }

    }
  }

  static Map<String, String> combineGroupByAndValues(List<String> groupBy, String valueStr) {
    List<String> values = COMMA_SPLITTER.splitToList(valueStr);
    Map<String, String> newDimensions = new HashMap<>();
    for (int i = 0; i < groupBy.size(); i++) {
      newDimensions.put(groupBy.get(i), values.get(i));
    }
    return newDimensions;
  }

  static String buildGroupByConcatString(List<String> groupBy) {
    if (groupBy.isEmpty() || "*".equals(groupBy.get(0)))
      return "";

    String select = "(";
    for (int i = 0; i < groupBy.size(); i++) {
      if (i > 0)
        select += " || ',' || ";
      select += "gb" + i + ".value";
    }
    select += ")";
    return select;
  }

  static String buildGroupByCommaString(List<String> groupBy) {
    String result = "";
    if (!groupBy.contains("*")) {
      for (int i = 0; i < groupBy.size(); i++) {
        if (i > 0) {
          result += ',';
        }
        result += "gb" + i + ".value";
      }
    }

    return result;
  }

  static String buildGroupBySql(List<String> groupBy) {
    if (groupBy.isEmpty() || "*".equals(groupBy.get(0)))
      return "";

    StringBuilder groupBySql = new StringBuilder(
            " JOIN MonMetrics.DefinitionDimensions as dd on dd.id = mes.definition_dimensions_id ");

    for (int i = 0; i < groupBy.size(); i++) {
      groupBySql.append("JOIN (SELECT dimension_set_id,value FROM MonMetrics.Dimensions WHERE name = ");
      groupBySql.append(":groupBy").append(i).append(") as gb").append(i);
      groupBySql.append(" ON gb").append(i).append(".dimension_set_id = dd.dimension_set_id ");
    }

    return groupBySql.toString();
  }

  static void bindGroupBy(Query<Map<String, Object>> query, List<String> groupBy) {
    int i = 0;
    for (String value: groupBy) {
      query.bind("groupBy" + i, value);
      i++;
    }
  }
}
