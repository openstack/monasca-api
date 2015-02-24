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
package monasca.api.infrastructure.persistence.influxdb;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import monasca.api.ApiConfig;
import monasca.api.domain.model.metric.MetricDefinitionRepo;
import monasca.common.model.metric.MetricDefinition;

import static monasca.api.infrastructure.persistence.influxdb.InfluxV9Utils.dimPart;
import static monasca.api.infrastructure.persistence.influxdb.InfluxV9Utils.namePart;
import static monasca.api.infrastructure.persistence.influxdb.InfluxV9Utils.regionPart;
import static monasca.api.infrastructure.persistence.influxdb.InfluxV9Utils.tenantIdPart;

public class InfluxV9MetricDefinitionRepo implements MetricDefinitionRepo {

  private static final Logger logger = LoggerFactory.getLogger(InfluxV9MetricDefinitionRepo.class);

  private final ApiConfig config;
  private final InfluxV9RepoReader influxV9RepoReader;
  private final String region;

  private final ObjectMapper objectMapper = new ObjectMapper();


  @Inject
  public InfluxV9MetricDefinitionRepo(ApiConfig config,
                                      InfluxV9RepoReader influxV9RepoReader) {
    this.config = config;
    this.region = config.region;
    this.influxV9RepoReader = influxV9RepoReader;

  }

  @Override
  public List<MetricDefinition> find(String tenantId, String name,
                                     Map<String, String> dimensions,
                                     String offset) throws Exception {

    String
        q =
        String.format("show series %1$s where %2$s %3$s %4$s", namePart(name), tenantIdPart(
            tenantId),
                      regionPart(this.region), dimPart(dimensions));

    logger.debug("Metric definition query: {}", q);

    String r = this.influxV9RepoReader.read(q);

    Series series = this.objectMapper.readValue(r, Series.class);

    List<MetricDefinition> metricDefinitionList = metricDefinitionList(series);

    logger.debug("Found {} metric definitions matching query", metricDefinitionList.size());

    return metricDefinitionList;
  }

  private List<MetricDefinition> metricDefinitionList(Series series) {

    List<MetricDefinition> metricDefinitionList = new ArrayList<>();

    if (!series.isEmpty()) {

      for (Serie serie : series.getSeries()) {

        for (String[] values : serie.getValues()) {

          metricDefinitionList.add(new MetricDefinition(serie.getName(), dims(values, serie.getColumns())));
        }
      }
    }

    return metricDefinitionList;
  }

  private Map<String, String> dims(String[] vals, String[] cols) {

    Map<String, String> dims = new HashMap<>();

    for (int i = 0; i < cols.length; ++i) {
      if (!vals[i].equalsIgnoreCase("null")) {
        dims.put(cols[i], vals[i]);
      }
    }
    return dims;
  }

}

