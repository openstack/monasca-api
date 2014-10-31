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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.util.StringMapper;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import monasca.common.model.alarm.AggregateFunction;
import monasca.common.model.alarm.AlarmOperator;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepository;
import monasca.api.infrastructure.persistence.DimensionQueries;
import monasca.api.infrastructure.persistence.SubAlarmQueries;
import monasca.common.persistence.BeanMapper;

/**
 * Alarm repository implementation.
 */
public class AlarmDefinitionMySqlRepositoryImpl implements AlarmDefinitionRepository {
  private static final Joiner COMMA_JOINER = Joiner.on(',');
  private static final String SUB_ALARM_SQL =
      "select sa.*, sad.dimensions from sub_alarm_definition as sa "
          + "left join (select sub_alarm_definition_id, group_concat(dimension_name, '=', value) as dimensions from sub_alarm_definition_dimension group by sub_alarm_definition_id ) as sad "
          + "on sad.sub_alarm_definition_id = sa.id where sa.alarm_definition_id = :alarmDefId";

  private final DBI db;

  @Inject
  public AlarmDefinitionMySqlRepositoryImpl(@Named("mysql") DBI db) {
    this.db = db;
  }

  @Override
  public AlarmDefinition create(String tenantId, String id, String name, String description,
      String severity, String expression, Map<String, AlarmSubExpression> subExpressions,
      List<String> matchBy, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions) {
    Handle h = db.open();

    try {
      h.begin();
      h.insert(
          "insert into alarm_definition (id, tenant_id, name, description, severity, expression, match_by, actions_enabled, created_at, updated_at, deleted_at) values (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NULL)",
          id, tenantId, name, description, severity, expression,
          matchBy == null || Iterables.isEmpty(matchBy) ? null : COMMA_JOINER.join(matchBy), true);

      // Persist sub-alarms
      createSubExpressions(h, id, subExpressions);

      // Persist actions
      persistActions(h, id, AlarmState.ALARM, alarmActions);
      persistActions(h, id, AlarmState.OK, okActions);
      persistActions(h, id, AlarmState.UNDETERMINED, undeterminedActions);

      h.commit();
      return new AlarmDefinition(id, name, description, severity, expression, matchBy, true,
          alarmActions, okActions == null ? Collections.<String>emptyList() : okActions,
          undeterminedActions == null ? Collections.<String>emptyList() : undeterminedActions);
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }

  @Override
  public void deleteById(String tenantId, String alarmDefId) {
    try (Handle h = db.open()) {
      if (h
          .update(
              "update alarm_definition set deleted_at = NOW() where tenant_id = ? and id = ? and deleted_at is NULL",
              tenantId, alarmDefId) == 0)
        throw new EntityNotFoundException("No alarm definition exists for %s", alarmDefId);
      
      // Cascade soft delete to alarms
      h.execute("delete from alarm where alarm_definition_id = :id", alarmDefId);
    }
  }

  @Override
  public boolean exists(String tenantId, String name) {
    try (Handle h = db.open()) {
      return h
          .createQuery(
              "select exists(select 1 from alarm_definition where tenant_id = :tenantId and name = :name and deleted_at is NULL)")
          .bind("tenantId", tenantId).bind("name", name).mapTo(Boolean.TYPE).first();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<AlarmDefinition> find(String tenantId, String name, Map<String, String> dimensions) {
    try (Handle h = db.open()) {
      String query =
          "select distinct ad.id, ad.description, ad.tenant_id, ad.severity, ad.expression, ad.match_by, ad.name, ad.actions_enabled, ad.created_at, ad.updated_at, ad.deleted_at "
              + "from alarm_definition ad join sub_alarm_definition sub on ad.id = sub.alarm_definition_id "
              + "left outer join sub_alarm_definition_dimension dim on sub.id = dim.sub_alarm_definition_id%s "
              + "where tenant_id = :tenantId and deleted_at is NULL %s order by ad.created_at";
      StringBuilder sbWhere = new StringBuilder();

      if (name != null) {
        sbWhere.append(" and ad.name = :name");
      }

      String sql = String.format(query, SubAlarmQueries.buildJoinClauseFor(dimensions), sbWhere);
      Query<?> q = h.createQuery(sql).bind("tenantId", tenantId);

      if (name != null) {
        q.bind("name", name);
      }

      q = q.map(new BeanMapper<AlarmDefinition>(AlarmDefinition.class));
      DimensionQueries.bindDimensionsToQuery(q, dimensions);

      List<AlarmDefinition> alarms = (List<AlarmDefinition>) q.list();

      for (AlarmDefinition alarm : alarms)
        hydrateRelationships(h, alarm);
      return alarms;
    }
  }

  @Override
  public AlarmDefinition findById(String tenantId, String alarmDefId) {
    try (Handle h = db.open()) {
      AlarmDefinition alarm =
          h.createQuery(
              "select * from alarm_definition where tenant_id = :tenantId and id = :id and deleted_at is NULL")
              .bind("tenantId", tenantId).bind("id", alarmDefId)
              .map(new BeanMapper<AlarmDefinition>(AlarmDefinition.class)).first();

      if (alarm == null)
        throw new EntityNotFoundException("No alarm definition exists for %s", alarmDefId);

      hydrateRelationships(h, alarm);
      return alarm;
    }
  }

  @Override
  public Map<String, MetricDefinition> findSubAlarmMetricDefinitions(String alarmDefId) {
    try (Handle h = db.open()) {
      List<Map<String, Object>> rows =
          h.createQuery(SUB_ALARM_SQL).bind("alarmDefId", alarmDefId).list();
      Map<String, MetricDefinition> subAlarmMetricDefs = new HashMap<>();
      for (Map<String, Object> row : rows) {
        String id = (String) row.get("id");
        String metricName = (String) row.get("metric_name");
        Map<String, String> dimensions =
            DimensionQueries.dimensionsFor((String) row.get("dimensions"));
        subAlarmMetricDefs.put(id, new MetricDefinition(metricName, dimensions));
      }

      return subAlarmMetricDefs;
    }
  }

  @Override
  public Map<String, AlarmSubExpression> findSubExpressions(String alarmDefId) {
    try (Handle h = db.open()) {
      List<Map<String, Object>> rows =
          h.createQuery(SUB_ALARM_SQL).bind("alarmDefId", alarmDefId).list();
      Map<String, AlarmSubExpression> subExpressions = new HashMap<>();
      for (Map<String, Object> row : rows) {
        String id = (String) row.get("id");
        AggregateFunction function = AggregateFunction.fromJson((String) row.get("function"));
        String metricName = (String) row.get("metric_name");
        AlarmOperator operator = AlarmOperator.fromJson((String) row.get("operator"));
        Double threshold = (Double) row.get("threshold");
        Integer period = (Integer) row.get("period");
        Integer periods = (Integer) row.get("periods");
        Map<String, String> dimensions =
            DimensionQueries.dimensionsFor((String) row.get("dimensions"));
        subExpressions.put(id, new AlarmSubExpression(function, new MetricDefinition(metricName,
            dimensions), operator, threshold, period, periods));
      }

      return subExpressions;
    }
  }

  @Override
  public void update(String tenantId, String id, boolean patch, String name, String description,
      String expression, List<String> matchBy, String severity, boolean actionsEnabled,
      Collection<String> oldSubAlarmIds, Map<String, AlarmSubExpression> changedSubAlarms,
      Map<String, AlarmSubExpression> newSubAlarms, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    Handle h = db.open();

    try {
      h.begin();
      h.insert(
          "update alarm_definition set name = ?, description = ?, expression = ?, match_by = ?, severity = ?, actions_enabled = ?, updated_at = NOW() where tenant_id = ? and id = ?",
          name, description, expression, matchBy == null || Iterables.isEmpty(matchBy) ? null
              : COMMA_JOINER.join(matchBy), severity, actionsEnabled, tenantId, id);

      // Delete old sub-alarms
      if (oldSubAlarmIds != null)
        for (String oldSubAlarmId : oldSubAlarmIds)
          h.execute("delete from sub_alarm_definition where id = ?", oldSubAlarmId);

      // Update changed sub-alarms
      if (changedSubAlarms != null)
        for (Map.Entry<String, AlarmSubExpression> entry : changedSubAlarms.entrySet()) {
          AlarmSubExpression sa = entry.getValue();
          h.execute(
              "update sub_alarm_definition set operator = ?, threshold = ?, updated_at = NOW() where id = ?",
              sa.getOperator().name(), sa.getThreshold(), entry.getKey());
        }

      // Insert new sub-alarms
      createSubExpressions(h, id, newSubAlarms);

      // Delete old actions
      if (patch) {
        deleteActions(h, id, AlarmState.ALARM, alarmActions);
        deleteActions(h, id, AlarmState.OK, okActions);
        deleteActions(h, id, AlarmState.UNDETERMINED, undeterminedActions);
      } else
        h.execute("delete from alarm_action where alarm_definition_id = ?", id);

      // Insert new actions
      persistActions(h, id, AlarmState.ALARM, alarmActions);
      persistActions(h, id, AlarmState.OK, okActions);
      persistActions(h, id, AlarmState.UNDETERMINED, undeterminedActions);

      h.commit();
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }

  private void deleteActions(Handle handle, String id, AlarmState alarmState, List<String> actions) {
    if (actions != null)
      handle.execute("delete from alarm_action where alarm_definition_id = ? and alarm_state = ?", id,
          alarmState.name());
  }

  private List<String> findActionsById(Handle handle, String alarmDefId, AlarmState state) {
    return handle
        .createQuery(
            "select action_id from alarm_action where alarm_definition_id = :alarmDefId and alarm_state = :alarmState")
        .bind("alarmDefId", alarmDefId).bind("alarmState", state.name()).map(StringMapper.FIRST)
        .list();
  }

  private void persistActions(Handle handle, String id, AlarmState alarmState, List<String> actions) {
    if (actions != null)
      for (String action : actions)
        handle.insert("insert into alarm_action values (?, ?, ?)", id, alarmState.name(), action);
  }

  private void hydrateRelationships(Handle handle, AlarmDefinition alarm) {
    alarm.setAlarmActions(findActionsById(handle, alarm.getId(), AlarmState.ALARM));
    alarm.setOkActions(findActionsById(handle, alarm.getId(), AlarmState.OK));
    alarm.setUndeterminedActions(findActionsById(handle, alarm.getId(), AlarmState.UNDETERMINED));
  }

  private void createSubExpressions(Handle handle, String id,
      Map<String, AlarmSubExpression> alarmSubExpressions) {
    if (alarmSubExpressions != null) {
      for (Map.Entry<String, AlarmSubExpression> subEntry : alarmSubExpressions.entrySet()) {
        String subAlarmId = subEntry.getKey();
        AlarmSubExpression subExpr = subEntry.getValue();
        MetricDefinition metricDef = subExpr.getMetricDefinition();

        // Persist sub-alarm
        handle
            .insert(
                "insert into sub_alarm_definition (id, alarm_definition_id, function, metric_name, operator, threshold, period, periods, created_at, updated_at) "
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())", subAlarmId, id, subExpr
                    .getFunction().name(), metricDef.name, subExpr.getOperator().name(), subExpr
                    .getThreshold(), subExpr.getPeriod(), subExpr.getPeriods());

        // Persist sub-alarm dimensions
        if (metricDef.dimensions != null && !metricDef.dimensions.isEmpty())
          for (Map.Entry<String, String> dimEntry : metricDef.dimensions.entrySet())
            handle.insert("insert into sub_alarm_definition_dimension values (?, ?, ?)", subAlarmId,
                dimEntry.getKey(), dimEntry.getValue());
      }
    }
  }
}
