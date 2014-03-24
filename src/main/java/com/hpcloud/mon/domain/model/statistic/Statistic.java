package com.hpcloud.mon.domain.model.statistic;

import java.util.Map;

/**
 * Encapsulates a metric measurements.
 * 
 * @author Jonathan Halterman
 */
public class Statistic {
  private Map<String, String> dimensions;
  private Map<String, Object> measurements;

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    Statistic other = (Statistic) obj;
    if (measurements == null) {
      if (other.measurements != null)
        return false;
    } else if (!measurements.equals(other.measurements))
      return false;
    if (dimensions == null) {
      if (other.dimensions != null)
        return false;
    } else if (!dimensions.equals(other.dimensions))
      return false;
    return true;
  }

  public Map<String, Object> getMeasurements() {
    return measurements;
  }

  public Map<String, String> getDimensions() {
    return dimensions;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((measurements == null) ? 0 : measurements.hashCode());
    result = prime * result + ((dimensions == null) ? 0 : dimensions.hashCode());
    return result;
  }

  public void setMeasurements(Map<String, Object> measurements) {
    this.measurements = measurements;
  }

  public void setDimensions(Map<String, String> dimensions) {
    this.dimensions = dimensions;
  }
}
