package com.hpcloud.mon.infrastructure.persistence;

import java.util.Map;

final class MetricQueries {
  private MetricQueries() {
  }

  static void buildClausesForDimensions(StringBuilder sbFrom, StringBuilder sbWhere,
      Map<String, String> dimensions) {
    if (dimensions != null) {
      for (int i = 0; i < dimensions.size(); i++) {
        sbFrom.append(", MonMetrics.Dimensions d").append(i);
        sbWhere.append(" and d")
            .append(i)
            .append(".name = :dname")
            .append(i)
            .append(" and d")
            .append(i)
            .append(".value = :dvalue")
            .append(i)
            .append(" and def.id = d")
            .append(i)
            .append(".definition_id");
      }
    }
  }
}
