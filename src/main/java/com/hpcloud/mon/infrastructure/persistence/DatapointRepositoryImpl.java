package com.hpcloud.mon.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.hpcloud.mon.domain.model.metric.Measurement;
import com.hpcloud.mon.domain.model.metric.MeasurementRepository;

/**
 * Vertica repository implementation.
 * 
 * @author Jonathan Halterman
 */
public class DatapointRepositoryImpl implements MeasurementRepository {
  @Override
  public List<Measurement> find(String authToken, String namespace, DateTime startTime,
      DateTime endTime, Map<String, String> dimensions, List<String> statistics, int period) {
    return null;
  }
}
