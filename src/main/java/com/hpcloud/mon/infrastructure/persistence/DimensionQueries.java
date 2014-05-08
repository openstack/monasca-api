package com.hpcloud.mon.infrastructure.persistence;

import java.util.Iterator;
import java.util.Map;

import org.skife.jdbi.v2.Query;

/**
 * Utilities for querying dimensions.
 */
public final class DimensionQueries {
  private DimensionQueries() {
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
