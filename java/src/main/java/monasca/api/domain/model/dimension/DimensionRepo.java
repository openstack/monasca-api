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

import javax.annotation.Nullable;

import java.util.List;

/**
 * Repository for dimensions.
 */
public interface DimensionRepo {
  /**
   * Finds dimension values given a dimension name and
   * optional metric name.
   */
  List<DimensionValue> findValues(String metricName,
                                  String tenantId,
                                  String dimensionName,
                                  @Nullable String offset,
                                  int limit)
      throws Exception;

  /**
   * Finds dimension names given an optional metric name.
   */
  List<DimensionName> findNames(String metricName,
                                String tenantId,
                                @Nullable String offset,
                                int limit)
      throws Exception;
}
