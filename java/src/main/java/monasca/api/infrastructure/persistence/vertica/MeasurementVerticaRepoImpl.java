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
package monasca.api.infrastructure.persistence.vertica;

import monasca.api.domain.exception.MultipleMetricsException;
import monasca.api.domain.model.measurement.MeasurementRepo;
import monasca.api.domain.model.measurement.Measurements;
import monasca.api.infrastructure.persistence.DimensionQueries;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeasurementVerticaRepoImpl implements MeasurementRepo {

  private static final Logger logger = LoggerFactory
      .getLogger(MeasurementVerticaRepoImpl.class);

  public static final DateTimeFormatter DATETIME_FORMATTER =
      ISODateTimeFormat.dateTime().withZoneUTC();

  private static final String FIND_BY_METRIC_DEF_SQL =
      "select def.name, mes.definition_dimensions_id, defdims.dimension_set_id, defdims.definition_id, "
      + "mes.time_stamp, mes.value, mes.value_meta "
      + "from MonMetrics.Measurements mes, MonMetrics.Definitions def, MonMetrics.DefinitionDimensions defdims "
      + "where mes.definition_dimensions_id = defdims.id "
      + "and def.id = defdims.definition_id "
      + "and def.tenant_id = :tenantId "
      + "and mes.time_stamp >= :startTime "
      + "%s " // metric name here
      + "%s " // dimension and clause here
      + "order by mes.time_stamp ASC "
      + "limit :limit";

  private static final String TABLE_TO_JOIN_DIMENSIONS_ON = "defDims";

  private final DBI db;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final static TypeReference VALUE_META_TYPE = new TypeReference<Map<String, String>>() {};

  @Inject
  public MeasurementVerticaRepoImpl(
      @Named("vertica") DBI db) {

    this.db = db;
  }

  @Override
  public List<Measurements> find(
      String tenantId,
      String name,
      Map<String, String> dimensions,
      DateTime startTime,
      @Nullable DateTime endTime,
      @Nullable String offset,
      int limit,
      Boolean mergeMetricsFlag) throws MultipleMetricsException {

    try (Handle h = db.open()) {

      StringBuilder sb = new StringBuilder();

      if (name != null && !name.isEmpty()) {

        sb.append(" and def.name = :name");

      }

      if (endTime != null) {

        sb.append(" and mes.time_stamp <= :endTime");

      }

      if (offset != null && !offset.isEmpty()) {

        sb.append(" and time_stamp > :offset");

      }

      String sql =
          String.format(FIND_BY_METRIC_DEF_SQL,
              sb,
              MetricQueries.buildDimensionAndClause(dimensions, TABLE_TO_JOIN_DIMENSIONS_ON));

      Query<Map<String, Object>> query =
          h.createQuery(sql)
              .bind("tenantId", tenantId)
              .bind("startTime", new Timestamp(startTime.getMillis()))
              .bind("limit", limit + 1);

      if (name != null && !name.isEmpty()) {

        logger.debug("binding name: {}", name);

        query.bind("name", name);

      }

      if (endTime != null) {

        logger.debug("binding endtime: {}", endTime);

        query.bind("endTime", new Timestamp(endTime.getMillis()));

      }

      if (offset != null && !offset.isEmpty()) {

        logger.debug("binding offset: {}", offset);

        query.bind("offset", new Timestamp(DateTime.parse(offset).getMillis()));

      }

      DimensionQueries.bindDimensionsToQuery(query, dimensions);

      List<Map<String, Object>> rows = query.list();

      Map<ByteBuffer, Measurements> results = new LinkedHashMap<>();

      for (Map<String, Object> row : rows) {

        String metricName = (String) row.get("name");

        byte[] defIdBytes = (byte[]) row.get("definition_id");

        ByteBuffer defId = ByteBuffer.wrap(defIdBytes);

        byte[] defdimsIdBytes = (byte[]) row.get("definition_dimensions_id");

        byte[] dimSetIdBytes = (byte[]) row.get("dimension_set_id");

        ByteBuffer defdimsId = ByteBuffer.wrap(defdimsIdBytes);

        String timestamp = DATETIME_FORMATTER.print(((Timestamp) row.get("time_stamp")).getTime());

        double value = (double) row.get("value");

        String valueMetaString = (String) row.get("value_meta");

        Map<String, String> valueMetaMap = new HashMap<>();

        if (valueMetaString != null && !valueMetaString.isEmpty()) {

          try {

            valueMetaMap = this.objectMapper.readValue(valueMetaString, VALUE_META_TYPE);

          } catch (IOException e) {

            logger.error("failed to parse value metadata: {}", valueMetaString);
          }

        }

        Measurements measurements;

        if (Boolean.TRUE.equals(mergeMetricsFlag)) {

          measurements = results.get(defId);

        } else {

          measurements = results.get(defdimsId);

        }

        if (measurements == null) {

          if (Boolean.TRUE.equals(mergeMetricsFlag)) {

            measurements =
                new Measurements(metricName, new HashMap<String, String>(),
                                 new ArrayList<Object[]>());

            results.put(defId, measurements);

          } else {

            measurements =
                new Measurements(metricName, MetricQueries.dimensionsFor(h, dimSetIdBytes),
                                 new ArrayList<Object[]>());

            results.put(defdimsId, measurements);

            if (results.keySet().size() > 1) {

              throw new MultipleMetricsException(name, dimensions);

            }
          }
        }

        measurements.addMeasurement(new Object[] {timestamp, value, valueMetaMap});
      }

      return new ArrayList<>(results.values());
    }
  }
}
