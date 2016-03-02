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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;

import monasca.common.persistence.SqlQueries;

/**
 * Vertica utilities for building metric queries.
 */
final class MetricQueries {
  private MetricQueries() {}

  static String buildDimensionAndClause(Map<String, String> dimensions,
                                        String tableToJoinName,
                                        int limit) {

    StringBuilder sb = null;

    if (dimensions != null && dimensions.size() > 0) {

      int numDims = dimensions.size();
      sb = new StringBuilder();
      sb.append(" and " + tableToJoinName + ".dimension_set_id in ")
        .append("(select dimension_set_id from MonMetrics.Dimensions where ");

      for (int i = 0; i < numDims; i++) {
        sb.append("name = :dname")
          .append(i)
          .append(" and value = :dvalue")
          .append(i);
        if (i != (numDims - 1)) {
           sb.append(" or ");
        }
      }

      sb.append(" group by dimension_set_id ")
        .append(" having count(*) = " + numDims +" ");

      //
      // Limit is non-deterministic here unless we also
      // order by.
      //
      if (limit > 0) {
        sb.append("order by dimension_set_id ")
          .append("limit " + Integer.toString(limit + 1));
      }

      sb.append(")");
    }

    return sb == null ? "" : sb.toString();
  }

  static void bindDimensionsToQuery(Query<?> query, Map<String, String> dimensions) {
    if (dimensions != null) {
      int i = 0;
      for (Iterator<Map.Entry<String, String>> it = dimensions.entrySet().iterator(); it.hasNext(); i++) {
        Map.Entry<String, String> entry = it.next();
        query.bind("dname" + i, entry.getKey());
        query.bind("dvalue" + i, entry.getValue());
      }
    }
  }


  static Map<String, String> dimensionsFor(Handle handle, byte[] dimensionSetId) {

    return SqlQueries.keyValuesFor(handle, "select name, value from MonMetrics.Dimensions "
        + "where" + " dimension_set_id = ?", dimensionSetId);
  }

  static String createDefDimIdInClause(Set<byte[]> defDimIdSet) {

    StringBuilder sb = new StringBuilder("IN ");

    sb.append("(");

    boolean first = true;
    for (byte[] defDimId : defDimIdSet) {

      if (first) {
        first = false;
      } else {
        sb.append(",");
      }

      sb.append("'" + Hex.encodeHexString(defDimId) + "'");
    }

    sb.append(") ");

    return sb.toString();
  }
}
