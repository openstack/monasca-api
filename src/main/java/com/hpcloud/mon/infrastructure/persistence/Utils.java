package com.hpcloud.mon.infrastructure.persistence;

import com.hpcloud.persistence.SqlQueries;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Utils {

  private Utils() {
  }

  /**
   * InfluxDB Utilities for protecting against SQL injection attacks.
   */
  static class SQLSanitizer {

    private SQLSanitizer() {
    }

    private static final Pattern p = Pattern.compile("^(\\w|-|\\.)+$");

    static String sanitize(String taintedString) throws Exception {

      Matcher m = p.matcher(taintedString);
      if (!m.matches()) {
        throw new Exception(String.format("Input from user contains non-word chars[ %1$s ]. Only " +
            "" + "word chars [a-zA-Z_0-9], dash [-], and dot [.] allowed. ", taintedString));
      }

      return taintedString;
    }
  }

  /**
   * InfluxDB Utilities for building parts of where clauses.
   */
  static class WhereClauseBuilder {

    private WhereClauseBuilder() {
    }

    static String buildTimePart(DateTime startTime, DateTime endTime) {

      StringBuilder sb = new StringBuilder();

      if (startTime != null) {
        sb.append(String.format(" and time > %1$ds", startTime.getMillis() / 1000));
      }

      if (endTime != null) {
        sb.append(String.format(" and time < %1$ds", endTime.getMillis() / 1000));
      }

      return sb.toString();
    }

    static String buildDimsPart(Map<String, String> dims) throws Exception {

      StringBuilder sb = new StringBuilder();
      if (dims != null) {
        for (String colName : dims.keySet()) {
          sb.append(String.format(" and %1$s = '%2$s'", Utils.SQLSanitizer.sanitize(colName),
              Utils.SQLSanitizer.sanitize(dims.get(colName))));
        }
      }
      return sb.toString();
    }
  }

  /**
   * Vertica utilities for building metric queries.
   */
  static final class MetricQueries {
    private MetricQueries() {
    }

    static String buildJoinClauseFor(Map<String, String> dimensions) {
      StringBuilder sbJoin = null;
      if (dimensions != null) {
        sbJoin = new StringBuilder();
        for (int i = 0; i < dimensions.size(); i++)
          sbJoin.append(" inner join MonMetrics.Dimensions d").append(i).append(" on d").append
              (i).append(".name = :dname").append(i).append(" and d").append(i).append(".value = " +
              ":dvalue").append(i).append(" and dd.dimension_set_id = d").append(i).append("" +
              ".dimension_set_id");
      }

      return sbJoin == null ? "" : sbJoin.toString();
    }

    static Map<String, String> dimensionsFor(Handle handle, byte[] dimensionSetId) {
      return SqlQueries.keyValuesFor(handle, "select name, value from MonMetrics.Dimensions " +
          "where" + " dimension_set_id = ?", dimensionSetId);
    }
  }

  /**
   * Vertica Utilities for building sub alarm queries.
   */
  static final class SubAlarmQueries {
    private SubAlarmQueries() {
    }

    static String buildJoinClauseFor(Map<String, String> dimensions) {
      StringBuilder sbJoin = null;
      if (dimensions != null) {
        sbJoin = new StringBuilder();
        for (int i = 0; i < dimensions.size(); i++) {
          sbJoin.append(" inner join sub_alarm_dimension d").append(i).append(" on d").append(i)
              .append(".dimension_name = :dname").append(i).append(" and d").append(i).append("" +
              ".value = :dvalue").append(i).append(" and dim.sub_alarm_id = d").append(i).append
              (".sub_alarm_id");
        }
      }

      return sbJoin == null ? "" : sbJoin.toString();
    }
  }

  /**
   * Vertica Utilities for querying dimensions.
   *
   * This class has issues with testing with mockito because bind method on Query class
   * is final.
   */
  public static final class DimensionQueries {
    private DimensionQueries() {
    }

    static void bindDimensionsToQuery(Query<?> query, Map<String, String> dimensions) {
      if (dimensions != null) {
        int i = 0;
        for (Iterator<Map.Entry<String, String>> it = dimensions.entrySet().iterator(); it
            .hasNext(); i++) {
          Map.Entry<String, String> entry = it.next();
          query.bind("dname" + i, entry.getKey());
          query.bind("dvalue" + i, entry.getValue());
        }
      }
    }
  }
}
