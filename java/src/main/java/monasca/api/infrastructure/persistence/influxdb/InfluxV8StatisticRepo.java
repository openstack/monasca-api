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

import monasca.api.ApiConfig;
import monasca.api.domain.model.statistic.StatisticRepo;
import monasca.api.domain.model.statistic.Statistics;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import static monasca.api.infrastructure.persistence.influxdb.InfluxV8Utils.buildSerieNameRegex;

public class InfluxV8StatisticRepo implements StatisticRepo {

  private static final Logger logger = LoggerFactory
      .getLogger(InfluxV8StatisticRepo.class);

  private final ApiConfig config;
  private final InfluxDB influxDB;

  public static final DateTimeFormatter DATETIME_FORMATTER = ISODateTimeFormat.dateTimeNoMillis()
      .withZoneUTC();

  @Inject
  public InfluxV8StatisticRepo(ApiConfig config, InfluxDB influxDB) {
    this.config = config;
    this.influxDB = influxDB;
  }

  @Override
  public List<Statistics> find(String tenantId, String name, Map<String, String> dimensions,
                               DateTime startTime, @Nullable DateTime endTime,
                               List<String> statistics, int period, String offset, int limit,
                               Boolean mergeMetricsFlag)
      throws Exception {

    // mergeMetricsFlag is not implemented for Influxdb V8.

    // Limit is not implemented for Influxdb V8.

    String serieNameRegex = buildSerieNameRegex(tenantId, config.region, name, dimensions);
    String statsPart = buildStatsPart(statistics);
    String timePart = InfluxV8Utils.WhereClauseBuilder.buildTimePart(startTime, endTime);
    String periodPart = buildPeriodPart(period);

    String query =
        String.format("select time %1$s from /%2$s/ where 1=1 %3$s %4$s",
                      statsPart, serieNameRegex, timePart, periodPart);
    logger.debug("Query string: {}", query);

    List<Serie> result = null;
    try {
      result = this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.MILLISECONDS);
    } catch (RuntimeException e) {
      if (e.getMessage().startsWith(InfluxV8Utils.COULD_NOT_LOOK_UP_COLUMNS_EXC_MSG)) {
        return new LinkedList<>();
      } else {
        logger.error("Failed to get data from InfluxDB", e);
        throw e;
      }
    }

    return buildStatisticsList(statistics, result);
  }

  private List<Statistics> buildStatisticsList(List<String> statistics, List<Serie> result)
      throws Exception {
    List<Statistics> statisticsList = new LinkedList<>();
    for (Serie serie : result) {

      InfluxV8Utils.SerieNameDecoder serieNameDecoder;
      try {
        serieNameDecoder = new InfluxV8Utils.SerieNameDecoder(serie.getName());
      } catch (InfluxV8Utils.SerieNameDecodeException e) {
        logger.warn("Dropping series name that is not decodable: {}", serie.getName(), e);
        continue;
      }

      Statistics statistic = new Statistics();
      statistic.setName(serieNameDecoder.getMetricName());
      List<String> colNamesList = new LinkedList<>(statistics);
      colNamesList.add(0, "timestamp");
      statistic.setColumns(colNamesList);
      statistic.setDimensions(serieNameDecoder.getDimensions());
      List<List<Object>> valObjArryArry = new LinkedList<>();
      statistic.setStatistics(valObjArryArry);
      for (Map<String, Object> row : serie.getRows()) {
        List<Object> valObjArry = new ArrayList<>();
        // First column is always time.
        Double timeDouble = (Double) row.get(serie.getColumns()[0]);
        valObjArry.add(DATETIME_FORMATTER.print(timeDouble.longValue()));
        for (int j = 1; j < statistics.size() + 1; j++) {
          valObjArry.add(row.get(serie.getColumns()[j]));
        }
        valObjArryArry.add(valObjArry);
      }
      statisticsList.add(statistic);
    }
    return statisticsList;
  }

  private String buildPeriodPart(int period) {
    String s = "";
    if (period >= 1) {
      s += String.format("group by time(%1$ds)", period);
    }

    return s;
  }

  private String buildStatsPart(List<String> statistics) {
    String s = "";

    for (String statistic : statistics) {
      s += ",";
      if (statistic.trim().toLowerCase().equals("avg")) {
        s += " mean(value)";
      } else {
        s += " " + statistic + "(value)";
      }
    }

    return s;
  }
}
