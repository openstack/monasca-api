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

import org.joda.time.DateTime;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Utils {

  // Serie names match this pattern.
  private static final Pattern serieNamePattern = Pattern.compile("^.+\\?.+&.+(&.+=.+)*$");

  private Utils() {
  }

  /**
   * InfluxDB Utilities for protecting against SQL injection attacks.
   */
  static class SQLSanitizer {

    private SQLSanitizer() {
    }

    private static final Pattern p = Pattern.compile("^.*('|;)+.*$");

    static String sanitize(String taintedString) throws Exception {
      Matcher m = p.matcher(taintedString);
      if (m.matches()) {
        throw new Exception(String.format("Input from user contains single quote ['] or " +
                                          "semi-colon [;] characters[ %1$s ]", taintedString));
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

  static String buildSerieNameRegex(String tenantId, String name,
                                    Map<String, String> dimensions) throws Exception {
    StringBuilder regex = new StringBuilder("^");

    // Name is optional.
    if (name != null) {
      regex.append(urlEncodeUTF8(name));
    } else {
      regex.append(".+");
    }

    // Tenant ID will always be included in the regex.
    regex.append("\\?");
    regex.append(urlEncodeUTF8(tenantId));

    // Dimensions are optional.
    if (dimensions != null && !dimensions.isEmpty()) {

      // We depend on the fact that dimensions are sorted in the series name.
      TreeSet<Dimension> dimSortedSet = buildSortedDimSet(dimensions);

      for (Dimension dim : dimSortedSet) {
        regex.append(".*&");
        regex.append(urlEncodeUTF8(dim.name));
        regex.append("=");
        regex.append(urlEncodeUTF8(dim.value));
      }
    }
    return regex.toString();
  }

  static String urlEncodeUTF8(String s) throws Exception {
    return URLEncoder.encode(Utils.SQLSanitizer.sanitize(s), "UTF-8");
  }


  static String urlDecodeUTF8(String s) throws UnsupportedEncodingException {
    return URLDecoder.decode(s, "UTF-8");
  }

  static TreeSet<Dimension> buildSortedDimSet(Map<String, String> dimMap) {

    TreeSet<Dimension> dimTreeSet = new TreeSet<>();
    for (String dimName : dimMap.keySet()) {
      Dimension dim = new Dimension(dimName, dimMap.get(dimName));
      dimTreeSet.add(dim);
    }
    return dimTreeSet;
  }

  static final class Dimension implements Comparable<Dimension> {

    String name;
    String value;

    private Dimension(String name, String value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public int compareTo(Dimension o) {
      int nameCmp = String.CASE_INSENSITIVE_ORDER.compare(name, o.name);
      return (nameCmp != 0 ? nameCmp : String.CASE_INSENSITIVE_ORDER.compare(value, o.value));
    }
  }

  static class SerieNameConverter {

    private final String serieName;
    private final String metricName;
    private final String tenantId;
    private final String region;
    private final Map<String, String> dimensions;

    SerieNameConverter(String serieName) throws UnsupportedEncodingException {

      this.serieName = serieName;

      this.metricName = urlDecodeUTF8(serieName.substring(0, serieName.indexOf('?')));
      String rest = serieName.substring(serieName.indexOf('?') + 1);

      this.tenantId = urlDecodeUTF8(rest.substring(0, rest.indexOf('&')));
      rest = rest.substring(rest.indexOf('&') + 1);

      if (rest.contains("&")) {
        this.region = urlDecodeUTF8(rest.substring(0, rest.indexOf('&')));
        rest = rest.substring(rest.indexOf('&') + 1);
      } else {
        this.region = urlDecodeUTF8(rest);
        rest = null;
      }

      // It's possible to have no dimensions.
      this.dimensions = new HashMap();
      while (rest != null) {
        String nameValPair;
        if (rest.contains("&")) {
          nameValPair = rest.substring(0, rest.indexOf('&'));
          rest = rest.substring(rest.indexOf('&') + 1);
        } else {
          nameValPair = rest;
          rest = null;
        }
        String dimName = urlDecodeUTF8(nameValPair.split("=")[0]);
        String dimVal = urlDecodeUTF8(nameValPair.split("=")[1]);
        this.dimensions.put(dimName, dimVal);

      }
    }

    public String getSerieName() {
      return serieName;
    }

    public String getMetricName() {
      return metricName;
    }

    public String getTenantId() {
      return tenantId;
    }

    public String getRegion() {
      return region;
    }

    public Map<String, String> getDimensions() {
      return dimensions;
    }

  }

  /**
   * We might come across other series that are created by the persister or don't pertain to metric
   * data.  They will break the parsing. Throw them away.
   */

  static boolean serieNameMatcher(String serieName) {
    Matcher m = serieNamePattern.matcher(serieName);
    return m.matches();
  }
}
