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

import org.skife.jdbi.v2.Handle;

import monasca.common.persistence.SqlQueries;

/**
 * Vertica utilities for building metric queries.
 */
final class MetricQueries {
  private MetricQueries() {}

  static String buildJoinClauseFor(Map<String, String> dimensions) {

    StringBuilder sb = null;

    if (dimensions != null) {

      sb = new StringBuilder();

      for (int i = 0; i < dimensions.size(); i++) {

        sb
            .append(" inner join MonMetrics.Dimensions dim")
            .append(i)
            .append(" on dim")
            .append(i)
            .append(".name = :dname")
            .append(i)
            .append(" and dim")
            .append(i)
            .append(".value = " + ":dvalue")
            .append(i)
            .append(" and defdims.dimension_set_id = dim")
            .append(i)
            .append(".dimension_set_id");
      }
    }

    return sb == null ? "" : sb.toString();

  }

  static Map<String, String> dimensionsFor(Handle handle, byte[] dimensionSetId) {

    return SqlQueries.keyValuesFor(handle, "select name, value from MonMetrics.Dimensions "
        + "where" + " dimension_set_id = ?", dimensionSetId);
  }
}
