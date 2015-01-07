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
package monasca.api.infrastructure.persistence.mysql;

import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmRepository;
import monasca.api.domain.model.common.Paged;
import monasca.api.infrastructure.persistence.DimensionQueries;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import monasca.common.persistence.BeanMapper;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class AlarmMySqlRepositoryImpl implements AlarmRepository {
  private static final Logger logger = LoggerFactory.getLogger(AlarmMySqlRepositoryImpl.class);

  private final DBI db;
  private static final String ALARM_SQL =
      "select ad.id as alarm_definition_id, ad.severity, ad.name as alarm_definition_name, "
          + "a.id, a.state, "
          + "md.name as metric_name, mdg.dimensions as metric_dimensions from alarm as a "
          + "inner join alarm_definition ad on ad.id = a.alarm_definition_id "
          + "inner join alarm_metric as am on am.alarm_id = a.id "
          + "inner join metric_definition_dimensions as mdd on mdd.id = am.metric_definition_dimensions_id "
          + "inner join metric_definition as md on md.id = mdd.metric_definition_id "
          + "left join (select dimension_set_id, name, value, group_concat(name, '=', value) as dimensions "
          + "from metric_dimension group by dimension_set_id) as mdg on mdg.dimension_set_id = mdd.metric_dimension_set_id "
          + "where ad.tenant_id = :tenantId and ad.deleted_at is null %s order by a.id %s";

  @Inject
  public AlarmMySqlRepositoryImpl(@Named("mysql") DBI db) {
    this.db = db;
  }

  private void buildJoinClauseFor(Map<String, String> dimensions, StringBuilder sbJoin) {
    if (dimensions == null) {
      return;
    }
    for (int i = 0; i < dimensions.size(); i++) {
      final String indexStr = String.valueOf(i);
      sbJoin.append(" inner join metric_dimension md").append(indexStr).append(" on md").append(indexStr)
            .append(".name = :dname").append(indexStr).append(" and md").append(indexStr)
            .append(".value = :dvalue").append(indexStr).append(" and mdd.metric_dimension_set_id = md")
            .append(indexStr).append(".dimension_set_id");
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
      Map<String, String> metricDimensions, AlarmState state, String offset) {

    try (Handle h = db.open()) {

      StringBuilder sbWhere = new StringBuilder();

      if (alarmDefId != null) {
        sbWhere.append("and ad.id = :alarmDefId ");
      }
      if (metricName != null) {
        sbWhere.append(" and a.id in (select distinct a.id from alarm as a "
            + "inner join alarm_metric as am on am.alarm_id = a.id "
            + "inner join metric_definition_dimensions as mdd "
            + "  on mdd.id = am.metric_definition_dimensions_id "
            + "inner join (select distinct id from metric_definition "
            + "            where name = :metricName) as md "
            + "on md.id = mdd.metric_definition_id ");
        buildJoinClauseFor(metricDimensions, sbWhere);
        sbWhere.append(")");
      }
      if (state != null) {
        sbWhere.append(" and a.state = :state");
      }
      if (offset != null) {
        sbWhere.append(" and a.id > :offset");
      }
      String limit = offset != null ? " limit :limit" : "";

      String sql = String.format(ALARM_SQL, sbWhere, limit);
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
      if (offset != null) {
        q.bind("offset", offset);
        q.bind("limit", Paged.LIMIT);
      }

      DimensionQueries.bindDimensionsToQuery(q, metricDimensions);

      final long start = System.currentTimeMillis();
      final List<Map<String, Object>> rows = q.list();
      logger.debug("Query took {} milliseconds", System.currentTimeMillis() - start);
      final List<Alarm> alarms = createAlarms(tenantId, rows);
      return alarms;
    }
  }

  @Override
  public Alarm findById(String tenantId, String alarmId) {
    try (Handle h = db.open()) {
      return findAlarm(tenantId, alarmId, h);
    }
  }

  private Alarm findAlarm(String tenantId, String alarmId, Handle h) {
    final String sql = String.format(ALARM_SQL, " and a.id = :id", "");

    final List<Map<String, Object>> rows =
        h.createQuery(sql).bind("id", alarmId).bind("tenantId", tenantId).list();
    if (rows.isEmpty())
      throw new EntityNotFoundException("No alarm exists for %s", alarmId);

    return createAlarms(tenantId, rows).get(0);
  }

  private List<Alarm> createAlarms(String tenantId, List<Map<String, Object>> rows) {
    Alarm alarm = null;
    String previousAlarmId = null;
    final List<Alarm> alarms = new LinkedList<>();
    List<MetricDefinition> alarmedMetrics = null;
    for (final Map<String, Object> row : rows) {
      final String alarmId = (String) row.get("id");
      if (!alarmId.equals(previousAlarmId)) {
        alarmedMetrics = new ArrayList<>();
        alarm =
            new Alarm(alarmId, getString(row, "alarm_definition_id"), getString(row,
                "alarm_definition_name"), getString(row, "severity"), alarmedMetrics,
                AlarmState.valueOf(getString(row, "state")));
        alarms.add(alarm);
      }
      previousAlarmId = alarmId;
      final Map<String, String> dimensionMap = new HashMap<>();

      // Not all Metrics have dimensions (at least theoretically)
      if (row.containsKey("metric_dimensions")) {
        final String dimensions = getString(row, "metric_dimensions");
        for (String dimension : dimensions.split(",")) {
          final String[] parsed_dimension = dimension.split("=");
          dimensionMap.put(parsed_dimension[0], parsed_dimension[1]);
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
  public Alarm update(String tenantId, String id, AlarmState state) {
    Handle h = db.open();

    try {
      h.begin();
      final Alarm originalAlarm = findAlarm(tenantId, id, h);
      if (!originalAlarm.getState().equals(state)) {
        h.insert("update alarm set state = ?, updated_at = NOW() where id = ?", state.name(), id);
      }
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
          .map(new BeanMapper<SubAlarm>(SubAlarm.class)).list();
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
            .createQuery("select sa.* from sub_alarm as sa, alarm as a where sa.alarm_id=a.id and a.alarm_definition_id = :alarmDefinitionId")
            .bind("alarmDefinitionId", alarmDefinitionId).list();

        Map<String, Map<String, AlarmSubExpression>> subAlarms = new HashMap<>();
        for (Map<String, Object> row : rows) {
          final String alarmId = (String)row.get("alarm_id");
          Map<String, AlarmSubExpression> alarmMap = subAlarms.get(alarmId);
          if (alarmMap == null) {
            alarmMap = new HashMap<>();
            subAlarms.put(alarmId, alarmMap);
          }
          
          final String id = (String)row.get("id");
          final String expression = (String)row.get("expression");
          alarmMap.put(id, AlarmSubExpression.of(expression));
        }

        return subAlarms;
    }
  }
}
