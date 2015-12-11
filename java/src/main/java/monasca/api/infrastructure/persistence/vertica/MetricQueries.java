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

import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.skife.jdbi.v2.Handle;

import monasca.common.persistence.SqlQueries;

/**
 * Vertica utilities for building metric queries.
 */
final class MetricQueries {
  private MetricQueries() {}

  static String buildDimensionAndClause(Map<String, String> dimensions, String tableToJoinName) {

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
        .append(" having count(*) = " + numDims +") ");
    }

    return sb == null ? "" : sb.toString();
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
