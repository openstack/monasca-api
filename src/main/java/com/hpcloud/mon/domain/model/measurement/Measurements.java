package com.hpcloud.mon.domain.model.measurement;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates a metric measurements.
 * 
 * @author Jonathan Halterman
 */
public class Measurements {
  private String name;
  private Map<String, String> dimensions;
  private List<Measurement> measurements;

  public Measurements() {
  }

  public Measurements(String name, Map<String, String> dimensions, List<Measurement> measurements) {
    this.name = name;
    this.dimensions = dimensions;
    this.measurements = measurements;
  }

  public void addMeasurement(Measurement measurement) {
    measurements.add(measurement);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Measurements other = (Measurements) obj;
    if (dimensions == null) {
      if (other.dimensions != null)
        return false;
    } else if (!dimensions.equals(other.dimensions))
      return false;
    if (measurements == null) {
      if (other.measurements != null)
        return false;
    } else if (!measurements.equals(other.measurements))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  public Map<String, String> getDimensions() {
    return dimensions;
  }

  public List<Measurement> getMeasurements() {
    return measurements;
  }

  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dimensions == null) ? 0 : dimensions.hashCode());
    result = prime * result + ((measurements == null) ? 0 : measurements.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  public void setDimensions(Map<String, String> dimensions) {
    this.dimensions = dimensions;
  }

  public void setMeasurements(List<Measurement> measurements) {
    this.measurements = measurements;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return String.format("Measurement [name=%s, dimensions=%s, measurements=%s]", name, dimensions,
        measurements);
  }
}
