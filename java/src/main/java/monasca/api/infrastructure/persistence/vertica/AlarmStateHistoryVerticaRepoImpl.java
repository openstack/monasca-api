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
package monasca.api.infrastructure.persistence.vertica;

import monasca.api.domain.model.alarmstatehistory.AlarmStateHistory;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistoryRepo;
import monasca.api.infrastructure.persistence.DimensionQueries;
import monasca.api.infrastructure.persistence.mysql.MySQLUtils;
import monasca.common.persistence.BeanMapper;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

public class AlarmStateHistoryVerticaRepoImpl implements AlarmStateHistoryRepo {

  private static final Logger logger =
      LoggerFactory.getLogger(AlarmStateHistoryVerticaRepoImpl.class);

  private static final String FIND_BY_ALARM_IDS_SQL =
      "select alarm_id, metrics, old_state, new_state, reason, reason_data, sub_alarms, time_stamp as timestamp "
      + "from MonAlarms.StateHistory "
      + "where tenant_id = :tenantId %s "
      + "order by time_stamp desc "
      + "limit :limit";

  private static final String FIND_BY_ALARM_ID_SQL =
      "select alarm_id, metrics, old_state, new_state, reason, reason_data, sub_alarms, time_stamp as timestamp "
      + "from MonAlarms.StateHistory "
      + "where tenant_id = :tenantId and alarm_id = :alarmId %s "
      + "order by time_stamp desc "
      + "limit :limit";

  private final DBI vertica;
  private final MySQLUtils mySQLUtils;

  @Inject
  public AlarmStateHistoryVerticaRepoImpl(
      @Named("vertica") DBI vertica,
      MySQLUtils mySQLUtils) {

    this.vertica = vertica;
    this.mySQLUtils = mySQLUtils;
  }

  @Override
  public List<AlarmStateHistory> findById(
      String tenantId,
      String alarmId,
      String offset,
      int limit) {

    String offsetPart = "";

    if (offset != null && !offset.isEmpty()) {

      offsetPart = (" and time_stamp < :offset");

    }

    String sql = String.format(FIND_BY_ALARM_ID_SQL, offsetPart);

    logger.debug("vertica sql: {}", sql);

    List<AlarmStateHistory> alarmStateHistoryList;

    try (Handle h = vertica.open()) {

      Query<Map<String, Object>> verticaQuery =
          h.createQuery(sql)
              .bind("tenantId", tenantId)
              .bind("alarmId", alarmId)
              .bind("limit", limit + 1);

      if (offset != null && !offset.isEmpty()) {

        verticaQuery.bind("offset", new Timestamp(Long.valueOf(offset)));

      }

      alarmStateHistoryList =
          verticaQuery.map(new BeanMapper<>(AlarmStateHistory.class)).list();

    }

    return alarmStateHistoryList;

  }

  @Override
  public List<AlarmStateHistory> find(
      String tenantId,
      Map<String, String> dimensions,
      DateTime startTime,
      @Nullable DateTime endTime,
      @Nullable String offset,
      int limit) {

    List<String> alarmIds = this.mySQLUtils.findAlarmIds(tenantId, dimensions);

    if (alarmIds == null || alarmIds.isEmpty()) {

      logger.debug("list of alarm ids is empty");

      return Collections.emptyList();

    }

    StringBuilder sb = new StringBuilder();

    sb.append(" and alarm_id in (");

    for (int i = 0; i < alarmIds.size(); i++) {

      if (i > 0) {

        sb.append(", ");
      }

      sb.append('\'').append(alarmIds.get(i)).append('\'');

    }

    sb.append(')');

    if (startTime != null) {

      sb.append(" and time_stamp >= :startTime");

    }

    if (endTime != null) {

      sb.append(" and time_stamp <= :endTime");

    }

    if (offset != null && !offset.isEmpty()) {

      sb.append(" and time_stamp < :offset");

    }

    String sql = String.format(FIND_BY_ALARM_IDS_SQL, sb);

    logger.debug("vertica sql: {}", sql);

    List<AlarmStateHistory> alarmStateHistoryList;

    try (Handle h = vertica.open()) {

      Query<Map<String, Object>> verticaQuery =
          h.createQuery(sql)
              .bind("tenantId", tenantId)
              .bind("limit", limit + 1);

      if (startTime != null) {

        logger.debug("binding startime: {}", startTime);

        verticaQuery.bind("startTime", new Timestamp(startTime.getMillis()));

      }

      if (endTime != null) {

        logger.debug("binding endtime: {}", endTime);

        verticaQuery.bind("endTime", new Timestamp(endTime.getMillis()));

      }

      if (offset != null && !offset.isEmpty()) {

        logger.debug("binding offset: {}", offset);

        verticaQuery.bind("offset", new Timestamp(Long.valueOf(offset)));

      }

      DimensionQueries.bindDimensionsToQuery(verticaQuery, dimensions);

      alarmStateHistoryList = verticaQuery.map(new BeanMapper<>(AlarmStateHistory.class)).list();

    }

    return alarmStateHistoryList;

  }
}
