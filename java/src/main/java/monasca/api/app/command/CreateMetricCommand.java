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

import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import monasca.api.app.validation.DimensionValidation;
import monasca.api.app.validation.MetricNameValidation;
import monasca.api.app.validation.ValueMetaValidation;
import monasca.common.model.metric.Metric;
import monasca.api.resource.exception.Exceptions;

public class CreateMetricCommand {
  private static final long TIME_2MIN_MILLIS = 120*1000;
  private static final long TIME_2WEEKS_MILLIS = 1209600*1000;
  public static final int MAX_NAME_LENGTH = 255;

  @NotEmpty
  @Size(min = 1, max = MAX_NAME_LENGTH)
  public String name;
  public Map<String, String> dimensions;
  @NotNull
  public Long timestamp;
  @NotNull
  public Double value;
  public Map<String, String> valueMeta;

  public CreateMetricCommand() {}

  public CreateMetricCommand(String name, @Nullable Map<String, String> dimensions,
      long timestamp, double value, @Nullable Map<String, String> valueMeta) {
    setName(name);
    setDimensions(dimensions);
    this.timestamp = timestamp;
    setValueMeta(valueMeta);
    this.value = value;
  }

  private static void validateTimestamp(long timestamp) {
    long time = System.currentTimeMillis();
    if (timestamp > time + TIME_2MIN_MILLIS || timestamp < time - TIME_2WEEKS_MILLIS)
      throw Exceptions.unprocessableEntity("Timestamp %s is out of legal range", timestamp);
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
    result = prime * result + ((timestamp == null) ? 0: (int) (timestamp ^ (timestamp >>> 32)));
    long temp;
    temp = (value == null) ? 0 : Double.doubleToLongBits(value);
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
  public void setValueMeta(Map<String, String> valueMeta) {
    this.valueMeta = ValueMetaValidation.normalize(valueMeta);
  }

  @JsonProperty
  public void setName(String name) {
    this.name = MetricNameValidation.normalize(name);
  }

  public Metric toMetric() {
    return new Metric(name, dimensions, timestamp, value, valueMeta);
  }

  public void validate() {
    // Validate name and dimensions
    MetricNameValidation.validate(name, true);
    if (dimensions != null) {
      DimensionValidation.validate(dimensions);
    }
    if (valueMeta != null) {
      ValueMetaValidation.validate(valueMeta);
    }

    // Validate times and values
    validateTimestamp(timestamp);
  }
}
