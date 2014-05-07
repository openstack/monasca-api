package com.hpcloud.mon.infrastructure.persistence;


import com.hpcloud.persistence.SqlQueries;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;

import java.util.Iterator;
import java.util.Map;

/**
  * Utilities for building metric queries.
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

}
