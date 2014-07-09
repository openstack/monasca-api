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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.domain.model.statistic.StatisticRepository;
import com.hpcloud.mon.domain.model.statistic.Statistics;

public class StatisticInfluxDbRepositoryImpl implements StatisticRepository {
  private static final Logger logger = LoggerFactory
      .getLogger(StatisticInfluxDbRepositoryImpl.class);

  private final MonApiConfiguration config;
  private final InfluxDB influxDB;

  public static final DateTimeFormatter DATETIME_FORMATTER = ISODateTimeFormat.dateTimeNoMillis()
      .withZoneUTC();

  @Inject
  public StatisticInfluxDbRepositoryImpl(MonApiConfiguration config, InfluxDB influxDB) {
    this.config = config;
    this.influxDB = influxDB;
  }

  @Override
  public List<Statistics> find(String tenantId, String name, Map<String, String> dimensions,
      DateTime startTime, @Nullable DateTime endTime, List<String> statistics, int period)
      throws Exception {
    String statsPart = buildStatsPart(statistics);
    String timePart = Utils.WhereClauseBuilder.buildTimePart(startTime, endTime);
    String dimsPart = Utils.WhereClauseBuilder.buildDimsPart(dimensions);
    String periodPart = buildPeriodPart(period);

    String query =
        String.format("select time %1$s from %2$s where tenant_id = '%3$s' %4$s %5$s " + "%6$s",
            statsPart, Utils.SQLSanitizer.sanitize(name), Utils.SQLSanitizer.sanitize(tenantId),
            timePart, dimsPart, periodPart);

    logger.debug("Query string: {}", query);

    List<Serie> result =
        this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.MILLISECONDS);

    List<Statistics> statisticsList = new LinkedList<Statistics>();

    // Should only be one serie -- name.
    for (Serie serie : result) {
      Statistics stat = new Statistics();
      stat.setName(serie.getName());
      List<String> colNamesList = new LinkedList<>(statistics);
      colNamesList.add(0, "timestamp");
      stat.setColumns(colNamesList);
      stat.setDimensions(dimensions == null ? new HashMap<String, String>() : dimensions);
      List<List<Object>> valObjArryArry = new LinkedList<List<Object>>();
      stat.setStatistics(valObjArryArry);
      Object[][] pointsArryArry = serie.getPoints();
      for (int i = 0; i < pointsArryArry.length; i++) {
        List<Object> valObjArry = new ArrayList<>();
        // First column is always time.
        Double timeDouble = (Double) pointsArryArry[i][0];
        valObjArry.add(DATETIME_FORMATTER.print(timeDouble.longValue()));
        for (int j = 1; j < statistics.size() + 1; j++) {
          valObjArry.add(pointsArryArry[i][j]);
        }
        valObjArryArry.add(valObjArry);
      }
      statisticsList.add(stat);
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
