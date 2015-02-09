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

import monasca.api.MonApiConfiguration;
import monasca.api.domain.model.measurement.MeasurementRepository;
import monasca.api.domain.model.measurement.Measurements;

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

import static monasca.api.infrastructure.persistence.influxdb.Utils.buildSerieNameRegex;

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
  public List<Measurements> find(String tenantId, String name,
                                       Map<String, String> dimensions, DateTime startTime,
                                       @Nullable DateTime endTime, @Nullable String offset)
      throws Exception {

    String serieNameRegex = buildSerieNameRegex(tenantId, config.region, name, dimensions);

    String timePart = Utils.WhereClauseBuilder.buildTimePart(startTime, endTime);

    String offsetPart = Utils.buildOffsetPart(offset);

    String query =
        String.format("select value " + "from /%1$s/ where 1 = 1 " + " %2$s  %3$s",
                      serieNameRegex, timePart, offsetPart);
    logger.debug("Query string: {}", query);

    List<Serie> result = null;
    try {
      result = this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.MILLISECONDS);
    } catch (RuntimeException e) {
      if (e.getMessage().startsWith(Utils.COULD_NOT_LOOK_UP_COLUMNS_EXC_MSG)) {
        return new LinkedList<>();
      } else {
        logger.error("Failed to get data from InfluxDB", e);
        throw e;
      }
    }

    return buildMeasurementList(result);
  }

  private List<Measurements> buildMeasurementList(List<Serie> result) throws Exception {
    List<Measurements> measurementsList = new LinkedList<>();

    for (Serie serie : result) {

      Utils.SerieNameDecoder serieNameDecoder;
      try {
        serieNameDecoder = new Utils.SerieNameDecoder(serie.getName());
      } catch (Utils.SerieNameDecodeException e) {
        logger.warn("Dropping series name that is not decodable: {}", serie.getName(), e);
        continue;
      }

      Measurements measurements = new Measurements();
      measurements.setName(serieNameDecoder.getMetricName());
      measurements.setDimensions(serieNameDecoder.getDimensions());

      List<Object[]> valObjArryList = new LinkedList<>();
      for (Map<String, Object> row : serie.getRows()) {

        Object[] objArry = new Object[3];

        // sequence_number
        objArry[0] = ((Double) row.get(serie.getColumns()[1])).longValue();
        // time
        Double timeDouble = (Double) row.get(serie.getColumns()[0]);
        // last id wins. ids should be in descending order.
        measurements.setId(String.valueOf(timeDouble.longValue()));
        objArry[1] = DATETIME_FORMATTER.print(timeDouble.longValue());
        // value
        objArry[2] = (Double) row.get(serie.getColumns()[2]);

        valObjArryList.add(objArry);
      }
      measurements.setMeasurements(valObjArryList);
      measurementsList.add(measurements);
    }

    return measurementsList;
  }

}
