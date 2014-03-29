package com.hpcloud.mon.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;

import com.hpcloud.mon.domain.model.statistic.StatisticRepository;
import com.hpcloud.util.stats.Statistic;

/**
 * Vertica statistic repository implementation.
 */
public class StatisticRepositoryImpl implements StatisticRepository {
  private final DBI db;

  @Inject
  public StatisticRepositoryImpl(@Named("vertica") DBI db) {
    this.db = db;
  }

  @Override
  public List<Statistic> find(String tenantId, String name, Map<String, String> dimensions,
      DateTime startTime, DateTime endTime, List<String> statistics, int period) {
    return null;
  }
}
