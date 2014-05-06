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
package com.hpcloud.mon.domain.model.alarm;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;

/**
 * Repository for alarms.
 */
public interface AlarmRepository {
  /**
   * Creates and returns a new alarm for the criteria.
   */
  Alarm create(String tenantId, String id, String name, String description, String expression,
    Map<String, AlarmSubExpression> subExpressions, List<String> alarmActions,
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
  List<Alarm> find(String tenantId, Map<String, String> dimensions, String state);

  /**
   * @throws EntityNotFoundException if an alarm cannot be found for the {@code alarmId}
   */
  Alarm findById(String tenantId, String alarmId);

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
  void update(String tenantId, String id, boolean patch, String name, String description,
    String expression, AlarmState state, boolean enabled, Collection<String> oldSubAlarmIds,
    Map<String, AlarmSubExpression> changedSubAlarms,
    Map<String, AlarmSubExpression> newSubAlarms, List<String> alarmActions,
    List<String> okActions, List<String> undeterminedActions);
}
