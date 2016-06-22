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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.Query;

/**
 * Vertica utilities for building metric queries.
 */
final class MetricQueries {
  static final Splitter BAR_SPLITTER = Splitter.on('|').omitEmptyStrings().trimResults();
  static final char OFFSET_SEPARATOR = '_';
  static final Splitter offsetSplitter = Splitter.on(OFFSET_SEPARATOR).omitEmptyStrings().trimResults();

  static final String FIND_METRIC_DEFS_SQL =
      "SELECT %s TO_HEX(defDims.id) as defDimsId, def.name, dims.name as dName, dims.value AS dValue "
      + "FROM MonMetrics.Definitions def "
      + "JOIN MonMetrics.DefinitionDimensions defDims ON def.id = defDims.definition_id "
      // Outer join needed in case there are no dimensions for a definition.
      + "LEFT OUTER JOIN MonMetrics.Dimensions dims ON dims.dimension_set_id = defDims"
      + ".dimension_set_id "
      + "WHERE defDims.id in (%s) "
      + "ORDER BY defDims.id ASC";

  static final String METRIC_DEF_SUB_SQL =
      "SELECT defDimsSub.id "
      + "FROM MonMetrics.Definitions as defSub "
      + "JOIN MonMetrics.DefinitionDimensions as defDimsSub ON defDimsSub.definition_id = defSub.id "
      + "WHERE defSub.tenant_id = :tenantId "
      + "%s " // metric name here
      + "%s " // dimension and clause here
      + "GROUP BY defDimsSub.id";

  private static final String TABLE_TO_JOIN_DIMENSIONS_ON = "defDimsSub";

  private MetricQueries() {}

  static String buildMetricDefinitionSubSql(String name, Map<String, String> dimensions) {

    String namePart = "";

    if (name != null && !name.isEmpty()) {
      namePart = "AND defSub.name = :name ";
    }

    return String.format(METRIC_DEF_SUB_SQL,
                         namePart,
                         buildDimensionAndClause(dimensions,
                                                 TABLE_TO_JOIN_DIMENSIONS_ON));
  }

  static String buildDimensionAndClause(Map<String, String> dimensions,
                                        String tableToJoinName) {

    if (dimensions == null || dimensions.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append(" and ").append(tableToJoinName).append(
              ".dimension_set_id in ( "
              + "SELECT dimension_set_id FROM MonMetrics.Dimensions WHERE (");

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

    sb.append(") GROUP BY dimension_set_id HAVING count(*) = ").append(dimensions.size()).append(") ");


    return sb.toString();
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

  static String createDefDimIdInClause(Set<String> defDimIdSet) {

    StringBuilder sb = new StringBuilder("IN ");

    sb.append("(");

    boolean first = true;
    for (String defDimId : defDimIdSet) {

      if (first) {
        first = false;
      } else {
        sb.append(",");
      }
      sb.append("'").append(defDimId).append("'");
    }

    sb.append(") ");

    return sb.toString();
  }

  static void bindOffsetToQuery(Query<Map<String, Object>> query, String offset) {
    List<String> offsets =  offsetSplitter.splitToList(offset);
    if (offsets.size() > 1) {
      query.bind("offset_id", offsets.get(0));
      query.bind("offset_timestamp",
                 new Timestamp(DateTime.parse(offsets.get(1)).getMillis()));
    } else {
      query.bind("offset_timestamp",
                 new Timestamp(DateTime.parse(offsets.get(0)).getMillis()));
    }
  }
}
