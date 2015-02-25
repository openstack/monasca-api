/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P.
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

import com.google.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.List;
import java.util.Map;

import monasca.api.infrastructure.persistence.PersistUtils;

import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.SQLSanitizer.sanitize;
import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.WhereClauseBuilder.buildTimePart;
import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.buildAlarmsPart;

public class InfluxV9Utils {

  private static final String
      MULTIPLE_METRICS_ERROR_MSG =
      "Found multiple metrics matching search criteria. "
      + "Please refine your search criteria using additional dimensions "
      + "or specify merge_metrics=true as query param to combine "
      + "all metrics into a single series.";

  private final PersistUtils persistUtils;

  @Inject
  public InfluxV9Utils(PersistUtils persistUtils) {

    this.persistUtils = persistUtils;

  }

  public String groupByPart() {

    return " group by *";

  }

  public String namePart(String name, boolean isRequired) throws Exception {

    if (isRequired) {
      if (name == null || name.isEmpty()) {
        throw new Exception(String.format("Found null or empty name: %1$s", name));
      }
    }

    if (name == null || name.isEmpty()) {
      return "";
    } else {
      return String.format(" from \"%1$s\"", sanitize(name));
    }
  }

  public String tenantIdPart(String tenantId) throws Exception {

    if (tenantId == null || tenantId.isEmpty()) {
      throw new Exception(String.format("Found null or empty tenant id: %1$s", tenantId));
    }

    return " tenant_id=" + "'" + sanitize(tenantId) + "'";

  }

  public String alarmIdPart(String alarmId) {

    if (alarmId == null || alarmId.isEmpty()) {
      return "";
    }

    return " and alarm_id=" + "'" + alarmId + "'";
  }


  public String timeOffsetPart(String offset) {

    if (offset == null || offset.isEmpty()) {
      return "";
    }

    return String.format(" and time > '%1$s'", offset);
  }

  public String regionPart(String region) throws Exception {

    if (region == null || region.isEmpty()) {
      throw new Exception(String.format("Found null or empty region: %1$s", region));
    }

    return " and region=" + "'" + sanitize(region) + "'";

  }

  public String dimPart(Map<String, String> dims) throws Exception {

    StringBuilder sb = new StringBuilder();

    if (dims != null && !dims.isEmpty()) {
      for (String k : dims.keySet()) {
        String v = dims.get(k);
        if (k != null && !k.isEmpty() && v != null && !v.isEmpty()) {
          sb.append(" and " + sanitize(k) + "=" + "'" + sanitize(v) + "'");
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

  public int startIndex(String offset) {

    if (offset == null || offset.isEmpty()) {
      return 0;
    }

    // We've already returned up to offset, so return offset + 1.
    return Integer.parseInt(offset) + 1;
  }

  public String startTimeEndTimePart(DateTime startTime, DateTime endTime) {

    return buildTimePart(startTime, endTime);
  }

  public String alarmIdsPart(List<String> alarmIdList) {

    return buildAlarmsPart(alarmIdList);

  }

  public String periodPartWithGroupBy(int period) {

    return period > 0 ? String.format(" group by time(%1$ds), * fill(0)", period)
                       : " group by time(300s), * fill(0)";
  }

  public String periodPart(int period) {

    return period > 0 ? String.format(" group by time(%1$ds) fill(0)", period)
                      : " group by time(300s) fill(0)";
  }

  public String getMultipleMetricsErrorMsg() {

    return MULTIPLE_METRICS_ERROR_MSG;

  }
}
