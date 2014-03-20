package com.hpcloud.mon.domain.model.metric;

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
  List<Measurement> find(String authToken, String name, DateTime startTime,
      @Nullable DateTime endTime, Map<String, String> dimensions, List<String> statistics,
      int period);
}
