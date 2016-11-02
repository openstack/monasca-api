/*
 * (C) Copyright 2015,2016 Hewlett Packard Enterprise Development LP
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
package monasca.api.infrastructure.persistence.influxdb;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import monasca.common.util.Conversions;

public class InfluxV9Utils {
  private static final Pattern sqlUnsafePattern = Pattern.compile("^.*('|;|\")+.*$");

  static final String OFFSET_SEPARATOR = "_";
  static final Splitter
      offsetSplitter = Splitter.on(OFFSET_SEPARATOR).omitEmptyStrings().trimResults();
  static final Joiner COMMA_JOINER = Joiner.on(',');

  public InfluxV9Utils() {
  }

  public String sanitize(final String taintedString) {

    Matcher m = sqlUnsafePattern.matcher(taintedString);

    if (m.matches()) {

      throw new IllegalArgumentException(String.format("Input from user contains single quote ['] or "
                                        + "semi-colon [;] or double quote [\"] characters[ %1$s ]",
                                        taintedString));
    }

    return taintedString;
  }

  String buildTimePart(final DateTime startTime, final DateTime endTime) {
    final StringBuilder sb = new StringBuilder();

    if (startTime != null) {
      sb.append(String.format(" and time >= " + "'" + ISODateTimeFormat.dateTime().print(startTime)
              + "'"));
    }

    if (endTime != null) {
      sb.append(String.format(" and time <= " + "'" + ISODateTimeFormat.dateTime().print(endTime)
              + "'"));
    }

    return sb.toString();
  }

  public String buildAlarmsPart(List<String> alarmIds) {

    StringBuilder sb = new StringBuilder();
    for (String alarmId : alarmIds) {
      if (sb.length() > 0) {
        sb.append(" or ");
      }
      sb.append(String.format(" alarm_id = '%1$s' ", alarmId));
    }

    if (sb.length() > 0) {
      sb.insert(0, " and (");
      sb.insert(sb.length(), ")");
    }
    return sb.toString();
  }

  public String groupByPart(List<String> groupBy) {

    if (!groupBy.isEmpty() && !groupBy.contains("*"))
      return " group by " + COMMA_JOINER.join(groupBy) + ' ';
    return "group by * ";

  }

  public String namePart(String name, boolean isRequired) {

    if (isRequired) {
      if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException(String.format("Found null or empty name: %1$s", name));
      }
    }

    if (name == null || name.isEmpty()) {
      return "";
    } else {
      return String.format(" from \"%1$s\"", sanitize(name));
    }
  }

  public String publicTenantIdPart(String tenantId) {

    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException(String.format("Found null or empty tenant id: %1$s", tenantId));
    }

    return " tenant_id=" + "'" + sanitize(tenantId) + "'";

  }

  public String privateTenantIdPart(String tenantId) {

    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException(String.format("Found null or empty tenant id: %1$s", tenantId));
    }

    return " _tenant_id=" + "'" + sanitize(tenantId) + "'";

  }

  public String alarmIdPart(String alarmId) {

    if (alarmId == null || alarmId.isEmpty()) {
      return "";
    }

    return " and alarm_id=" + "'" + alarmId + "'";
  }


  public String timeOffsetPart(String offset) {

    if (StringUtils.isEmpty(offset)) {
      return StringUtils.EMPTY;
    }
    if(!"0".equals(offset)){
      Object convertible;
      try {
        convertible = Long.valueOf(offset);
      } catch (IllegalArgumentException exp) {
        // not a numeric value
        convertible = offset;
      }
      offset = Conversions.variantToDateTime(convertible).toString(ISODateTimeFormat.dateTime());
    }

    return String.format(" and time > '%1$s'", offset);
  }

  public String privateRegionPart(String region) {

    if (region == null || region.isEmpty()) {
      throw new IllegalArgumentException(String.format("Found null or empty region: %1$s", region));
    }

    return " and _region=" + "'" + sanitize(region) + "'";

  }

  public String dimPart(Map<String, String> dims) {

    StringBuilder sb = new StringBuilder();

    if (dims != null && !dims.isEmpty()) {
      for (String k : dims.keySet()) {
        String v = dims.get(k);
        if (k != null && !k.isEmpty()) {
          sb.append(" and \"" + sanitize(k) + "\"");
          if (Strings.isNullOrEmpty(v)) {
            sb.append("=~ /.*/");
          } else if (v.contains("|")) {
            sb.append("=~ " + "/^" + sanitize(v) + "$/");
          } else {
            sb.append("= " + "'" + sanitize(v) + "'");
          }
        }
      }
    }

    return sb.toString();
  }

  public String startTimePart(DateTime startTime) {

    return startTime != null ? " and time > " + "'" + ISODateTimeFormat.dateTime().print(startTime)
                               + "'" : "";
  }

  public String endTimePart(DateTime endTime) {

    return endTime != null ? " and time < " + "'" + ISODateTimeFormat.dateTime().print(endTime)
                             + "'" : "";
  }

  public String limitPart(int limit) {

    // We add 1 to limit to determine if we need to insert a next link.
    return String.format(" limit %1$d", limit + 1);
  }

  public String offsetPart(int startIndex) {

    return String.format(" offset %1$d", startIndex);
  }

  public int startIndex(String offset)  {

    if (offset == null || offset.isEmpty()) {

      return 0;

    }

    int intOffset;

    try {

      intOffset = Integer.parseInt(offset);

    } catch (NumberFormatException nfe) {

      throw new IllegalArgumentException(
          String.format("Found non-integer offset '%1$s'. Offset must be a positive integer", offset));
    }

    if (intOffset < 0) {

      throw new IllegalArgumentException(
          String.format("Found negative offset '%1$s'. Offset must be a positive integer", offset));

    }

    // We've already returned up to offset, so return offset + 1.
    return intOffset + 1;
  }

  public String startTimeEndTimePart(DateTime startTime, DateTime endTime) {

    return buildTimePart(startTime, endTime);
  }

  public String alarmIdsPart(List<String> alarmIdList) {

    return buildAlarmsPart(alarmIdList);

  }

  public String periodPartWithGroupBy(int period, List<String> groupBy) {
    if (period <= 0) {
      period = 300;
    }

    String periodStr = ",time(" + period + "s)";

    return String.format(" group by %1$s%2$s", COMMA_JOINER.join(groupBy), periodStr);
  }

  public String periodPart(int period, Boolean mergeMetricsFlag) {
    String periodStr = period > 0 ? String.format(" group by time(%1$ds)", period)
                      : " group by time(300s)";
    periodStr += mergeMetricsFlag ? "" : ", *";

    return periodStr;
  }

  Map<String, String> filterPrivateTags(Map<String, String> tagMap) {

    Map<String, String> filteredMap = new HashMap<>(tagMap);

    filteredMap.remove("_tenant_id");
    filteredMap.remove("_region");

    return filteredMap;
  }

  Map<String, String> filterGroupByTags(Map<String, String> tagMap, List<String> groupBy) {
    Map<String, String> filteredMap = new HashMap<>(tagMap);

    for (String key : tagMap.keySet()) {
      if (!groupBy.contains(key))
        filteredMap.remove(key);
    }

    return filteredMap;
  }

  public String threeDigitMillisTimestamp(String origTimestamp) {
    final int length = origTimestamp.length();
    final String timestamp;
    if (length == 20) {
      timestamp = origTimestamp.substring(0, 19) + ".000Z";
    } else {
      final String millisecond = origTimestamp.substring(20, length - 1);
      final String millisecond_3d = StringUtils.rightPad(millisecond, 3, '0');
      timestamp = origTimestamp.substring(0, 19) + '.' + millisecond_3d + 'Z';
    }
    return timestamp;
  }

  public List<String> parseMultiOffset(String offsetStr) {
    return offsetSplitter.splitToList(offsetStr);
  }

  public Map<String, String> getDimensions(String[] vals, String[] cols) {
    Map<String, String> dims = new HashMap<>();
    for (int i = 0; i < cols.length; ++i) {

      // Dimension names that start with underscore are reserved. I.e., _key, _region, _tenant_id.
      // Influxdb inserts _key.
      // Monasca Persister inserts _region and _tenant_id.
      if (!cols[i].startsWith("_")) {
        if (!vals[i].equalsIgnoreCase("null")) {
          dims.put(cols[i], vals[i]);
        }
      }
    }
    return dims;
  }
}
