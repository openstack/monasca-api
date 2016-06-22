/*
 * (C) Copyright 2015-2016 Hewlett Packard Enterprise Development Company LP
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
package monasca.api.infrastructure.persistence.mysql;

import com.google.inject.Inject;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.util.StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import monasca.api.infrastructure.persistence.Utils;
import monasca.common.model.alarm.AlarmSeverity;

public class MySQLUtils
    extends Utils {

  private static final Logger logger =
      LoggerFactory.getLogger(MySQLUtils.class);

  private final DBI mysql;

  @Inject
  public MySQLUtils(@Named("mysql") DBI mysql) {
    this.mysql = mysql;
  }

  public List<String> findAlarmIds(String tenantId,
                                   Map<String, String> dimensions) {

    final String FIND_ALARM_IDS_SQL =
        "select distinct a.id "
            + "from alarm as a "
            + "join alarm_definition as ad on a.alarm_definition_id = ad.id "
            + "%s "
            + "where ad.tenant_id = :tenantId and ad.deleted_at is NULL "
            + "order by ad.created_at";

    List<String> alarmIdList;

    try (Handle h = this.mysql.open()) {

      final String sql = String.format(FIND_ALARM_IDS_SQL, this.buildJoinClauseFor(dimensions));

      Query<Map<String, Object>> query = h.createQuery(sql).bind("tenantId", tenantId);

      logger.debug("mysql sql: {}", sql);

      this.bindDimensionsToQuery(query, dimensions);

      alarmIdList = query.map(StringMapper.FIRST).list();
    }

    return alarmIdList;
  }

  private void bindDimensionsToQuery(Query query, Map<String, String> dimensions) {
    if (dimensions != null) {
      int i = 0;
      for (Map.Entry<String, String> entry : dimensions.entrySet()) {
        query.bind("dname" + i, entry.getKey());
        query.bind("dvalue" + i, entry.getValue());
        i++;
      }
    }
  }

  public static String buildSeverityAndClause(List<AlarmSeverity> severities) {
    StringBuilder sbWhere = new StringBuilder();
    if (severities != null && !severities.isEmpty()) {
      sbWhere.append(" and (");
      for (int i = 0; i < severities.size(); i++) {
        sbWhere.append("ad.severity = :severity").append(i);
        if (i < severities.size() - 1) {
          sbWhere.append(" or ");
        }
      }
      sbWhere.append(") ");
    }
    return sbWhere.toString();
  }

  public static void bindSeverityToQuery(Query query, List<AlarmSeverity> severities) {
    if (severities != null && !severities.isEmpty()) {
      for (int i = 0; i < severities.size(); i++) {
        query.bind("severity" + String.valueOf(i), severities.get(i).name());
      }
    }
  }

}
