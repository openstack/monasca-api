/*
 * (C) Copyright 2016 Hewlett Packard Enterprise Development LP
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

/**
 * Encapsulates dimension name for an optional metric name.
 */
public class DimensionName extends DimensionBase {
    final private String dimensionName;

    public DimensionName(String metricName, String dimensionName) {
        super(metricName, dimensionName);
        this.dimensionName = dimensionName;
    }

    public String getDimensionName() {
        return dimensionName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        DimensionName other = (DimensionName) obj;
        if (dimensionName == null) {
            if (other.dimensionName != null)
                return false;
        } else if (!dimensionName.equals(other.dimensionName))
            return false;
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((dimensionName == null) ? 0 : dimensionName.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return String.format("DimensionName: MetricName=%s DimensionName [names=%s]",
                getMetricName(), dimensionName);
    }
}
