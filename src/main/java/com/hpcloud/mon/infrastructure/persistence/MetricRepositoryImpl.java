package com.hpcloud.mon.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import com.hpcloud.mon.common.model.metric.Metric;
import com.hpcloud.mon.domain.model.metric.MetricRepository;

/**
 * Vertica metric repository implementation.
 */
public class MetricRepositoryImpl implements MetricRepository {
  @Override
  public List<Metric> find(String tenantId, String name, Map<String, String> dimensions) {
    return null;
  }
}
