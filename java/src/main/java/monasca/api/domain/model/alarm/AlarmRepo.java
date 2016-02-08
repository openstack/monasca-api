/*
 * Copyright (c) 2015-2016 Hewlett Packard Enterprise Development Company, L.P.
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
package monasca.api.domain.model.alarm;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.api.domain.exception.EntityNotFoundException;

public interface AlarmRepo {
  /**
   * Deletes all alarms associated with the {@code id}.
   */
  void deleteById(String tenantId, String id);

  /**
   * Returns alarms for the given criteria.
   */
  List<Alarm> find(String tenantId, String alarmDefId, String metricName, Map<String,
      String> metricDimensions, AlarmState state, List<AlarmSeverity> severities, String lifecycleState, String link, DateTime stateUpdatedStart,
                   List<String> sort_by, String offset, int limit, boolean enforceLimit);

  /**
   * @throws EntityNotFoundException if an alarm cannot be found for the {@code id}
   */
  Alarm findById(String tenantId, String id);

  /**
   * Updates the state and returns the original alarm for the {@code id}.
   * @return the original alarm before any state change
   */
  Alarm update(String tenantId, String id, AlarmState state, String lifecycleState, String link);

  /**
   * Gets the AlarmSubExpressions mapped by their Ids for an Alarm Id
   */
  Map<String, AlarmSubExpression> findAlarmSubExpressions(String alarmId);

  /**
   * Gets the AlarmSubExpressions mapped by their Ids then mapped by alarm id for an
   * Alarm Definition Id
   */
  Map<String, Map<String, AlarmSubExpression>> findAlarmSubExpressionsForAlarmDefinition(String alarmDefinitionId);

  /**
   * Gets the count(s) of the alarms matching the parameters
   * @return 2 dimensional list of the counts with their group tags
   */
  AlarmCount getAlarmsCount(String tenantId, String alarmDefId, String metricName,
                            Map<String, String> metricDimensions, AlarmState state,
                            List<AlarmSeverity> severities, String lifecycleState, String link,
                            DateTime stateUpdatedStart, List<String> groupBy,
                            String offset, int limit);
}
