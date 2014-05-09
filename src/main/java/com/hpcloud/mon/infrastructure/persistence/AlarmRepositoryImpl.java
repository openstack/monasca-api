/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpcloud.mon.infrastructure.persistence;

import com.hpcloud.mon.common.model.alarm.AggregateFunction;
import com.hpcloud.mon.common.model.alarm.AlarmOperator;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.persistence.BeanMapper;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.util.StringMapper;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.*;

/**
 * Alarm repository implementation.
 */
public class AlarmRepositoryImpl implements AlarmRepository {
<<<<<<< HEAD
  private static final String SUB_ALARM_SQL = "select sa.*, sad.dimensions from sub_alarm as sa, "
      + "(select sub_alarm_id, group_concat(dimension_name, '=', value) as dimensions from sub_alarm_dimension group by sub_alarm_id) as sad "
      + "where sa.id = sad.sub_alarm_id and sa.alarm_id = :alarmId";
=======
  private static final String SUB_ALARM_SQL = "select sa.*, sad.dimensions from sub_alarm as sa "
    + "left join (select sub_alarm_id, group_concat(dimension_name, '=', value) as dimensions from sub_alarm_dimension group by sub_alarm_id ) as sad "
    + "on sad.sub_alarm_id = sa.id where sa.alarm_id = :alarmId";
>>>>>>> 9a37a4276b92dde20c1a568b3a73b621aa2c6dac

  private final DBI db;

  @Inject
  public AlarmRepositoryImpl(@Named("mysql") DBI db) {
    this.db = db;
  }

  @Override
  public Alarm create(String tenantId, String id, String name, String description, String severity,
    String expression, Map<String, AlarmSubExpression> subExpressions, List<String> alarmActions,
    List<String> okActions, List<String> undeterminedActions) {
    Handle h = db.open();

    try {
      h.begin();
      h.insert(
        "insert into alarm (id, tenant_id, name, description, severity, expression, state, actions_enabled, created_at, updated_at, deleted_at) values (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NULL)",
        id, tenantId, name, description, severity, expression, AlarmState.UNDETERMINED.toString(), true);

      // Persist sub-alarms
      createSubExpressions(h, id, subExpressions);

      // Persist actions
      persistActions(h, id, AlarmState.ALARM, alarmActions);
      persistActions(h, id, AlarmState.OK, okActions);
      persistActions(h, id, AlarmState.UNDETERMINED, undeterminedActions);

      h.commit();
      return new Alarm(id, name, description, severity, expression, AlarmState.UNDETERMINED, true,
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
  public void deleteById(String tenantId, String alarmId) {
    try (Handle h = db.open()) {
      if (h.update(
          "update alarm set deleted_at = NOW() where tenant_id = ? and id = ? and deleted_at is NULL",
          tenantId, alarmId) == 0)
        throw new EntityNotFoundException("No alarm exists for %s", alarmId);
    }
  }

  @Override
  public boolean exists(String tenantId, String name) {
    try (Handle h = db.open()) {
      return h.createQuery(
          "select exists(select 1 from alarm where tenant_id = :tenantId and name = :name and deleted_at is NULL)")
          .bind("tenantId", tenantId)
          .bind("name", name)
          .mapTo(Boolean.TYPE)
          .first();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Alarm> find(String tenantId, Map<String, String> dimensions, String state) {
    try (Handle h = db.open()) {

      String query = "select distinct alarm.id, alarm.description,alarm.tenant_id, alarm.severity, alarm.expression,alarm.state,alarm.name,alarm.actions_enabled,alarm.created_at, alarm.updated_at, alarm.deleted_at from alarm join sub_alarm sub on alarm.id=sub.alarm_id left outer join sub_alarm_dimension dim on sub.id=dim.sub_alarm_id%s where tenant_id = :tenantId and deleted_at is NULL %s";
      StringBuilder sbWhere = new StringBuilder();

      if (state != null) {
        sbWhere.append(" and alarm.state = :state");
      }

      String sql = String.format(query, SubAlarmQueries.buildJoinClauseFor(dimensions), sbWhere);
      Query<?> q = h.createQuery(sql).bind("tenantId", tenantId);

      if (state != null) {
        q.bind("state", state);
      }

      q = q.map(new BeanMapper<Alarm>(Alarm.class));
      DimensionQueries.bindDimensionsToQuery(q, dimensions);

      List<Alarm> alarms = (List<Alarm>) q.list();

      for (Alarm alarm : alarms)
        hydrateRelationships(h, alarm);
      return alarms;
    }
  }

  @Override
  public Alarm findById(String tenantId, String alarmId) {
    try (Handle h = db.open()) {
      Alarm alarm = h.createQuery(
          "select * from alarm where tenant_id = :tenantId and id = :id and deleted_at is NULL")
          .bind("tenantId", tenantId)
          .bind("id", alarmId)
          .map(new BeanMapper<Alarm>(Alarm.class))
          .first();

      if (alarm == null)
        throw new EntityNotFoundException("No alarm exists for %s", alarmId);

      hydrateRelationships(h, alarm);
      return alarm;
    }
  }

  @Override
  public Map<String, MetricDefinition> findSubAlarmMetricDefinitions(String alarmId) {
    try (Handle h = db.open()) {
      List<Map<String, Object>> rows = h.createQuery(SUB_ALARM_SQL).bind("alarmId", alarmId).list();
      Map<String, MetricDefinition> subAlarmMetricDefs = new HashMap<>();
      for (Map<String, Object> row : rows) {
        String id = (String) row.get("id");
        String metricName = (String) row.get("metric_name");
        Map<String, String> dimensions = dimensionsFor((String) row.get("dimensions"));
        subAlarmMetricDefs.put(id, new MetricDefinition(metricName, dimensions));
      }

      return subAlarmMetricDefs;
    }
  }

  @Override
  public Map<String, AlarmSubExpression> findSubExpressions(String alarmId) {
    try (Handle h = db.open()) {
      List<Map<String, Object>> rows = h.createQuery(SUB_ALARM_SQL).bind("alarmId", alarmId).list();
      Map<String, AlarmSubExpression> subExpressions = new HashMap<>();
      for (Map<String, Object> row : rows) {
        String id = (String) row.get("id");
        AggregateFunction function = AggregateFunction.fromJson((String) row.get("function"));
        String metricName = (String) row.get("metric_name");
        AlarmOperator operator = AlarmOperator.fromJson((String) row.get("operator"));
        Double threshold = (Double) row.get("threshold");
        Integer period = (Integer) row.get("period");
        Integer periods = (Integer) row.get("periods");
        Map<String, String> dimensions = dimensionsFor((String) row.get("dimensions"));
        subExpressions.put(id, new AlarmSubExpression(function, new MetricDefinition(metricName,
            dimensions), operator, threshold, period, periods));
      }

      return subExpressions;
    }
  }

  @Override
  public void update(String tenantId, String id, boolean patch, String name, String description,
    String expression, String severity, AlarmState state, boolean actionsEnabled,
    Collection<String> oldSubAlarmIds, Map<String, AlarmSubExpression> changedSubAlarms,
    Map<String, AlarmSubExpression> newSubAlarms, List<String> alarmActions,
    List<String> okActions, List<String> undeterminedActions) {
    Handle h = db.open();

    try {
      h.begin();
      h.insert(
        "update alarm set name = ?, description = ?, expression = ?, severity = ?, state = ?, actions_enabled = ?, updated_at = NOW() where tenant_id = ? and id = ?",
        name, description, expression, severity, state.name(), actionsEnabled, tenantId, id);

      // Delete old sub-alarms
      if (oldSubAlarmIds != null)
        for (String oldSubAlarmId : oldSubAlarmIds)
          h.execute("delete from sub_alarm where id = ?", oldSubAlarmId);

      // Update changed sub-alarms
      if (changedSubAlarms != null)
        for (Map.Entry<String, AlarmSubExpression> entry : changedSubAlarms.entrySet()) {
          AlarmSubExpression sa = entry.getValue();
          h.execute(
              "update sub_alarm set operator = ?, threshold = ?, updated_at = NOW() where id = ?",
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
        h.execute("delete from alarm_action where alarm_id = ?", id);

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
      handle.execute("delete from alarm_action where alarm_id = ? and alarm_state = ?", id,
          alarmState.name());
  }

  private Map<String, String> dimensionsFor(String dimensionSet) {
    Map<String, String> dimensions = null;

    if (dimensionSet != null) {
      dimensions = new HashMap<String, String>();
      for (String kvStr : dimensionSet.split(",")) {
        String[] kv = kvStr.split("=");
        if (kv.length > 1)
          dimensions.put(kv[0], kv[1]);
      }
    }

    return dimensions;
  }

  private List<String> findActionsById(Handle handle, String alarmId, AlarmState state) {
    return handle.createQuery(
        "select action_id from alarm_action where alarm_id = :alarmId and alarm_state = :alarmState")
        .bind("alarmId", alarmId)
        .bind("alarmState", state.name())
        .map(StringMapper.FIRST)
        .list();
  }

  private void persistActions(Handle handle, String id, AlarmState alarmState, List<String> actions) {
    if (actions != null)
      for (String action : actions)
        handle.insert("insert into alarm_action values (?, ?, ?)", id, alarmState.name(), action);
  }

  private void hydrateRelationships(Handle handle, Alarm alarm) {
    alarm.setAlarmActions(findActionsById(handle, alarm.getId(), AlarmState.ALARM));
    alarm.setOkActions(findActionsById(handle, alarm.getId(), AlarmState.OK));
    alarm.setUndeterminedActions(findActionsById(handle, alarm.getId(), AlarmState.UNDETERMINED));
  }

  private void createSubExpressions(Handle handle, String id,
      Map<String, AlarmSubExpression> alarmSubExpressions) {
    if (alarmSubExpressions != null)
      for (Map.Entry<String, AlarmSubExpression> subEntry : alarmSubExpressions.entrySet()) {
        String subAlarmId = subEntry.getKey();
        AlarmSubExpression subExpr = subEntry.getValue();
        MetricDefinition metricDef = subExpr.getMetricDefinition();

        // Persist sub-alarm
        handle.insert(
            "insert into sub_alarm (id, alarm_id, function, metric_name, operator, threshold, period, periods, state, created_at, updated_at) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())", subAlarmId, id,
            subExpr.getFunction().name(), metricDef.name, subExpr.getOperator().name(),
            subExpr.getThreshold(), subExpr.getPeriod(), subExpr.getPeriods(),
            AlarmState.UNDETERMINED.toString());

        // Persist sub-alarm dimensions
        if (metricDef.dimensions != null && !metricDef.dimensions.isEmpty())
          for (Map.Entry<String, String> dimEntry : metricDef.dimensions.entrySet())
            handle.insert("insert into sub_alarm_dimension values (?, ?, ?)", subAlarmId,
                dimEntry.getKey(), dimEntry.getValue());
      }
  }
}