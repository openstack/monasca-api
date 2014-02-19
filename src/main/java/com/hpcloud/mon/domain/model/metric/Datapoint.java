package com.hpcloud.mon.domain.model.metric;

import java.util.Map;

/**
 * Encapsulates a metric datapoint.
 * 
 * @author Jonathan Halterman
 */
public class Datapoint {
  private Map<String, String> dimensions;
  private Map<String, Object> datapoints;

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    Datapoint other = (Datapoint) obj;
    if (datapoints == null) {
      if (other.datapoints != null)
        return false;
    } else if (!datapoints.equals(other.datapoints))
      return false;
    if (dimensions == null) {
      if (other.dimensions != null)
        return false;
    } else if (!dimensions.equals(other.dimensions))
      return false;
    return true;
  }

  public Map<String, Object> getDatapoints() {
    return datapoints;
  }

  public Map<String, String> getDimensions() {
    return dimensions;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((datapoints == null) ? 0 : datapoints.hashCode());
    result = prime * result + ((dimensions == null) ? 0 : dimensions.hashCode());
    return result;
  }

  public void setDatapoints(Map<String, Object> datapoints) {
    this.datapoints = datapoints;
  }

  public void setDimensions(Map<String, String> dimensions) {
    this.dimensions = dimensions;
  }
}
