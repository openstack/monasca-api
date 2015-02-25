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

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import monasca.api.ApiConfig;
import monasca.api.domain.model.common.Paged;
import monasca.api.domain.model.metric.MetricDefinitionRepo;
import monasca.common.model.metric.MetricDefinition;

import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.buildSerieNameRegex;
import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.urlDecodeUTF8;
import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.urlEncodeUTF8;

public class InfluxV8MetricDefinitionRepo implements MetricDefinitionRepo {

  private static final Logger
      logger =
      LoggerFactory.getLogger(InfluxV8MetricDefinitionRepo.class);

  private final ApiConfig config;
  private final InfluxDB influxDB;

  @Inject
  public InfluxV8MetricDefinitionRepo(ApiConfig config, InfluxDB influxDB) {
    this.config = config;
    this.influxDB = influxDB;
  }

  @Override
  public List<MetricDefinition> find(String tenantId, String name, Map<String, String> dimensions,
                                     String offset, int limit) throws Exception {

    // Limit is not implemented for Influxdb V8.

    String serieNameRegex = buildSerieNameRegex(tenantId, config.region, name, dimensions);

    String query = String.format("list series /%1$s/", serieNameRegex);
    logger.debug("Query string: {}", query);

    List<Serie>
        result =
        this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.MILLISECONDS);
    return buildMetricDefList(result, offset);
  }

  private List<MetricDefinition> buildMetricDefList(List<Serie> result, String offset)
      throws Exception {

    // offset comes in as url encoded.
    String decodedOffset = null;
    if (offset != null) {
      decodedOffset = urlDecodeUTF8(offset);
    }

    List<MetricDefinition> metricDefinitionList = new ArrayList<>();
    for (Serie serie : result) {
      for (Map<String, Object> point : serie.getRows()) {

        String encodedMetricName = (String) point.get("name");


        if (offset != null) {
          if (encodedMetricName.compareTo(decodedOffset) <= 0) {
            continue;
          }
        }

        InfluxV8Utils.SerieNameDecoder serieNameDecoder;

        try {
          serieNameDecoder = new InfluxV8Utils.SerieNameDecoder(encodedMetricName);
        } catch (InfluxV8Utils.SerieNameDecodeException e) {
          logger.warn("Dropping series name that is not decodable: {}", point.get("name"), e);
          continue;
        }

        MetricDefinition
            metricDefinition =
            new MetricDefinition(serieNameDecoder.getMetricName(),
                                 serieNameDecoder.getDimensions());
        // Must url encode offset because it is part of a url.
        metricDefinition.setId(urlEncodeUTF8(encodedMetricName));
        metricDefinitionList.add(metricDefinition);

        if (offset != null) {
          if (metricDefinitionList.size() >= Paged.LIMIT) {
            return metricDefinitionList;
          }
        }
      }
    }
    return metricDefinitionList;
  }
}
