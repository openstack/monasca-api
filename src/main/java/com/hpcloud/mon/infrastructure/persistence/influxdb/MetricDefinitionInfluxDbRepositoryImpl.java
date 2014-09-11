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
package com.hpcloud.mon.infrastructure.persistence.influxdb;

import com.google.inject.Inject;

import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.model.metric.MetricDefinitionRepository;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hpcloud.mon.infrastructure.persistence.influxdb.Utils.buildSerieNameRegex;
import static com.hpcloud.mon.infrastructure.persistence.influxdb.Utils.isSerieMetricName;

public class MetricDefinitionInfluxDbRepositoryImpl implements MetricDefinitionRepository {

  private static final Logger logger = LoggerFactory.getLogger
      (MetricDefinitionInfluxDbRepositoryImpl.class);

  private final MonApiConfiguration config;
  private final InfluxDB influxDB;

  @Inject
  public MetricDefinitionInfluxDbRepositoryImpl(MonApiConfiguration config, InfluxDB influxDB) {
    this.config = config;
    this.influxDB = influxDB;
  }

  @Override
  public List<MetricDefinition> find(String tenantId, String name, Map<String,
      String> dimensions) throws Exception {

    String serieNameRegex = buildSerieNameRegex(tenantId, name, dimensions);

    String query = String.format("list series /%1$s/", serieNameRegex);
    logger.debug("Query string: {}", query);

    List<MetricDefinition> metricDefinitionList = new ArrayList<>();

    List<Serie> result = this.influxDB.Query(this.config.influxDB.getName(), query,
                                             TimeUnit.SECONDS);
    for (Serie serie : result) {
      for (Map point : serie.getRows()) {

        Utils.SerieNameDecoder serieNameDecoder;
        try {
          serieNameDecoder = new Utils.SerieNameDecoder((String) point.get("name"));
        } catch (Utils.SerieNameDecodeException e) {
          logger.warn("Dropping series name that is not decodable: {}", point.get("name"), e);
          continue;
        }

        MetricDefinition metricDefinition = new MetricDefinition(serieNameDecoder.getMetricName(),
                                                                 serieNameDecoder
                                                                     .getDimensions());
        metricDefinitionList.add(metricDefinition);
      }

    }
    return metricDefinitionList;
  }
}