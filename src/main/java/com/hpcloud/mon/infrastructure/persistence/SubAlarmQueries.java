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
package com.hpcloud.mon.infrastructure.persistence;

import java.util.Map;

/**
 * Utilities for building sub alarm queries.
 */
public final class SubAlarmQueries {
  private SubAlarmQueries() {}

  public static String buildJoinClauseFor(Map<String, String> dimensions) {
    StringBuilder sbJoin = null;
    if (dimensions != null) {
      sbJoin = new StringBuilder();
      for (int i = 0; i < dimensions.size(); i++) {
        sbJoin.append(" inner join sub_alarm_dimension d").append(i).append(" on d").append(i)
            .append(".dimension_name = :dname").append(i).append(" and d").append(i)
            .append("" + ".value = :dvalue").append(i).append(" and dim.sub_alarm_id = d")
            .append(i).append(".sub_alarm_id");
      }
    }

    return sbJoin == null ? "" : sbJoin.toString();
  }
}
