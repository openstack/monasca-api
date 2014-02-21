package com.hpcloud.mon.app.command;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nullable;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hpcloud.mon.app.validate.DimensionValidation;
import com.hpcloud.mon.app.validate.NamespaceValidation;
import com.hpcloud.mon.app.validate.Validateable;
import com.hpcloud.mon.app.validate.ValidationResult;
import com.hpcloud.mon.common.model.metric.Metric;

public class CreateMetricCommand implements Validateable {
  @NotEmpty public String namespace;
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
  public void setNamespace(String namespace) {
    this.namespace = NamespaceValidation.normalize(namespace);
  }

  @JsonProperty
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp == null || timestamp.longValue() == 0L ? System.currentTimeMillis() / 1000L
        : timestamp.longValue();
  }

  @JsonProperty
  public void setDimensions(Map<String, String> dimensions) {
    this.dimensions = dimensions == null || dimensions.isEmpty() ? null
        : DimensionValidation.normalize(dimensions);
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

  @Override
  public ValidationResult validate() {
    return null;
  }
}
