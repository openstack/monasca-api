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
package monasca.api.app.command;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import monasca.api.app.validation.DimensionValidation;
import monasca.api.app.validation.MetricNameValidation;
import com.hpcloud.mon.common.model.Services;
import com.hpcloud.mon.common.model.metric.Metric;
import monasca.api.resource.exception.Exceptions;

public class CreateMetricCommand {
  private static final long TIME_2MIN = 120;
  private static final long TIME_2WEEKS = 1209600;
  private static final double VALUE_MIN = 8.515920e-109;
  private static final double VALUE_MAX = 1.174271e+108;

  @NotEmpty
  @Size(min = 1, max = 64)
  public String name;
  public Map<String, String> dimensions;
  public long timestamp;
  public double value;
  public double[][] timeValues;

  public CreateMetricCommand() {}

  public CreateMetricCommand(String name, @Nullable Map<String, String> dimensions,
      @Nullable Long timestamp, double value) {
    setName(name);
    setDimensions(dimensions);
    setTimestamp(timestamp);
    this.value = value;
  }

  public CreateMetricCommand(String name, @Nullable Map<String, String> dimensions,
      @Nullable Long timestamp, double[][] timeValues) {
    setName(name);
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
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
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
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    this.dimensions =
        dimensions == null || dimensions.isEmpty() ? null : DimensionValidation
            .normalize(dimensions);
  }

  @JsonProperty
  public void setName(String name) {
    this.name = MetricNameValidation.normalize(name);
  }

  @JsonProperty
  public void setTimestamp(Long timestamp) {
    this.timestamp =
        timestamp == null || timestamp.longValue() == 0L ? System.currentTimeMillis() / 1000L
            : timestamp.longValue();
  }

  public Metric toMetric() {
    return timeValues == null || timeValues.length == 0 ? new Metric(name, dimensions, timestamp,
        value) : new Metric(name, dimensions, timestamp, timeValues);
  }

  public void validate() {
    // Validate name and dimensions
    if (dimensions != null) {
      String service = dimensions.get(Services.SERVICE_DIMENSION);
      MetricNameValidation.validate(name, service);
      DimensionValidation.validate(dimensions, service);
    }

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
