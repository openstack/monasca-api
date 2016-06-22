/*
 * Copyright (c) 2014-2016 Hewlett Packard Enterprise Development Company, L.P.
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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmCount;
import monasca.api.domain.model.alarm.AlarmRepo;
import monasca.api.infrastructure.persistence.DimensionQueries;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import monasca.common.persistence.BeanMapper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Alarmed metric repository implementation.
 */
public class AlarmMySqlRepoImpl implements AlarmRepo {

  private static final Joiner COMMA_JOINER = Joiner.on(',');
  private static final Splitter SPACE_SPLITTER = Splitter.on(' ');
  private static final Logger logger = LoggerFactory.getLogger(AlarmMySqlRepoImpl.class);

  private final DBI db;
  private final PersistUtils persistUtils;

  private static final String FIND_ALARM_BY_ID_SQL =
      "select ad.id as alarm_definition_id, ad.severity, ad.name as alarm_definition_name, "
      + "a.id as alarm_id, a.state, a.lifecycle_state, a.link, a.state_updated_at as state_updated_timestamp, "
      + "a.updated_at as updated_timestamp, a.created_at as created_timestamp, "
      + "md.name as metric_name, mdg.dimensions as metric_dimensions from alarm as a "
      + "inner join alarm_definition ad on ad.id = a.alarm_definition_id "
      + "inner join alarm_metric as am on am.alarm_id = a.id "
      + "inner join metric_definition_dimensions as mdd on mdd.id = am.metric_definition_dimensions_id "
      + "inner join metric_definition as md on md.id = mdd.metric_definition_id "
      + "left outer join (select dimension_set_id, name, value, group_concat(name, '=', value) as dimensions "
      + "from metric_dimension group by dimension_set_id) as mdg on mdg.dimension_set_id = mdd.metric_dimension_set_id "
      + "where ad.tenant_id = :tenantId and ad.deleted_at is null %s order by a.id %s";

  private static final String FIND_ALARMS_SQL =
      "select ad.id as alarm_definition_id, ad.severity, ad.name as alarm_definition_name, "
      + "a.id as alarm_id, a.state, a.lifecycle_state, a.link, a.state_updated_at as state_updated_timestamp, "
      + "a.updated_at as updated_timestamp, a.created_at as created_timestamp, "
      + "md.name as metric_name, group_concat(mdim.name, '=', mdim.value order by mdim.name) as metric_dimensions "
      + "from alarm as a "
      + "inner join %1$s as alarm_id_list on alarm_id_list.id = a.id "
      + "inner join alarm_definition ad on ad.id = a.alarm_definition_id "
      + "inner join alarm_metric as am on am.alarm_id = a.id "
      + "inner join metric_definition_dimensions as mdd on mdd.id = am.metric_definition_dimensions_id "
      + "inner join metric_definition as md on md.id = mdd.metric_definition_id "
      + "left outer join metric_dimension as mdim on mdim.dimension_set_id = mdd.metric_dimension_set_id "
      + "group by a.id, md.name, mdim.dimension_set_id "
      + "%2$s";

  @Inject
  public AlarmMySqlRepoImpl(@Named("mysql") DBI db, PersistUtils persistUtils) {
    this.db = db;
    this.persistUtils = persistUtils;
  }

  private void buildJoinClauseFor(Map<String, String> dimensions, StringBuilder sbJoin) {

    if (dimensions == null) {
      return;
    }
    int i = 0;
    for (String dimension_key : dimensions.keySet()) {
      final String indexStr = String.valueOf(i);
      sbJoin.append(" inner join metric_dimension md").append(indexStr).append(" on md")
          .append(indexStr)
          .append(".name = :dname").append(indexStr);
      String dim_value = dimensions.get(dimension_key);
      if (!Strings.isNullOrEmpty(dim_value)) {
        sbJoin.append(" and (");
        List<String> values = Splitter.on('|').splitToList(dim_value);
        for (int j = 0; j < values.size(); j++) {
          sbJoin.append(" md").append(indexStr)
              .append(".value = :dvalue").append(indexStr).append('_').append(j);
          if (j < values.size() - 1) {
            sbJoin.append(" or");
          }
        }
        sbJoin.append(")");
      }
      sbJoin.append(" and mdd.metric_dimension_set_id = md")
          .append(indexStr).append(".dimension_set_id");
      i++;
    }
  }

  @Override
  public void deleteById(String tenantId, String id) {
    final String sql = "delete a from alarm a where a.id = ?";

    try (Handle h = db.open()) {
      // This will throw an EntityNotFoundException if Alarm doesn't exist or has a different tenant id
      findAlarm(tenantId, id, h);
      h.execute(sql, id);
    }
  }

  @Override
  public List<Alarm> find(String tenantId, String alarmDefId, String metricName,
                          Map<String, String> metricDimensions, AlarmState state,
                          List<AlarmSeverity> severities, String lifecycleState, String link,
                          DateTime stateUpdatedStart, List<String> sortBy,
                          String offset, int limit, boolean enforceLimit) {

    StringBuilder
        sbWhere =
        new StringBuilder("(select a.id "
                          + "from alarm as a, alarm_definition as ad "
                          + "where ad.id = a.alarm_definition_id "
                          + "  and ad.deleted_at is null "
                          + "  and ad.tenant_id = :tenantId ");

    if (alarmDefId != null) {
      sbWhere.append(" and ad.id = :alarmDefId ");
    }

    if (metricName != null) {

      sbWhere.append(" and a.id in (select distinct a.id from alarm as a "
                     + "inner join alarm_metric as am on am.alarm_id = a.id "
                     + "inner join metric_definition_dimensions as mdd "
                     + "  on mdd.id = am.metric_definition_dimensions_id "
                     + "inner join (select distinct id from metric_definition "
                     + "            where name = :metricName) as md "
                     + "  on md.id = mdd.metric_definition_id ");

      buildJoinClauseFor(metricDimensions, sbWhere);

      sbWhere.append(")");

    } else if (metricDimensions != null) {

      sbWhere.append(" and a.id in (select distinct a.id from alarm as a "
                     + "inner join alarm_metric as am on am.alarm_id = a.id "
                     + "inner join metric_definition_dimensions as mdd "
                     + "  on mdd.id = am.metric_definition_dimensions_id ");

      buildJoinClauseFor(metricDimensions, sbWhere);

      sbWhere.append(")");

    }

    if (state != null) {
      sbWhere.append(" and a.state = :state");
    }

    sbWhere.append(MySQLUtils.buildSeverityAndClause(severities));

    if (lifecycleState != null) {
      sbWhere.append(" and a.lifecycle_state = :lifecycleState");
    }

    if (link != null) {
      sbWhere.append(" and a.link = :link");
    }

    if (stateUpdatedStart != null) {
      sbWhere.append(" and a.state_updated_at >= :stateUpdatedStart");
    }

    StringBuilder orderClause = new StringBuilder();

    if (sortBy != null && !sortBy.isEmpty()) {
      // Convert friendly names to column names
      replaceFieldName(sortBy, "alarm_id", "a.id");
      replaceFieldName(sortBy, "alarm_definition_id", "ad.id");
      replaceFieldName(sortBy, "alarm_definition_name", "ad.name");
      replaceFieldName(sortBy, "created_timestamp", "a.created_at");
      replaceFieldName(sortBy, "updated_timestamp", "a.updated_at");
      replaceFieldName(sortBy, "state_updated_timestamp", "a.state_updated_at");
      replaceFieldName(sortBy, "state", "FIELD(state, \"OK\", \"UNDETERMINED\", \"ALARM\")");
      replaceFieldName(sortBy, "severity", "FIELD(severity, \"LOW\", \"MEDIUM\", \"HIGH\", \"CRITICAL\")");

      orderClause.append(" order by ");
      orderClause.append(COMMA_JOINER.join(sortBy));
      // if alarm_id is not in the list, add it
      if (orderClause.indexOf("a.id") == -1) {
        orderClause.append(",a.id ASC");
      }
      orderClause.append(' ');
    } else {
      orderClause.append(" order by a.id ASC ");
    }

    sbWhere.append(orderClause);

    if (enforceLimit && limit > 0) {
      sbWhere.append(" limit :limit");
    }

    if (offset != null) {
      sbWhere.append(" offset ");
      sbWhere.append(offset);
      sbWhere.append(' ');
    }

    sbWhere.append(")");

    String sql = String.format(FIND_ALARMS_SQL, sbWhere, orderClause);

    try (Handle h = db.open()) {

      final Query<Map<String, Object>> q = h.createQuery(sql).bind("tenantId", tenantId);

      if (alarmDefId != null) {
        q.bind("alarmDefId", alarmDefId);
      }

      if (metricName != null) {
        q.bind("metricName", metricName);
      }

      if (state != null) {
        q.bind("state", state.name());
      }

      MySQLUtils.bindSeverityToQuery(q, severities);

      if (lifecycleState != null) {
        q.bind("lifecycleState", lifecycleState);
      }

      if (link != null) {
        q.bind("link", link);
      }

      if (stateUpdatedStart != null) {
        q.bind("stateUpdatedStart", stateUpdatedStart.toString());
      }

      if (enforceLimit && limit > 0) {
        q.bind("limit", limit + 1);
      }

      DimensionQueries.bindDimensionsToQuery(q, metricDimensions);

      final List<Map<String, Object>> rows = q.list();

      return createAlarms(tenantId, rows);

    }
  }

  private void replaceFieldName(List<String> list, String oldString, String newString) {
    for (int i = 0; i < list.size(); i++) {
      String listElement = list.get(i);
      String columnName = SPACE_SPLITTER.splitToList(listElement).get(0);
      if (columnName.equals(oldString)) {
        list.set(i, listElement.replace(oldString, newString));
      }
    }
  }

  @Override
  public Alarm findById(String tenantId, String alarmId) {
    try (Handle h = db.open()) {
      return findAlarm(tenantId, alarmId, h);
    }
  }

  private Alarm findAlarm(String tenantId, String alarmId, Handle h) {

    final String sql = String.format(FIND_ALARM_BY_ID_SQL, " and a.id = :id", "");

    final List<Map<String, Object>> rows = h.createQuery(sql).bind("id", alarmId)
            .bind("tenantId", tenantId)
            .list();

    if (rows.isEmpty()) {
      throw new EntityNotFoundException("No alarm exists for %s", alarmId);
    }

    return createAlarms(tenantId, rows).get(0);
  }

  private List<Alarm> createAlarms(String tenantId, List<Map<String, Object>> rows) {
    Alarm alarm;
    String previousAlarmId = null;
    final List<Alarm> alarms = new LinkedList<>();
    List<MetricDefinition> alarmedMetrics = null;
    for (final Map<String, Object> row : rows) {
      final String alarmId = (String) row.get("alarm_id");
      if (!alarmId.equals(previousAlarmId)) {
        alarmedMetrics = new ArrayList<>();
        alarm =
            new Alarm(alarmId, getString(row, "alarm_definition_id"), getString(row,
                      "alarm_definition_name"), getString(row, "severity"), alarmedMetrics,
                      AlarmState.valueOf(getString(row, "state")),
                      getString(row, "lifecycle_state"),
                      getString(row, "link"),
                      new DateTime(((Timestamp)row.get("state_updated_timestamp")).getTime(), DateTimeZone.forID("UTC")),
                      new DateTime(((Timestamp)row.get("updated_timestamp")).getTime(), DateTimeZone.forID("UTC")),
                      new DateTime(((Timestamp)row.get("created_timestamp")).getTime(), DateTimeZone.forID("UTC")));
        alarms.add(alarm);
      }
      previousAlarmId = alarmId;
      final Map<String, String> dimensionMap = new HashMap<>();

      // Not all Metrics have dimensions (at least theoretically)
      if (row.containsKey("metric_dimensions")) {
        final String dimensions = getString(row, "metric_dimensions");
        if (dimensions != null && !dimensions.isEmpty()) {
          for (String dimension : dimensions.split(",")) {
            final String[] parsed_dimension = dimension.split("=");
            if (parsed_dimension.length == 2) {
              dimensionMap.put(parsed_dimension[0], parsed_dimension[1]);
            } else {
              logger.error("Failed to parse dimension. Dimension is malformed: {}", dimension);
            }
          }
        }
      }

      alarmedMetrics.add(new MetricDefinition(getString(row, "metric_name"), dimensionMap));
    }
    return alarms;
  }

  private String getString(final Map<String, Object> row, String fieldName) {
    return (String) row.get(fieldName);
  }

  @Override
  public Alarm update(String tenantId, String id, AlarmState state, String lifecycleState, String link) {
    Handle h = db.open();

    try {
      h.begin();
      final Alarm originalAlarm = findAlarm(tenantId, id, h);
      if (!originalAlarm.getState().equals(state)) {
        h.insert(
            "update alarm set state = ?, state_updated_at = NOW() where id = ?",
            state.name(), id);
      }
      h.insert("update alarm set lifecycle_state = ?, link = ?, updated_at = NOW() where id = ?",
               lifecycleState, link, id);
      h.commit();
      return originalAlarm;
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }

  public static class SubAlarm {

    private String id;
    private String expression;

    public SubAlarm() {
    }

    public SubAlarm(String id, String expression) {
      this.id = id;
      this.expression = expression;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getExpression() {
      return expression;
    }

    public void setExpression(String expression) {
      this.expression = expression;
    }
  }

  @Override
  public Map<String, AlarmSubExpression> findAlarmSubExpressions(String alarmId) {
    try (Handle h = db.open()) {
      final List<SubAlarm> result = h
          .createQuery("select * from sub_alarm where alarm_id = :alarmId")
          .bind("alarmId", alarmId)
          .map(new BeanMapper<>(SubAlarm.class)).list();
      final Map<String, AlarmSubExpression> subAlarms = new HashMap<>(result.size());

      for (SubAlarm row : result) {
        subAlarms.put(row.id, AlarmSubExpression.of(row.expression));
      }

      return subAlarms;
    }
  }

  @Override
  public Map<String, Map<String, AlarmSubExpression>> findAlarmSubExpressionsForAlarmDefinition(
      String alarmDefinitionId) {

    try (Handle h = db.open()) {
      final List<Map<String, Object>> rows = h
          .createQuery(
              "select sa.* from sub_alarm as sa, alarm as a where sa.alarm_id=a.id and a.alarm_definition_id = :alarmDefinitionId")
          .bind("alarmDefinitionId", alarmDefinitionId).list();

      Map<String, Map<String, AlarmSubExpression>> subAlarms = new HashMap<>();
      for (Map<String, Object> row : rows) {
        final String alarmId = (String) row.get("alarm_id");
        Map<String, AlarmSubExpression> alarmMap = subAlarms.get(alarmId);
        if (alarmMap == null) {
          alarmMap = new HashMap<>();
          subAlarms.put(alarmId, alarmMap);
        }

        final String id = (String) row.get("id");
        final String expression = (String) row.get("expression");
        alarmMap.put(id, AlarmSubExpression.of(expression));
      }

      return subAlarms;
    }
  }

  @Override
  public AlarmCount getAlarmsCount(String tenantId, String alarmDefId, String metricName,
                                   Map<String, String> metricDimensions, AlarmState state,
                                   List<AlarmSeverity> severities, String lifecycleState, String link,
                                   DateTime stateUpdatedStart, List<String> groupBy,
                                   String offset, int limit) {
    final String SELECT_CLAUSE = "SELECT count(*) as count%1$s "
                                 + " FROM alarm AS a "
                                 + " INNER JOIN alarm_definition as ad on ad.id = a.alarm_definition_id ";

    StringBuilder queryBuilder = new StringBuilder();

    String groupByStr = "";
    String metricSelect;
    if (groupBy != null) {
      groupByStr = COMMA_JOINER.join(groupBy);
      queryBuilder.append(String.format(SELECT_CLAUSE, ',' + groupByStr));

      if (groupBy.contains("metric_name") || groupBy.contains("dimension_name") || groupBy
          .contains("dimension_value")) {
        metricSelect = " INNER JOIN (SELECT distinct am.alarm_id%1$s "
                       + "FROM metric_definition AS md "
                       + "JOIN metric_definition_dimensions AS mdd on md.id = mdd.metric_definition_id "
                       + "JOIN metric_dimension AS mdim ON mdd.metric_dimension_set_id = mdim.dimension_set_id "
                       + "JOIN alarm_metric AS am ON am.metric_definition_dimensions_id = mdd.id)"
                       + "AS metrics ON a.id = metrics.alarm_id ";
        String subSelect = "";
        if (groupBy.contains("metric_name")) {
          subSelect = subSelect + ",md.name AS metric_name";
        }
        if (groupBy.contains("dimension_name")) {
          subSelect = subSelect + ",mdim.name AS dimension_name";
        }
        if (groupBy.contains("dimension_value")) {
          subSelect = subSelect + ",mdim.value AS dimension_value";
        }

        queryBuilder.append(String.format(metricSelect, subSelect));
      }
    } else {
      queryBuilder.append(String.format(SELECT_CLAUSE, groupByStr));
    }


    queryBuilder.append(" INNER JOIN (SELECT a.id "
                        + "FROM alarm AS a, alarm_definition AS ad "
                        + "WHERE ad.id = a.alarm_definition_id "
                        + "  AND ad.deleted_at IS NULL "
                        + "  AND ad.tenant_id = :tenantId ");

    if (alarmDefId != null) {
      queryBuilder.append(" AND ad.id = :alarmDefId ");
    }

    if (metricName != null) {

      queryBuilder.append(" AND a.id IN (SELECT distinct a.id FROM alarm AS a "
                     + "INNER JOIN alarm_metric AS am ON am.alarm_id = a.id "
                     + "INNER JOIN metric_definition_dimensions AS mdd "
                     + "  ON mdd.id = am.metric_definition_dimensions_id "
                     + "INNER JOIN (SELECT distinct id FROM metric_definition "
                     + "            WHERE name = :metricName) AS md "
                     + "  ON md.id = mdd.metric_definition_id ");

      buildJoinClauseFor(metricDimensions, queryBuilder);

      queryBuilder.append(")");

    } else if (metricDimensions != null) {

      queryBuilder.append(" AND a.id IN (SELECT distinct a.id FROM alarm AS a "
                     + "INNER JOIN alarm_metric AS am ON am.alarm_id = a.id "
                     + "INNER JOIN metric_definition_dimensions AS mdd "
                     + "  ON mdd.id = am.metric_definition_dimensions_id ");

      buildJoinClauseFor(metricDimensions, queryBuilder);

      queryBuilder.append(")");

    }

    if (state != null) {
      queryBuilder.append(" AND a.state = :state");
    }

    queryBuilder.append(MySQLUtils.buildSeverityAndClause(severities));

    if (lifecycleState != null) {
      queryBuilder.append(" AND a.lifecycle_state = :lifecycleState");
    }

    if (link != null) {
      queryBuilder.append(" AND a.link = :link");
    }

    if (stateUpdatedStart != null) {
      queryBuilder.append(" AND a.state_updated_at >= :stateUpdatedStart");
    }

    queryBuilder.append(") AS alarm_id_list ON alarm_id_list.id = a.id ");

    if (groupBy != null) {
      queryBuilder.append(" GROUP BY ");
      queryBuilder.append(groupByStr);
    }

    queryBuilder.append(" ORDER BY ");
    if (!Strings.isNullOrEmpty(groupByStr)) {
      queryBuilder.append(groupByStr);
    } else {
      queryBuilder.append(" a.id ");
    }

    queryBuilder.append(" LIMIT :limit");

    if (offset != null) {
      queryBuilder.append(String.format(" OFFSET %1$s ", offset));
    }



    try (Handle h = db.open()) {

      final Query<Map<String, Object>> q = h.createQuery(queryBuilder.toString()).bind("tenantId", tenantId);

      if (alarmDefId != null) {
        q.bind("alarmDefId", alarmDefId);
      }

      if (metricName != null) {
        q.bind("metricName", metricName);
      }

      if (state != null) {
        q.bind("state", state.name());
      }

      MySQLUtils.bindSeverityToQuery(q, severities);

      if (lifecycleState != null) {
        q.bind("lifecycleState", lifecycleState);
      }

      if (link != null) {
        q.bind("link", link);
      }

      if (stateUpdatedStart != null) {
        q.bind("stateUpdatedStart", stateUpdatedStart.toString());
      }

      q.bind("limit", limit + 1);

      DimensionQueries.bindDimensionsToQuery(q, metricDimensions);

      final List<Map<String, Object>> rows = q.list();

      return createAlarmCounts(groupBy, rows);

    }
  }

  private AlarmCount createAlarmCounts(List<String> groupBy, List<Map<String, Object>> rows) {
    List<List<Object>> counts = new ArrayList<>();

    // if no results, return 0 and fill columns with null
    if (rows.size() == 0) {
      List<Object> countsAndTags =  new ArrayList<>();
      countsAndTags.add(0);
      if (groupBy != null) {
        for (final String columnName : groupBy) {
          countsAndTags.add(null);
        }
      }
      counts.add(countsAndTags);
      return new AlarmCount(groupBy, counts);
    }

    for (final Map<String, Object> row : rows) {
      List<Object> countAndTags = new ArrayList<>();
      countAndTags.add(row.get("count"));
      if (groupBy != null && !groupBy.isEmpty()) {
        for (final String columnName : groupBy) {
          countAndTags.add(row.get(columnName));
        }
      }
      counts.add(countAndTags);
    }

    return new AlarmCount(groupBy, counts);
  }
}
