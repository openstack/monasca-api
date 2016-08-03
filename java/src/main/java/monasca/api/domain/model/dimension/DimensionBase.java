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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import monasca.common.model.domain.common.AbstractEntity;

/**
 * Base class for DimensionNames and DimensionValues.
 */
public abstract class DimensionBase extends AbstractEntity {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    final private String metricName;
    final private String id;

    public DimensionBase(String metricName, String id) {
        this.metricName = metricName;
        this.id = id;
    }

    public String getMetricName() {
        return metricName;
    }

    @JsonIgnore
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DimensionBase other = (DimensionBase) obj;
        if (metricName == null) {
            if (other.getMetricName() != null)
                return false;
        } else if (!metricName.equals(other.getMetricName()))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 17;
        result = prime * result + ((metricName == null) ? 0 : metricName.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }
}
