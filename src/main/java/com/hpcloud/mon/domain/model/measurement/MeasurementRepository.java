package com.hpcloud.mon.domain.model.measurement;

import java.util.Collection;
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
  Collection<Measurements> find(String tenantId, String name, Map<String, String> dimensions,
      DateTime startTime, @Nullable DateTime endTime);
}
