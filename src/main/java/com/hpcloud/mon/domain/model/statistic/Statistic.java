package com.hpcloud.mon.domain.model.statistic;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates a metric measurements.
 * 
 * @author Jonathan Halterman
 */
public class Statistic {
  private String name;
  private Map<String, String> dimensions;
  private List<String> columns;
  private List<List<Object>> values;

  public Statistic() {
    values = new ArrayList<>();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Statistic other = (Statistic) obj;
    if (dimensions == null) {
      if (other.dimensions != null)
        return false;
    } else if (!dimensions.equals(other.dimensions))
      return false;
    if(columns==null) {
      if(other.columns!=null)
        return false;
    } else if(!columns.equals(other.columns))
      return false;
    if(values==null) {
      if(other.values!=null)
        return false;
    } else if(!values.equals(other.values))
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

  public String getName() {
    return name;
  }

  public List<String> getColumns() { return columns;}
  public List<List<Object>> getValues() { return values;}


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dimensions == null) ? 0 : dimensions.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((values == null) ?  0 : values.hashCode());
    result = prime * result + ((columns == null) ? 0 : columns.hashCode());
    return result;
  }

  public void setDimensions(Map<String, String> dimensions) {
    this.dimensions = dimensions;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setColumns(List<String> columns) { this.columns =columns;}

  public void setValues(List<List<Object>> values) { this.values = values;}

  public void addValues(List<Object> value) {
    values.add(value);
  }

  @Override
  public String toString() {
    return String.format("Statistic [name=%s, dimensions=%s,statistics=%s]", name, dimensions,
        values);
  }
}
