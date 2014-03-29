package com.hpcloud.mon.infrastructure.persistence;

import java.util.Map;

import org.skife.jdbi.v2.Handle;

import com.hpcloud.persistence.SqlQueries;

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

  static Map<String, String> dimensionsFor(Handle handle, byte[] definitionId) {
    return SqlQueries.keyValuesFor(handle,
        "select name, value from MonMetrics.Dimensions where definition_id = ?", definitionId);
  }
}
