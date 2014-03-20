package com.hpcloud.mon.app.validation;

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.hpcloud.mon.common.model.Services;
import com.hpcloud.mon.resource.exception.Exceptions;
import com.sun.jersey.spi.container.WebApplication;

/**
 * Utilities for validating metric names.
 * 
 * @author Todd Walk
 */
public class MetricNameValidation {
  private static final Pattern VALID_METRIC_NAME = Pattern.compile("^[a-zA-Z0-9_\\.\\-]+$");

  private MetricNameValidation() {
  }

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
  public static void validate(String metricName, @Nullable String service) {
    // General validations
    if (Strings.isNullOrEmpty(metricName))
      throw Exceptions.unprocessableEntity("Metric name is required");
    if (metricName.length() > 64)
      throw Exceptions.unprocessableEntity("Metric name %s must be 64 characters or less",
          metricName);
    if (!Services.isReserved(metricName) && !VALID_METRIC_NAME.matcher(metricName).matches())
      throw Exceptions.unprocessableEntity("Metric name %s may only contain: a-z A-Z 0-9 _ - .",
          metricName);

    // Service specific validations
    if (service != null && Services.isReserved(service)) {
      if (!Strings.isNullOrEmpty(metricName) && !Services.isValidMetricName(service, metricName)) {
        throw Exceptions.unprocessableEntity("%s is not a valid metric name for namespace %s",
            metricName, service);
      }
    }
  }
}
