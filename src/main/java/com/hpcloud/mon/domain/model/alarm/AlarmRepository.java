package com.hpcloud.mon.domain.model.alarm;

import java.util.List;
import java.util.Map;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;

public interface AlarmRepository {
  /**
   * Deletes all alarms associated with the {@code id}.
   */
  void deleteById(String id);

  /**
   * Returns alarms for the given criteria.
   */
  List<Alarm> find(String tenantId, String alarmDefId, String metricName,
      Map<String, String> metricDimensions, AlarmState state);

  /**
   * @throws EntityNotFoundException if an alarm cannot be found for the {@code id}
   */
  Alarm findById(String id);

  List<MetricDefinition> findMetrics(String alarmId);

  /**
   * Updates and returns an alarm for the criteria.
   */
  void update(String tenantId, String id, AlarmState state);
}
