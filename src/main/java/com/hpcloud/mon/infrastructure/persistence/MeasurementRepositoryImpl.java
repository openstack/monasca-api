package com.hpcloud.mon.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

import com.hpcloud.mon.domain.model.measurement.Measurement;
import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;

/**
 * Vertica measurement repository implementation.
 */
public class MeasurementRepositoryImpl implements MeasurementRepository {
  @Override
  public List<Measurement> find(String name, Map<String, String> dimensions, DateTime startTime,
      @Nullable DateTime endTime) {
    return null;
  }
}
