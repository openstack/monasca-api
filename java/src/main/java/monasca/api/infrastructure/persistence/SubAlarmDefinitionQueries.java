/*
 * (C) Copyright 2014,2016 Hewlett Packard Enterprise Development Company LP
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
package monasca.api.infrastructure.persistence;

import com.google.common.base.Strings;

import org.skife.jdbi.v2.Query;

import java.util.Map;

/**
 * Utilities for building sub alarm queries.
 */
public final class SubAlarmDefinitionQueries {
  private SubAlarmDefinitionQueries() {}

  public static String buildJoinClauseFor(Map<String, String> dimensions) {

    StringBuilder sbJoin = new StringBuilder();

    if (dimensions != null) {

      sbJoin = new StringBuilder();

      int i = 0;
      for (String dimension_key : dimensions.keySet()) {
        sbJoin.append(" inner join sub_alarm_definition_dimension d").append(i).append(" on d")
            .append(i)
            .append(".dimension_name = :dname").append(i);
        if (!Strings.isNullOrEmpty(dimensions.get(dimension_key))) {
          sbJoin.append(" and d").append(i)
              .append(".value = :dvalue").append(i);
        }
        sbJoin.append(" and dim.sub_alarm_definition_id = d")
            .append(i).append(".sub_alarm_definition_id");
        i++;
      }
    }

    return sbJoin.toString();
  }

  public static void bindDimensionsToQuery(Query query, Map<String, String> dimensions) {
    if (dimensions != null) {
      int i = 0;
      for (Map.Entry<String, String> entry: dimensions.entrySet()) {
        query.bind("dname" + i, entry.getKey());
        query.bind("dvalue" + i, entry.getValue());
        i++;
      }
    }
  }
}
