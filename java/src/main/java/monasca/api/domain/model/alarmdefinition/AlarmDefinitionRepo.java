/*
 * Copyright (c) 2014,2016 Hewlett Packard Enterprise Development Company, L.P.
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
package monasca.api.domain.model.alarmdefinition;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import monasca.api.domain.exception.EntityNotFoundException;

/**
 * Repository for alarm definitions.
 */
public interface AlarmDefinitionRepo {
  /**
   * Creates and returns a new alarm definition for the criteria.
   */
  AlarmDefinition create(String tenantId, String id, String name, String description,
      String severity, String expression, Map<String, AlarmSubExpression> subExpressions,
      List<String> matchBy, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions);

  /**
   * @throws EntityNotFoundException if an alarm definition cannot be found for the
   *         {@code alarmDefId}
   */
  void deleteById(String tenantId, String alarmDefId);

  /**
   * Returns true if an alarm exists for the given criteria, else false.
   */
  String exists(String tenantId, String name);

  /**
   * Returns alarms for the given criteria.
   */
  List<AlarmDefinition> find(String tenantId, String name, Map<String, String> dimensions,
                             List<AlarmSeverity> severities, List<String> sortBy, String offset,
                             int limit);

  /**
   * @throws EntityNotFoundException if an alarm cannot be found for the {@code alarmDefId}
   */
  AlarmDefinition findById(String tenantId, String alarmDefId);

  /**
   * Returns the sub-alarm Ids for the {@code alarmDefId}.
   */
  Map<String, MetricDefinition> findSubAlarmMetricDefinitions(String alarmDefId);

  /**
   * Returns the sub expressions for the {@code alarmDefId}.
   */
  Map<String, AlarmSubExpression> findSubExpressions(String alarmDefId);

  /**
   * Updates and returns an alarm definition for the criteria.
   */
  void update(String tenantId, String id, boolean patch, String name, String description,
      String expression, List<String> matchBy, String severity, boolean actionsEnabled,
      Collection<String> oldSubAlarmIds, Map<String, AlarmSubExpression> changedSubAlarms,
      Map<String, AlarmSubExpression> newSubAlarms, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions);
}
