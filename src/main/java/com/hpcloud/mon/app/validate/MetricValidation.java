package com.hpcloud.mon.app.validate;

import javax.ws.rs.WebApplicationException;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.hpcloud.mon.app.command.CreateMetricCommand;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * Utilities for working with metrics.
 * 
 * @author Todd Walk
 */
public final class MetricValidation {
  private static final long TIME_2MIN = 120;
  private static final long TIME_2WEEKS = 1209600;
  private static final double VALUE_MIN = 8.515920e-109;
  private static final double VALUE_MAX = 1.174271e+108;

  private MetricValidation() {
  }

  /**
   * Normalizes supported metrics for the {@code namespace} to lowercase.
   */
  public static String normalize(String metric) {
    return metric == null ? null : CharMatcher.WHITESPACE.trimFrom(metric);
  }

  /**
   * Validates that the given {@code metrics} are valid for the {@code namespace}.
   * 
   * @throws WebApplicationException if validation fails
   */
  public static void validateSimple(String namespace, String metric) {
    NamespaceValidation.validateSimple(namespace);
    if (Strings.isNullOrEmpty(metric))
      throw Exceptions.unprocessableEntity("Metric type is required");
    if (!Namespaces.isValidMetricname(namespace, metric))
      throw Exceptions.unprocessableEntity("%s is not a valid metric type for namespace %s",
          metric, namespace);
    if (metric.length() > 64)
      throw Exceptions.unprocessableEntity("Metric type %s must be 64 characters or less", metric);
  }

  public static void validate(String namespace, String metric) {
    validateSimple(namespace, metric);
    if (!metric.matches("^[a-zA-Z0-9_\\.\\-]+$"))
      throw Exceptions.unprocessableEntity("Metric Type %s may only contain: a-z A-Z 0-9 _ - .",
          metric);
  }

  public static void validate(CreateMetricCommand metric) {
    String type = null;
    if (Namespaces.isReserved(metric.namespace)) {
      type = metric.dimensions.get("metric_name");
      if (!Namespaces.isValidMetricname(metric.namespace, type))
        throw Exceptions.unprocessableEntity("%s is not a valid metric name for namespace %s",
            type, metric.namespace);
    }

    validateTimestamp(metric.timestamp);
    if (metric.timeValues != null && metric.timeValues.length > 0) {
      for (double[] list : metric.timeValues) {
        if (list.length != 2)
          throw Exceptions.unprocessableEntity("All entries in the timeValues list must be timestamp / value pairs");
        validateTimestamp((long) list[0]);
        validateValue(list[1]);
      }
    } else {
      validateValue(metric.value);
    }
  }

  public static void validateValue(double value) {
    if (value < 0 || (value > 0 && (value < VALUE_MIN || value > VALUE_MAX)))
      throw Exceptions.unprocessableEntity("Value %s is out of range or not legal", value);
  }

  public static void validateTimestamp(long timestamp) {
    long time = System.currentTimeMillis() / 1000;
    if (timestamp > time + TIME_2MIN || timestamp < time - TIME_2WEEKS)
      throw Exceptions.unprocessableEntity("Timestamp %s is out of range or not legal", timestamp);
  }
}
