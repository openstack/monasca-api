package com.hpcloud.mon.domain.model.alarm;

import java.util.List;
import java.util.Map;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;

/**
 * Repository for alarms.
 * 
 * @author Jonathan Halterman
 */
public interface AlarmRepository {
  /**
   * Creates and returns a new alarm for the criteria.
   */
  AlarmDetail create(String id, String tenantId, String name, String description,
      String expression, Map<String, AlarmSubExpression> subExpressions, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions);

  /**
   * @throws EntityNotFoundException if an alarm cannot be found for the {@code alarmId}
   */
  void deleteById(String tenantId, String alarmId);

  /**
   * Returns true if an alarm exists for the given criteria, else false.
   */
  boolean exists(String tenantId, String name);

  /**
   * Returns alarms for the {@code tenantId}.
   */
  List<Alarm> find(String tenantId);

  /**
   * @throws EntityNotFoundException if an alarm cannot be found for the {@code alarmId}
   */
  AlarmDetail findById(String tenantId, String alarmId);

  /**
   * Returns the sub-alarm Ids for the {@code alarmId}.
   */
  Map<String, MetricDefinition> findSubAlarmMetricDefinitions(String alarmId);

  /**
   * Returns the sub expressions for the {@code alarmId}.
   */
  Map<String, AlarmSubExpression> findSubExpressions(String alarmId);

  /**
   * Updates and returns an alarm for the criteria.
   */
  AlarmDetail update(String id, String tenantId, String name, String description,
      String expression, AlarmState state, boolean enabled,
      Map<String, AlarmSubExpression> oldSubAlarms, Map<String, AlarmSubExpression> newSubAlarms,
      List<String> alarmActions, List<String> okActions, List<String> undeterminedActions);
}
