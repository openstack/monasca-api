package com.hpcloud.mon.infrastructure.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.util.StringMapper;

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
  private static final String SUB_ALARM_METRIC_DEF_SQL = "select sa.id, sa.metric_name, sad.dimensions from sub_alarm as sa, "
      + "(select sub_alarm_id, group_concat(dimension_name, '=', value) as dimensions from sub_alarm_dimension group by sub_alarm_id) as sad "
      + "where sa.id = sad.sub_alarm_id and sa.alarm_id = :alarmId";

  private final DBI db;

  @Inject
  public AlarmRepositoryImpl(DBI db) {
    this.db = db;
  }

  @Override
  public AlarmDetail create(String id, String tenantId, String name, String expression,
      Map<String, AlarmSubExpression> subExpressions, List<String> alarmActions) {
    Handle h = db.open();

    try {
      h.begin();
      h.insert(
          "insert into alarm (id, tenant_id, name, expression, state, created_at, updated_at, deleted_at) values (?, ?, ?, ?, ?, NOW(), NOW(), NULL)",
          id, tenantId, name, expression, AlarmState.UNDETERMINED.toString());

      // Persist sub-alarms
      for (Map.Entry<String, AlarmSubExpression> subEntry : subExpressions.entrySet()) {
        String subAlarmId = subEntry.getKey();
        AlarmSubExpression subExpr = subEntry.getValue();
        MetricDefinition metricDef = subExpr.getMetricDefinition();

        h.insert(
            "insert into sub_alarm (id, alarm_id, function, metric_name, operator, threshold, period, periods, state, created_at, updated_at) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())", subAlarmId, id,
            subExpr.getFunction().name(), metricDef.name, subExpr.getOperator().name(),
            subExpr.getThreshold(), subExpr.getPeriod(), subExpr.getPeriods(),
            AlarmState.UNDETERMINED.toString());

        // Persist sub-alarm dimensions
        if (metricDef.dimensions != null && !metricDef.dimensions.isEmpty())
          for (Map.Entry<String, String> dimEntry : metricDef.dimensions.entrySet())
            h.insert("insert into sub_alarm_dimension values (?, ?, ?)", subAlarmId,
                dimEntry.getKey(), dimEntry.getValue());
      }

      // Persist actions
      if (alarmActions != null)
        for (String alarmAction : alarmActions)
          h.insert("insert into alarm_action values (?, ?)", id, alarmAction);
      h.commit();
      return new AlarmDetail(id, name, expression, AlarmState.UNDETERMINED, alarmActions);
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
      alarm.setAlarmActions(findAlarmActionsById(h, alarmId));
      return alarm;
    } finally {
      h.close();
    }
  }

  @Override
  public Map<String, MetricDefinition> findSubAlarmMetricDefinitions(String alarmId) {
    Handle h = db.open();

    try {
      List<Map<String, Object>> rows = h.createQuery(SUB_ALARM_METRIC_DEF_SQL)
          .bind("alarmId", alarmId)
          .list();
      Map<String, MetricDefinition> subAlarmMetricDefs = new HashMap<String, MetricDefinition>();
      for (Map<String, Object> row : rows) {
        String id = (String) row.get("id");
        String metricName = (String) row.get("metric_name");
        String dimensionSet = (String) row.get("dimensions");
        Map<String, String> dimensions = null;

        if (dimensionSet != null) {
          dimensions = new HashMap<String, String>();
          for (String kvStr : dimensionSet.split(",")) {
            String[] kv = kvStr.split("=");
            if (kv.length > 1)
              dimensions.put(kv[0], kv[1]);
          }
        }

        subAlarmMetricDefs.put(id, new MetricDefinition(metricName, dimensions));
      }

      return subAlarmMetricDefs;
    } finally {
      h.close();
    }
  }

  private List<String> findAlarmActionsById(Handle handle, String alarmId) {
    return handle.createQuery("select action_id from alarm_action where alarm_id = :alarmId")
        .bind("alarmId", alarmId)
        .map(StringMapper.FIRST)
        .list();
  }
}
