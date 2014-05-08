package com.hpcloud.mon.infrastructure.persistence;

import java.util.Map;

/**
 * Utilities for building sub alarm queries.
 */
final class SubAlarmQueries {
  private SubAlarmQueries() {
  }

  static String buildJoinClauseFor(Map<String, String> dimensions) {
    StringBuilder sbJoin = null;
    if (dimensions != null) {
      sbJoin = new StringBuilder();
      for (int i = 0; i < dimensions.size(); i++) {
        sbJoin.append(" inner join sub_alarm_dimension d")
            .append(i)
            .append(" on d")
            .append(i)
            .append(".dimension_name = :dname")
            .append(i)
            .append(" and d")
            .append(i)
            .append(".value = :dvalue")
            .append(i)
            .append(" and dim.sub_alarm_id = d")
            .append(i)
            .append(".sub_alarm_id");
      }
    }

    return sbJoin == null ? "" : sbJoin.toString();
  }
}
