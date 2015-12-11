/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package monasca.api.domain.model.metric;

import monasca.common.model.metric.MetricDefinition;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * Repository for metrics.
 */
public interface MetricDefinitionRepo {

  /**
   * Finds metrics for the given criteria.
   */
  List<MetricDefinition> find(String tenantId, String name, Map<String, String> dimensions,
                              DateTime startTime, DateTime endTime, String offset, int limit)
      throws Exception;

  List<MetricName> findNames(String tenantId, Map<String, String> dimensions, String offset, int limit) throws Exception;
}
