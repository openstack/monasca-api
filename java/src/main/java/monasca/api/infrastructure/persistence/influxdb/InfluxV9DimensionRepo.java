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
package monasca.api.infrastructure.persistence.influxdb;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;

import monasca.api.ApiConfig;
import monasca.api.domain.model.dimension.DimensionValues;
import monasca.api.domain.model.dimension.DimensionRepo;


public class InfluxV9DimensionRepo implements DimensionRepo {

  private static final Logger logger = LoggerFactory.getLogger(InfluxV9DimensionRepo.class);

  private final ApiConfig config;
  private final InfluxV9RepoReader influxV9RepoReader;
  private final InfluxV9Utils influxV9Utils;
  private final String region;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Inject
  public InfluxV9DimensionRepo(ApiConfig config,
                               InfluxV9RepoReader influxV9RepoReader,
                               InfluxV9Utils influxV9Utils) {
    this.config = config;
    this.region = config.region;
    this.influxV9RepoReader = influxV9RepoReader;
    this.influxV9Utils = influxV9Utils;
  }

  @Override
  public DimensionValues find(
    String metricName,
    String tenantId,
    String dimensionName,
    String offset,
    int limit) throws Exception
  {
    //
    // Use treeset to keep list in alphabetic/predictable order
    // for string based offset.
    //
    Set<String> matchingValues = new TreeSet<String>();
    String dimNamePart = "and \""
                         + this.influxV9Utils.sanitize(dimensionName)
                         + "\" =~ /.*/";

    String q = String.format("show series %1$s where %2$s %3$s",
                             this.influxV9Utils.namePart(metricName, false),
                             this.influxV9Utils.privateTenantIdPart(tenantId),
                             dimNamePart);

    logger.debug("Dimension values query: {}", q);
    String r = this.influxV9RepoReader.read(q);
    Series series = this.objectMapper.readValue(r, Series.class);

    if (!series.isEmpty()) {
      for (Serie serie : series.getSeries()) {
        for (String[] values : serie.getValues()) {
          Map<String, String> dimensions = this.influxV9Utils.getDimensions(values, serie.getColumns());
          for (Map.Entry<String, String> entry : dimensions.entrySet()) {
            if (dimensionName.equals(entry.getKey())) {
              matchingValues.add(entry.getValue());
            }
          }
        }
      }
    }

    List<String> filteredValues = filterDimensionValues(matchingValues,
                                                        dimensionName,
                                                        limit,
                                                        offset);

    return new DimensionValues(metricName, dimensionName, filteredValues);
  }

  private List<String> filterDimensionValues(Set<String> matchingValues,
                                             String dimensionName,
                                             int limit,
                                             String offset)
  {
    Boolean haveOffset = (null != offset && !"".equals(offset));
    List<String> filteredValues = new ArrayList<String>();
    int remaining_limit = limit + 1;

    for (String dimVal : matchingValues) {
      if (remaining_limit <= 0) {
        break;
      }
      if (haveOffset && dimVal.compareTo(offset) <= 0) {
        continue;
      }
      filteredValues.add(dimVal);
      remaining_limit--;
    }

    return filteredValues;
  }
}
