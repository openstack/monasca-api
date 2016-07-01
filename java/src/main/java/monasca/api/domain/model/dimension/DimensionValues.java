/*
 * Copyright (c) 2016 Hewlett-Packard Development Company, L.P.
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
package monasca.api.domain.model.dimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import monasca.common.model.domain.common.AbstractEntity;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.binary.Hex;

/**
 * Encapsulates the list of dimension values for a given dimension name
 * (and optional metric-name).
 */
public class DimensionValues extends AbstractEntity {

  protected String id = null;
  protected String dimensionName;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  protected String metricName;
  protected List<String> values;
  protected Map<String, List<String>> dimensionValues;

  public DimensionValues() {
    this.values = new ArrayList<String>();
    this.dimensionValues = new HashMap<String, List<String>>();
  }

  public DimensionValues(String metricName, String dimensionName, List<String> values) {
    this.metricName = metricName;
    this.dimensionName = dimensionName;
    this.values = values;
    this.dimensionValues = new HashMap<String, List<String>>();
    this.dimensionValues.put(dimensionName, values);
    this.id = generateId();
  }

  public List<String> getValues() {
    return values;
  }

  public String getDimensionName() {
    return dimensionName;
  }

  public String getMetricName() {
    return metricName;
  }

  public String getId() {
    if (null == this.id) {
      this.id = generateId();
    }
    return this.id;
  }

  private String generateId() {
    String hashstr = "metricName=" + metricName + "dimensionName=" + dimensionName;
    byte[] sha1Hash = DigestUtils.sha(hashstr);
    return Hex.encodeHexString(sha1Hash);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DimensionValues other = (DimensionValues) obj;
    if (dimensionName == null) {
      if (other.dimensionName != null)
        return false;
    } else if (!dimensionName.equals(other.dimensionName))
      return false;
    if (metricName == null) {
      if (other.metricName != null)
        return false;
    } else if (!metricName.equals(other.metricName))
      return false;
    if (values == null) {
      if (other.values != null)
        return false;
    } else if (!values.equals(other.values))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dimensionName == null) ? 0 : dimensionName.hashCode());
    result = prime * result + ((metricName == null) ? 0 : metricName.hashCode());
    result = prime * result + ((values == null) ? 0 : values.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return String.format("MetricName=%s DimensionValues [name=%s, values=%s]",
                         metricName, dimensionName, values);
  }
}
