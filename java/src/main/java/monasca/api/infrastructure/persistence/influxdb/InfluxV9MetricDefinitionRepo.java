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
package monasca.api.infrastructure.persistence.influxdb;

import com.google.common.base.Strings;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;

import monasca.api.ApiConfig;
import monasca.api.domain.model.metric.MetricDefinitionRepo;
import monasca.api.domain.model.metric.MetricName;
import monasca.common.model.metric.MetricDefinition;


public class InfluxV9MetricDefinitionRepo implements MetricDefinitionRepo {

  private static final Logger logger = LoggerFactory.getLogger(InfluxV9MetricDefinitionRepo.class);

  private final ApiConfig config;
  private final InfluxV9RepoReader influxV9RepoReader;
  private final InfluxV9Utils influxV9Utils;
  private final String region;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Inject
  public InfluxV9MetricDefinitionRepo(ApiConfig config,
                                      InfluxV9RepoReader influxV9RepoReader,
                                      InfluxV9Utils influxV9Utils) {
    this.config = config;
    this.region = config.region;
    this.influxV9RepoReader = influxV9RepoReader;
    this.influxV9Utils = influxV9Utils;

  }

  boolean isAtMostOneSeries(String tenantId, String name, Map<String, String> dimensions)
      throws Exception {

    // Set limit to 2. We only care if we get 0, 1, or 2 results back.
    String q = String.format("show series %1$s "
                             + "where %2$s %3$s %4$s limit 2",
                             this.influxV9Utils.namePart(name, false),
                             this.influxV9Utils.privateTenantIdPart(tenantId),
                             this.influxV9Utils.privateRegionPart(this.region),
                             this.influxV9Utils.dimPart(dimensions));

    logger.debug("Metric definition query: {}", q);

    String r = this.influxV9RepoReader.read(q);

    Series series = this.objectMapper.readValue(r, Series.class);

    List<MetricDefinition> metricDefinitionList = metricDefinitionList(series,
                                                                       tenantId,
                                                                       name,
                                                                       null,
                                                                       null,
                                                                       0);

    logger.debug("Found {} metric definitions matching query", metricDefinitionList.size());

    return metricDefinitionList.size() > 1 ? false : true;

  }

  @Override
  public List<MetricDefinition> find(String tenantId, String name,
                                     Map<String, String> dimensions,
                                     DateTime startTime,
                                     DateTime endTime,
                                     String offset, int limit) throws Exception {

    int startIndex = this.influxV9Utils.startIndex(offset);

    String q = String.format("show series %1$s "
                             + "where %2$s %3$s %4$s %5$s %6$s",
                             this.influxV9Utils.namePart(name, false),
                             this.influxV9Utils.privateTenantIdPart(tenantId),
                             this.influxV9Utils.privateRegionPart(this.region),
                             this.influxV9Utils.dimPart(dimensions),
                             this.influxV9Utils.limitPart(limit),
                             this.influxV9Utils.offsetPart(startIndex));

    logger.debug("Metric definition query: {}", q);

    String r = this.influxV9RepoReader.read(q);

    Series series = this.objectMapper.readValue(r, Series.class);

    List<MetricDefinition> metricDefinitionList = metricDefinitionList(series,
                                                                       tenantId,
                                                                       name,
                                                                       startTime,
                                                                       endTime,
                                                                       startIndex);

    logger.debug("Found {} metric definitions matching query", metricDefinitionList.size());

    return metricDefinitionList;
  }

  @Override
  public List<MetricName> findNames(String tenantId, Map<String, String> dimensions,
                                    String offset, int limit) throws Exception {
    //
    // Use treeset to keep list in alphabetic/predictable order
    // for string based offset.
    //
    List<MetricName> metricNameList = new ArrayList<>();
    Set<String> matchingNames = new TreeSet<>();

    String q = String.format("show series "
                             + "where %1$s %2$s %3$s",
                             this.influxV9Utils.privateTenantIdPart(tenantId),
                             this.influxV9Utils.privateRegionPart(this.region),
                             this.influxV9Utils.dimPart(dimensions));

    logger.debug("Metric name query: {}", q);
    String r = this.influxV9RepoReader.read(q);
    Series series = this.objectMapper.readValue(r, Series.class);
    if (!series.isEmpty()) {
      for (Serie serie : series.getSeries()) {
        matchingNames.add(serie.getName());
      }
    }

    List<String> filteredNames = filterMetricNames(matchingNames, limit, offset);

    for (String filteredName : filteredNames) {
      MetricName dimName = new MetricName(filteredName);
      metricNameList.add(dimName);
    }
    logger.debug("Found {} metric definitions matching query", metricNameList.size());

    return metricNameList;
  }

  private List<String> filterMetricNames(Set<String> matchingNames,
                                         int limit,
                                         String offset) {
    Boolean haveOffset = !Strings.isNullOrEmpty(offset);
    List<String> filteredNames = new ArrayList<>();
    int remaining_limit = limit + 1;

    for (String dimName : matchingNames) {
      if (remaining_limit <= 0) {
        break;
      }
      if (haveOffset && dimName.compareTo(offset) <= 0) {
        continue;
      }
      filteredNames.add(dimName);
      remaining_limit--;
    }

    return filteredNames;
  }

  private List<MetricDefinition> metricDefinitionList(Series series,
                                                      String tenantId,
                                                      String name,
                                                      DateTime startTime,
                                                      DateTime endTime,
                                                      int startIndex)
  {

    List<MetricDefinition> metricDefinitionList = new ArrayList<>();

    if (!series.isEmpty()) {

      int index = startIndex;

      for (Serie serie : series.getSeries()) {

        for (String[] values : serie.getValues()) {

          MetricDefinition m = new MetricDefinition(serie.getName(),
                                                    this.influxV9Utils.getDimensions(values, serie.getColumns()));
          //
          // If start/end time are specified, ensure we've got measurements
          // for this definition before we add to the return list
          //
          if (hasMeasurements(m, tenantId, startTime, endTime)) {
            m.setId(String.valueOf(index++));
            metricDefinitionList.add(m);
          }
        }
      }
    }

    return metricDefinitionList;
  }

  private List<MetricName> metricNameList(Series series) {
    List<MetricName> metricNameList = new ArrayList<>();

    if (!series.isEmpty()) {

      Serie serie = series.getSeries()[0];

      for (String[] values : serie.getValues()) {
        MetricName m = new MetricName(values[0]);
        metricNameList.add(m);
      }

    }

    return metricNameList;
  }

  private boolean hasMeasurements(MetricDefinition m,
                                  String tenantId,
                                  DateTime startTime,
                                  DateTime endTime)
  {
    boolean hasMeasurements = true;

    //
    // Only make the additional query if startTime has been
    // specified.
    //
    if (startTime == null) {
      return hasMeasurements;
    }

    try {

      String q = buildMeasurementsQuery(tenantId,
                                        m.name,
                                        m.dimensions,
                                        startTime,
                                        endTime);

      String r = this.influxV9RepoReader.read(q);
      Series series = this.objectMapper.readValue(r, Series.class);
      hasMeasurements = !series.isEmpty();

    } catch (Exception e) {
      //
      // If something goes wrong with the measurements query
      // checking if there are current measurements, default to
      // existing behavior and return the definition.
      //
      logger.error("Failed to query for measurements for: {}", m.name, e);
      hasMeasurements = true;
    }

    return hasMeasurements;
  }

  private String buildMeasurementsQuery(String tenantId,
                                       String name,
                                       Map<String, String> dimensions,
                                       DateTime startTime,
                                       DateTime endTime) throws Exception
  {

    String q = String.format("select value, value_meta %1$s "
                        + "where %2$s %3$s %4$s %5$s %6$s group by * slimit 1",
                        this.influxV9Utils.namePart(name, true),
                        this.influxV9Utils.privateTenantIdPart(tenantId),
                        this.influxV9Utils.privateRegionPart(this.region),
                        this.influxV9Utils.startTimePart(startTime),
                        this.influxV9Utils.dimPart(dimensions),
                        this.influxV9Utils.endTimePart(endTime));

    logger.debug("Measurements query: {}", q);

    return q;
  }

}
