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
package com.hpcloud.mon.infrastructure.persistence.influxdb;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;

final class Utils {
  private Utils() {}

  /**
   * InfluxDB Utilities for protecting against SQL injection attacks.
   */
  static class SQLSanitizer {
    private SQLSanitizer() {}

    private static final Pattern p = Pattern.compile("^(\\w|-|\\.)+$");

    static String sanitize(String taintedString) throws Exception {
      Matcher m = p.matcher(taintedString);
      if (!m.matches()) {
        throw new Exception(String.format("Input from user contains non-word chars[ %1$s ]. Only "
            + "" + "word chars [a-zA-Z_0-9], dash [-], and dot [.] allowed. ", taintedString));
      }

      return taintedString;
    }
  }

  /**
   * InfluxDB Utilities for building parts of where clauses.
   */
  static class WhereClauseBuilder {
    private WhereClauseBuilder() {}

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
}
