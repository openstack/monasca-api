package com.hpcloud.mon.app.command;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hpcloud.mon.app.validation.DimensionValidation;
import com.hpcloud.mon.app.validation.NamespaceValidation;
import com.hpcloud.mon.common.model.metric.Metric;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * @author Jonathan Halterman
 * @author Todd Walk
 */
public class CreateMetricCommand {
  private static final long TIME_2MIN = 120;
  private static final long TIME_2WEEKS = 1209600;
  private static final double VALUE_MIN = 8.515920e-109;
  private static final double VALUE_MAX = 1.174271e+108;

  @NotEmpty @Size(min = 1, max = 64) public String namespace;
  public Map<String, String> dimensions;
  public long timestamp;
  public double value;
  public double[][] timeValues;

  public CreateMetricCommand() {
  }

  public CreateMetricCommand(String namespace, @Nullable Map<String, String> dimensions,
      @Nullable Long timestamp, double value) {
    setNamespace(namespace);
    setDimensions(dimensions);
    setTimestamp(timestamp);
    this.value = value;
  }

  public CreateMetricCommand(String namespace, @Nullable Map<String, String> dimensions,
      @Nullable Long timestamp, double[][] timeValues) {
    setNamespace(namespace);
    setDimensions(dimensions);
    setTimestamp(timestamp);
    this.timeValues = timeValues;
  }

  private static void validateTimestamp(long timestamp) {
    long time = System.currentTimeMillis() / 1000;
    if (timestamp > time + TIME_2MIN || timestamp < time - TIME_2WEEKS)
      throw Exceptions.unprocessableEntity("Timestamp %s is out of legal range", timestamp);
  }

  private static void validateValue(double value) {
    if (value < 0 || (value > 0 && (value < VALUE_MIN || value > VALUE_MAX)))
      throw Exceptions.unprocessableEntity("Value %s is out of legal range", value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CreateMetricCommand other = (CreateMetricCommand) obj;
    if (dimensions == null) {
      if (other.dimensions != null)
        return false;
    } else if (!dimensions.equals(other.dimensions))
      return false;
    if (namespace == null) {
      if (other.namespace != null)
        return false;
    } else if (!namespace.equals(other.namespace))
      return false;
    // Note - Deep Equals is used here
    if (!Arrays.deepEquals(timeValues, other.timeValues))
      return false;
    if (timestamp != other.timestamp)
      return false;
    if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dimensions == null) ? 0 : dimensions.hashCode());
    result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
    // Note Deep hash code is used here
    result = prime * result + Arrays.deepHashCode(timeValues);
    result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
    long temp;
    temp = Double.doubleToLongBits(value);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @JsonProperty
  public void setDimensions(Map<String, String> dimensions) {
    this.dimensions = dimensions == null || dimensions.isEmpty() ? null
        : DimensionValidation.normalize(dimensions);
  }

  @JsonProperty
  public void setNamespace(String namespace) {
    this.namespace = NamespaceValidation.normalize(namespace);
  }

  @JsonProperty
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp == null || timestamp.longValue() == 0L ? System.currentTimeMillis() / 1000L
        : timestamp.longValue();
  }

  public Metric toMetric() {
    return timeValues == null || timeValues.length == 0 ? new Metric(namespace, dimensions,
        timestamp, value) : new Metric(namespace, dimensions, timestamp, timeValues);
  }

  @Override
  public String toString() {
    return String.format("FlatMetric [namespace=%s,  dimensions=%s, timestamp=%s, value=%s]",
        namespace, dimensions, timestamp, timeValues == null ? value : Arrays.toString(timeValues));
  }

  public void validate() {
    // Validate namespace and dimensions
    NamespaceValidation.validate(namespace);
    DimensionValidation.validate(namespace, dimensions);

    // Validate times and values
    validateTimestamp(timestamp);
    if (timeValues != null && timeValues.length > 0) {
      for (double[] timeValuePair : timeValues) {
        if (timeValuePair.length != 2)
          throw Exceptions.unprocessableEntity("All times_values must be timestamp / value pairs");
        validateTimestamp((long) timeValuePair[0]);
        validateValue(timeValuePair[1]);
      }
    } else {
      validateValue(value);
    }
  }
}
