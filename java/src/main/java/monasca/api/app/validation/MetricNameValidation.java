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
package monasca.api.app.validation;

import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import monasca.api.app.command.CreateMetricCommand;
import monasca.api.resource.exception.Exceptions;
import com.sun.jersey.spi.container.WebApplication;

/**
 * Utilities for validating metric names.
 */
public class MetricNameValidation {
  private static final Pattern VALID_METRIC_NAME = Pattern.compile("[^><={}(), \"\\\\;&]+$");

  private MetricNameValidation() {}

  /**
   * Normalizes the {@code metricName} by removing whitespace.
   */
  public static String normalize(String metricName) {
    return metricName == null ? null : CharMatcher.WHITESPACE.trimFrom(metricName);
  }

  /**
   * Validates the {@code metricName} for the character constraints.
   * 
   * @throws WebApplication if validation fails
   */
  public static void validate(String metricName, boolean nameRequiredFlag) {

    // General validations

    if (Strings.isNullOrEmpty(metricName)) {
      if (nameRequiredFlag) {
        throw Exceptions.unprocessableEntity("Metric name is required");
      } else {
        return;
      }
    }

    if (metricName.length() > CreateMetricCommand.MAX_NAME_LENGTH)
      throw Exceptions.unprocessableEntity("Metric name %s must be %d characters or less",
        metricName, CreateMetricCommand.MAX_NAME_LENGTH);
    if (!VALID_METRIC_NAME.matcher(metricName).matches())
      throw Exceptions.unprocessableEntity("Metric name %s may not contain: > < = { } ( ) ' \" \\ , ; &",
        metricName);
  }
}
