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
import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;
import com.hpcloud.mon.domain.model.measurement.Measurements;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import static com.hpcloud.mon.infrastructure.persistence.influxdb.Utils.buildSerieNameRegex;
import static com.hpcloud.mon.infrastructure.persistence.influxdb.Utils.isSerieMetricName;

public class MeasurementInfluxDbRepositoryImpl implements MeasurementRepository {

  private static final Logger logger = LoggerFactory
      .getLogger(MeasurementInfluxDbRepositoryImpl.class);

  private final MonApiConfiguration config;
  private final InfluxDB influxDB;

  public static final DateTimeFormatter DATETIME_FORMATTER = ISODateTimeFormat.dateTimeNoMillis()
      .withZoneUTC();

  @Inject
  public MeasurementInfluxDbRepositoryImpl(MonApiConfiguration config, InfluxDB influxDB) {
    this.config = config;

    this.influxDB = influxDB;
  }

  @Override
  public Collection<Measurements> find(String tenantId, String name,
                                       Map<String, String> dimensions, DateTime startTime,
                                       @Nullable DateTime endTime)
      throws Exception {

    String serieNameRegex = buildSerieNameRegex(tenantId, name, dimensions);

    String timePart = Utils.WhereClauseBuilder.buildTimePart(startTime, endTime);

    String query =
        String.format("select value " + "from /%1$s/ where 1 = 1 " + " %2$s ",
                      serieNameRegex, timePart);
    logger.debug("Query string: {}", query);

    List<Measurements> measurementsList = new LinkedList<>();

    List<Serie> result = null;
    try {
      result = this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.MILLISECONDS);
    } catch (RuntimeException e) {
      if (e.getMessage().equals("Couldn't look up columns")) {
        return measurementsList;
      } else {
        logger.error("Failed to get data from InfluxDB", e);
        throw e;
      }
    }

    for (Serie serie : result) {

      String serieName = serie.getName();
      if (!isSerieMetricName(serieName)) {
        logger.warn("Dropping series name that is not well-formed: {}", serieName);
        continue;
      }

      Utils.SerieNameConverter serieNameConverter = new Utils.SerieNameConverter(serieName);
      Measurements measurements = new Measurements();
      measurements.setName(serieNameConverter.getMetricName());
      measurements.setDimensions(serieNameConverter.getDimensions());
      List<Object[]> valObjArryList = new LinkedList<>();
      final String[] colNames = serie.getColumns();
      final List<Map<String, Object>> rows = serie.getRows();
      for (Map<String, Object> row : rows) {

        Object[] objArry = new Object[3];

        // sequence_number
        objArry[0] = ((Double) row.get(colNames[1])).longValue();
        // time
        Double timeDouble = (Double) row.get(colNames[0]);
        objArry[1] = DATETIME_FORMATTER.print(timeDouble.longValue());
        // value
        objArry[2] = (Double) row.get(colNames[2]);

        valObjArryList.add(objArry);
      }
      measurements.setMeasurements(valObjArryList);
      measurementsList.add(measurements);
    }
    return measurementsList;
  }

}
