/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpcloud.mon.infrastructure.persistence;

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

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MeasurementInfluxDBRepositoryImpl implements MeasurementRepository {

  private static final Logger logger = LoggerFactory.getLogger(MeasurementInfluxDBRepositoryImpl
      .class);

  private final MonApiConfiguration config;
  private final InfluxDB influxDB;

  public static final DateTimeFormatter DATETIME_FORMATTER = ISODateTimeFormat.dateTimeNoMillis();

  @Inject
  public MeasurementInfluxDBRepositoryImpl(MonApiConfiguration config, InfluxDB influxDB) {
    this.config = config;

    this.influxDB = influxDB;
  }

  @Override
  public Collection<Measurements> find(String tenantId, String name, Map<String,
      String> dimensions, DateTime startTime, @Nullable DateTime endTime) throws Exception {

    String dimsPart = Utils.WhereClauseBuilder.buildDimsPart(dimensions);
    String timePart = Utils.WhereClauseBuilder.buildTimePart(startTime, endTime);
    String query = String.format("select value " +
            "from %1$s " +
            "where tenant_id = '%2$s' %3$s %4$s", Utils.SQLSanitizer.sanitize(name),
        Utils.SQLSanitizer.sanitize(tenantId), timePart, dimsPart);

    logger.debug("Query string: {}", query);

    List<Serie> result = this.influxDB.Query(this.config.influxDB.getName(), query,
        TimeUnit.MILLISECONDS);

    Measurements measurements = new Measurements();
    measurements.setName(name);
    measurements.setDimensions(dimensions);
    List<Object[]> valObjArryList = new LinkedList<>();
    for (Serie serie : result) {
      Object[][] valObjArry = serie.getPoints();
      for (int i = 0; i < valObjArry.length; i++) {

        Object[] objArry = new Object[3];

        // sequence_number
        objArry[0] = valObjArry[i][1];
        // time
        Double timeDouble = (Double) valObjArry[i][0];
        objArry[1] = DATETIME_FORMATTER.print(timeDouble.longValue());
        // value
        objArry[2] = valObjArry[i][2];

        valObjArryList.add(objArry);
      }
    }

    measurements.setMeasurements(valObjArryList);

    return Arrays.asList(measurements);
  }

}
