package com.hpcloud.mon.infrastructure.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.util.StringMapper;

import com.hpcloud.mon.common.model.alarm.AggregateFunction;
import com.hpcloud.mon.common.model.alarm.AlarmOperator;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.alarm.AlarmDetail;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.persistence.BeanMapper;

/**
 * Alarm repository implementation.
 * 
 * @author Jonathan Halterman
 */
public class AlarmRepositoryImpl implements AlarmRepository {
  private static final String SUB_ALARM_SQL = "select sa.*, sad.dimensions from sub_alarm as sa, "
      + "(select sub_alarm_id, group_concat(dimension_name, '=', value) as dimensions from sub_alarm_dimension group by sub_alarm_id) as sad "
      + "where sa.id = sad.sub_alarm_id and sa.alarm_id = :alarmId";

  private final DBI db;

  @Inject
  public AlarmRepositoryImpl(DBI db) {
    this.db = db;
  }

  @Override
  public AlarmDetail create(String id, String tenantId, String name, String description,
      String expression, Map<String, AlarmSubExpression> subExpressions, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    Handle h = db.open();

    try {
      h.begin();
      h.insert(
          "insert into alarm (id, tenant_id, name, description, expression, state, created_at, updated_at, deleted_at) values (?, ?, ?, ?, ?, ?, NOW(), NOW(), NULL)",
          id, tenantId, name, description, expression, AlarmState.UNDETERMINED.toString());

      // Persist sub-alarms
      persistSubExpressions(h, id, subExpressions);

      // Persist actions
      persistActions(h, id, AlarmState.ALARM, alarmActions);
      persistActions(h, id, AlarmState.OK, okActions);
      persistActions(h, id, AlarmState.UNDETERMINED, undeterminedActions);

      h.commit();
      return new AlarmDetail(id, name, description, expression, AlarmState.UNDETERMINED,
          alarmActions, okActions, undeterminedActions);
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }

  @Override
  public AlarmDetail update(String id, String tenantId, String name, String description,
      String expression, AlarmState state, boolean enabled,
      Map<String, AlarmSubExpression> oldSubAlarms, Map<String, AlarmSubExpression> newSubAlarms,
      List<String> alarmActions, List<String> okActions, List<String> undeterminedActions) {
    Handle h = db.open();

    try {
      h.begin();
      h.insert(
          "update alarm set name = ?, description = ?, expression = ?, state = ?, enabled = ?, updated_at = NOW() where tenant_id = ? and id = ?",
          name, description, expression, state.name(), enabled, tenantId, id);

      // Persist sub-alarms
      persistSubExpressions(h, id, newSubAlarms);

      // Update actions
      h.execute("delete from alarm_action where alarm_id = ?", id);
      persistActions(h, id, AlarmState.ALARM, alarmActions);
      persistActions(h, id, AlarmState.OK, okActions);
      persistActions(h, id, AlarmState.UNDETERMINED, undeterminedActions);

      h.commit();
      return new AlarmDetail(id, name, description, expression, AlarmState.UNDETERMINED,
          alarmActions, okActions, undeterminedActions);
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }

  @Override
  public void deleteById(String tenantId, String alarmId) {
    Handle h = db.open();

    try {
      if (h.update(
          "update alarm set deleted_at = NOW() where tenant_id = ? and id = ? and deleted_at is NULL",
          tenantId, alarmId) == 0)
        throw new EntityNotFoundException("No alarm exists for %s", alarmId);
    } finally {
      h.close();
    }
  }

  @Override
  public boolean exists(String tenantId, String name) {
    Handle h = db.open();

    try {
      return h.createQuery(
          "select exists(select 1 from alarm where tenant_id = :tenantId and name = :name and deleted_at is NULL)")
          .bind("tenantId", tenantId)
          .bind("name", name)
          .mapTo(Boolean.TYPE)
          .first();
    } finally {
      h.close();
    }
  }

  @Override
  public List<Alarm> find(String tenantId) {
    Handle h = db.open();

    try {
      return h.createQuery("select * from alarm where tenant_id = :tenantId and deleted_at is NULL")
          .bind("tenantId", tenantId)
          .map(new BeanMapper<Alarm>(Alarm.class))
          .list();
    } finally {
      h.close();
    }
  }

  @Override
  public AlarmDetail findById(String tenantId, String alarmId) {
    Handle h = db.open();

    try {
      AlarmDetail alarm = h.createQuery(
          "select * from alarm where tenant_id = :tenantId and id = :id and deleted_at is NULL")
          .bind("tenantId", tenantId)
          .bind("id", alarmId)
          .map(new BeanMapper<AlarmDetail>(AlarmDetail.class))
          .first();

      if (alarm == null)
        throw new EntityNotFoundException("No alarm exists for %s", alarmId);

      // Hydrate all relationships
      alarm.setAlarmActions(findActionsById(h, alarmId, AlarmState.ALARM));
      alarm.setOkActions(findActionsById(h, alarmId, AlarmState.OK));
      alarm.setUndeterminedActions(findActionsById(h, alarmId, AlarmState.UNDETERMINED));
      return alarm;
    } finally {
      h.close();
    }
  }

  @Override
  public Map<String, MetricDefinition> findSubAlarmMetricDefinitions(String alarmId) {
    Handle h = db.open();

    try {
      List<Map<String, Object>> rows = h.createQuery(SUB_ALARM_SQL).bind("alarmId", alarmId).list();
      Map<String, MetricDefinition> subAlarmMetricDefs = new HashMap<>();
      for (Map<String, Object> row : rows) {
        String id = (String) row.get("id");
        String metricName = (String) row.get("metric_name");
        Map<String, String> dimensions = dimensionsFor((String) row.get("dimensions"));
        subAlarmMetricDefs.put(id, new MetricDefinition(metricName, dimensions));
      }

      return subAlarmMetricDefs;
    } finally {
      h.close();
    }
  }

  @Override
  public Map<String, AlarmSubExpression> findSubExpressions(String alarmId) {
    Handle h = db.open();

    try {
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
    } finally {
      h.close();
    }
  }

  private void persistSubExpressions(Handle handle, String id,
      Map<String, AlarmSubExpression> alarmSubExpressions) {
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

  private void persistActions(Handle handle, String id, AlarmState alarmState, List<String> actions) {
    if (actions != null)
      for (String action : actions)
        handle.insert("insert into alarm_action values (?, ?, ?)", id, alarmState.name(), action);
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
}