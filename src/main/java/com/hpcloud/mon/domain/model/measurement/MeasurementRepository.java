package com.hpcloud.mon.domain.model.measurement;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

/**
 * Repository for measurements.
 * 
 * @author Jonathan Halterman
 */
public interface MeasurementRepository {
  /**
   * Finds measurements for the given criteria.
   */
  List<Measurement> find(String name, Map<String, String> dimensions, DateTime startTime,
      @Nullable DateTime endTime);

  /**
   * Finds aggregated measurements for the given criteria.
   */
  List<Measurement> findAggregated(String name, Map<String, String> dimensions, DateTime startTime,
      @Nullable DateTime endTime, List<String> statistics, int period);
}
