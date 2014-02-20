package com.hpcloud.mon.infrastructure.persistence;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.hpcloud.mon.domain.model.metric.Datapoint;
import com.hpcloud.mon.domain.model.metric.DatapointRepository;

/**
 * Vertica repository implementation.
 * 
 * @author Jonathan Halterman
 */
public class DatapointRepositoryImpl implements DatapointRepository {
  @Override
  public List<Datapoint> find(String authToken, String namespace, DateTime startTime,
      DateTime endTime, Map<String, String> dimensions, List<String> statistics, int period) {
    return null;
  }
}
