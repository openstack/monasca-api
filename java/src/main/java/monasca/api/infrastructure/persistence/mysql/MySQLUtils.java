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

import monasca.api.infrastructure.persistence.DimensionQueries;

public class MySQLUtils {

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

      final String sql = String.format(FIND_ALARM_IDS_SQL, buildJoinClauseFor(dimensions));

      Query<Map<String, Object>> query = h.createQuery(sql).bind("tenantId", tenantId);

      logger.debug("mysql sql: {}", sql);

      DimensionQueries.bindDimensionsToQuery(query, dimensions);

      alarmIdList = query.map(StringMapper.FIRST).list();
    }

    return alarmIdList;
  }

  private String buildJoinClauseFor(Map<String, String> dimensions) {

    if ((dimensions == null) || dimensions.isEmpty()) {
      return "";
    }

    final StringBuilder sb = new StringBuilder(
        "join alarm_metric as am on a.id=am.alarm_id "
        + "join metric_definition_dimensions as mdd on am.metric_definition_dimensions_id=mdd.id ");

    for (int i = 0; i < dimensions.size(); i++) {

      final String tableAlias = "md" + i;

      sb.append(" inner join metric_dimension ")
          .append(tableAlias)
          .append(" on ")
          .append(tableAlias)
          .append(".name = :dname")
          .append(i)
          .append(" and ")
          .append(tableAlias)
          .append(".value = :dvalue")
          .append(i)
          .append(" and mdd.metric_dimension_set_id = ")
          .append(tableAlias)
          .append(".dimension_set_id");
    }

    logger.debug("mysql dimension join clause: {}", sb.toString());

    return sb.toString();
  }

}
