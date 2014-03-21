package com.hpcloud.mon.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.hpcloud.mon.domain.model.measurement.Measurement;
import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;

/**
 * Vertica measurement repository implementation.
 */
public class MeasurementRepositoryImpl implements MeasurementRepository {
  @Override
  public List<Measurement> find(String name, Map<String, String> dimensions, DateTime startTime,
      DateTime endTime) {
    return null;
  }

  @Override
  public List<Measurement> findAggregated(String name, Map<String, String> dimensions,
      DateTime startTime, DateTime endTime, List<String> statistics, int period) {
    return null;
  }
}
