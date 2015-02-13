package monasca.api.domain.model.alarm;

import java.util.List;
import java.util.Map;

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
  List<Alarm> find(String tenantId, String alarmDefId, String metricName,
      Map<String, String> metricDimensions, AlarmState state, String offset);

  /**
   * @throws EntityNotFoundException if an alarm cannot be found for the {@code id}
   */
  Alarm findById(String tenantId, String id);

  /**
   * Updates the state and returns the original alarm for the {@code id}.
   * @return the original alarm before any state change
   */
  Alarm update(String tenantId, String id, AlarmState state);

  /**
   * Gets the AlarmSubExpressions mapped by their Ids for an Alarm Id
   */
  Map<String, AlarmSubExpression> findAlarmSubExpressions(String alarmId);

  /**
   * Gets the AlarmSubExpressions mapped by their Ids then mapped by alarm id for an
   * Alarm Definition Id
   */
  Map<String, Map<String, AlarmSubExpression>> findAlarmSubExpressionsForAlarmDefinition(String alarmDefinitionId);
}
