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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import monasca.api.ApiConfig;
import monasca.api.domain.exception.MultipleMetricsException;
import monasca.api.domain.model.measurement.MeasurementRepo;
import monasca.api.domain.model.measurement.Measurements;

public class InfluxV9MeasurementRepo implements MeasurementRepo {

  private static final Logger logger = LoggerFactory
      .getLogger(InfluxV9MeasurementRepo.class);

  private final static TypeReference VALUE_META_TYPE = new TypeReference<Map<String, String>>() {};

  private final ApiConfig config;
  private final String region;
  private final InfluxV9RepoReader influxV9RepoReader;
  private final InfluxV9Utils influxV9Utils;
  private final InfluxV9MetricDefinitionRepo influxV9MetricDefinitionRepo;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Inject
  public InfluxV9MeasurementRepo(ApiConfig config,
                                 InfluxV9RepoReader influxV9RepoReader,
                                 InfluxV9Utils influxV9Utils,
                                 InfluxV9MetricDefinitionRepo influxV9MetricDefinitionRepo) {
    this.config = config;
    this.region = config.region;
    this.influxV9RepoReader = influxV9RepoReader;
    this.influxV9Utils = influxV9Utils;
    this.influxV9MetricDefinitionRepo = influxV9MetricDefinitionRepo;

  }

  @Override
  public List<Measurements> find(String tenantId, String name, Map<String, String> dimensions,
                                 DateTime startTime, @Nullable DateTime endTime,
                                 @Nullable String offset, int limit, Boolean mergeMetricsFlag)
      throws Exception {

    String q = buildQuery(tenantId, name, dimensions, startTime, endTime,
                          offset, limit, mergeMetricsFlag);


    String r = this.influxV9RepoReader.read(q);

    Series series = this.objectMapper.readValue(r, Series.class);

    List<Measurements> measurementsList = measurementsList(series);

    logger.debug("Found {} metrics matching query", measurementsList.size());

    return measurementsList;
  }

  private String buildQuery(String tenantId, String name, Map<String, String> dimensions,
                            DateTime startTime, DateTime endTime, String offset, int limit,
                            Boolean mergeMetricsFlag) throws Exception {

    String q;
    if (Boolean.TRUE.equals(mergeMetricsFlag)) {

      // The time column is automatically included in the results before all other columns.
      q = String.format("select value, value_meta %1$s "
                        + "where %2$s %3$s %4$s %5$s %6$s %7$s %8$s",
                        this.influxV9Utils.namePart(name, true),
                        this.influxV9Utils.privateTenantIdPart(tenantId),
                        this.influxV9Utils.privateRegionPart(this.region),
                        this.influxV9Utils.startTimePart(startTime),
                        this.influxV9Utils.dimPart(dimensions),
                        this.influxV9Utils.endTimePart(endTime),
                        this.influxV9Utils.timeOffsetPart(offset),
                        this.influxV9Utils.limitPart(limit));

    } else {

      if (!this.influxV9MetricDefinitionRepo.isAtMostOneSeries(tenantId, name, dimensions)) {

        throw new MultipleMetricsException(name, dimensions);

      }
      // The time column is automatically included in the results before all other columns.
      q = String.format("select value, value_meta %1$s "
                        + "where %2$s %3$s %4$s %5$s %6$s %7$s %8$s %9$s slimit 1",
                        this.influxV9Utils.namePart(name, true),
                        this.influxV9Utils.privateTenantIdPart(tenantId),
                        this.influxV9Utils.privateRegionPart(this.region),
                        this.influxV9Utils.startTimePart(startTime),
                        this.influxV9Utils.dimPart(dimensions),
                        this.influxV9Utils.endTimePart(endTime),
                        this.influxV9Utils.timeOffsetPart(offset),
                        this.influxV9Utils.groupByPart(),
                        this.influxV9Utils.limitPart(limit));
    }

    logger.debug("Measurements query: {}", q);

    return q;
  }

  private List<Measurements> measurementsList(Series series) {
    List<Measurements> measurementsList = new LinkedList<>();

    if (!series.isEmpty()) {

      for (Serie serie : series.getSeries()) {

        Measurements measurements =
            new Measurements(serie.getName(),
                             influxV9Utils.filterPrivateTags(serie.getTags()));

        for (String[] values : serie.getValues()) {
          final String timestamp = influxV9Utils.threeDigitMillisTimestamp(values[0]);
          measurements.addMeasurement(
              new Object[]{timestamp, Double.parseDouble(values[1]), getValueMeta(values)});
        }

        measurementsList.add(measurements);
      }
    }

    return measurementsList;

  }

  private Map<String, String> getValueMeta(String[] values) {

    Map<String, String> valueMetaMap = new HashMap<>();

    String valueMetaStr = values[2];

    if (valueMetaStr != null && !valueMetaStr.isEmpty()) {

      try {
        valueMetaMap = this.objectMapper.readValue(valueMetaStr, VALUE_META_TYPE);
      } catch (IOException e) {
        logger.error("Failed to parse value metadata: {}", values[2], e);

      }
    }

    return valueMetaMap;
  }
}
