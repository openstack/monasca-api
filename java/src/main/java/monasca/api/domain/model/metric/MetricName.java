/*
 * (C) Copyright 2014, 2016 Hewlett Packard Enterprise Development LP
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
package monasca.api.domain.model.metric;

import com.fasterxml.jackson.annotation.JsonIgnore;

import monasca.common.model.domain.common.AbstractEntity;


public class MetricName extends AbstractEntity implements Comparable<MetricName> {

  private String id;
  private String name;

  public MetricName(String name) {
    this.id = name;
    this.name = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MetricName other = (MetricName) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  @JsonIgnore
  public String getId() {return id;}

  public String getName() {return name;}

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 17;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  public void setName(String name) {this.name = name;}

  @Override
  public int compareTo(MetricName other) {
    return this.name.compareTo(other.name);
  }
}
