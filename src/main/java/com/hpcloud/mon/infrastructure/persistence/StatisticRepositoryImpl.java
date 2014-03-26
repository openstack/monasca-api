package com.hpcloud.mon.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.hpcloud.mon.domain.model.statistic.StatisticRepository;
import com.hpcloud.util.stats.Statistic;

/**
 * Vertica statistic repository implementation.
 */
public class StatisticRepositoryImpl implements StatisticRepository {
  @Override
  public List<Statistic> find(String tenantId, String name, Map<String, String> dimensions,
      DateTime startTime, DateTime endTime, List<String> statistics, int period) {
    return null;
  }
}
