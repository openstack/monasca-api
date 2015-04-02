/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P.
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
package monasca.api.domain.exception;

import java.util.HashMap;
import java.util.Map;

public class MultipleMetricsException extends Exception {

  private String metricName;
  private Map<String, String> dimensions;

  public MultipleMetricsException() {
    super();
    init(null, null);
  }

  public MultipleMetricsException(String metricName, Map<String, String> dimensions) {
    super();
    init(metricName, dimensions);
  }

  public MultipleMetricsException(String metricName, Map<String, String> dimensions,
                                  String message) {
    super(message);
    init(metricName, dimensions);
  }

  public MultipleMetricsException(String metricName, Map<String, String> dimensions,
                                  String message, Throwable cause) {
    super(message, cause);
    init(metricName, dimensions);
  }

  public MultipleMetricsException(String metricName, Map<String, String> dimensions,
                                  Throwable cause) {
    super(cause);
    init(metricName, dimensions);
  }

  public MultipleMetricsException(String metricName, Map<String, String> dimensions,
                                  String message, Throwable cause, boolean enableSuppression,
                                  boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    init(metricName, dimensions);

  }

  private void init(String metricName, Map<String, String> dimensions) {
    this.metricName = metricName == null ? "" : metricName;
    this.dimensions = dimensions == null ? new HashMap<String, String>() : dimensions;
  }

  public String getMetricName() {
    return metricName;
  }

  public Map<String, String> getDimensions() {
    return dimensions;
  }
}
