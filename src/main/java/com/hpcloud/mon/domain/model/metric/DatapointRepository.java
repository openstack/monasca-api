package com.hpcloud.mon.domain.model.metric;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

/**
 * Repository for metrics.
 * 
 * @author Jonathan Halterman
 */
public interface DatapointRepository {
  /**
   * Finds metrics for the given criteria.
   */
  List<Datapoint> find(String authToken, String namespace, DateTime startTime,
      @Nullable DateTime endTime, Map<String, String> dimensions, List<String> statistics,
      int period);
}
