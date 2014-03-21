package com.hpcloud.mon.domain.model.metric;

import java.util.List;
import java.util.Map;

import com.hpcloud.mon.common.model.metric.Metric;

/**
 * Repository for metrics.
 * 
 * @author Jonathan Halterman
 */
public interface MetricRepository {
  /**
   * Finds metrics for the given criteria.
   */
  List<Metric> find(String name, Map<String, String> dimensions);
}
