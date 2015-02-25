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
package monasca.api.domain.model.statistic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import monasca.common.model.domain.common.AbstractEntity;

/**
 * Encapsulates a metric measurements.
 */
public class Statistics extends AbstractEntity {

  private String name;
  private Map<String, String> dimensions;
  private List<String> columns;
  private List<List<Object>> statistics;

  public Statistics() {
    statistics = new ArrayList<>();
  }

  public Statistics(String name, Map<String, String> dimensions, List<String> columns) {
    this.name = name;
    this.dimensions = dimensions;
    this.columns = columns;
    this.statistics = new LinkedList<>();

  }

  public void addValues(List<Object> value) {
    statistics.add(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Statistics other = (Statistics) obj;
    if (dimensions == null) {
      if (other.dimensions != null)
        return false;
    } else if (!dimensions.equals(other.dimensions))
      return false;
    if (columns == null) {
      if (other.columns != null)
        return false;
    } else if (!columns.equals(other.columns))
      return false;
    if (statistics == null) {
      if (other.statistics != null)
        return false;
    } else if (!statistics.equals(other.statistics))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;

    return true;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void addStatistics(List<Object> statistics) {
    this.statistics.add(statistics);
  }

  public List<String> getColumns() {
    return columns;
  }

  public Map<String, String> getDimensions() {
    return dimensions;
  }

  public String getName() {
    return name;
  }

  public List<List<Object>> getStatistics() {
    return statistics;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dimensions == null) ? 0 : dimensions.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((statistics == null) ? 0 : statistics.hashCode());
    result = prime * result + ((columns == null) ? 0 : columns.hashCode());
    return result;
  }

  public void setColumns(List<String> columns) {
    this.columns = columns;
  }

  public void setDimensions(Map<String, String> dimensions) {
    this.dimensions = dimensions;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setStatistics(List<List<Object>> statistics) {
    this.statistics = statistics;
  }

  @Override
  public String toString() {
    return String.format("Statistics [name=%s, dimensions=%s,statistics=%s]", name, dimensions,
        statistics);
  }
}
