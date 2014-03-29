package com.hpcloud.mon.domain.model.metric;

import java.util.List;
import java.util.Map;

import com.hpcloud.mon.common.model.metric.MetricDefinition;

/**
 * Repository for metrics.
 * 
 * @author Jonathan Halterman
 */
public interface MetricDefinitionRepository {
  /**
   * Finds metrics for the given criteria.
   */
  List<MetricDefinition> find(String tenantId, String name, Map<String, String> dimensions);
}
