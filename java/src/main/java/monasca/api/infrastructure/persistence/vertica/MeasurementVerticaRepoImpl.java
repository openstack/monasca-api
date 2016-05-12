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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  public static final ByteBuffer EMPTY_DEF_ID = ByteBuffer.wrap(new byte[0]);

  private static final String FIND_BY_METRIC_DEF_SQL =
      "select mes.definition_dimensions_id, "
      + "mes.time_stamp, mes.value, mes.value_meta "
      + "from MonMetrics.Measurements mes "
      + "where to_hex(mes.definition_dimensions_id) "
      + "%s " // defdim IN clause here
      + "%s " // endtime and offset here
      + "and mes.time_stamp >= :startTime "
      + "order by mes.time_stamp ASC "
      + "limit :limit";

  private static final String
      DEFDIM_IDS_SELECT =
      "SELECT defDims.id "
      + "FROM MonMetrics.DefinitionDimensions defDims "
      + "WHERE defDims.id IN (%s)";

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

      Map<ByteBuffer, Measurements> results = new LinkedHashMap<>();

      Set<byte[]> defDimIdSet = new HashSet<>();

      String defDimSql = String.format(
          DEFDIM_IDS_SELECT,
          MetricQueries.buildMetricDefinitionSubSql(name, dimensions));

      Query<Map<String, Object>> query = h.createQuery(defDimSql).bind("tenantId", tenantId);

      if (name != null && !name.isEmpty()) {
        query.bind("name", name);
      }

      MetricQueries.bindDimensionsToQuery(query, dimensions);

      List<Map<String, Object>> rows = query.list();

      for (Map<String, Object> row : rows) {

        byte[] defDimId = (byte[]) row.get("id");
        defDimIdSet.add(defDimId);

      }

      if (!Boolean.TRUE.equals(mergeMetricsFlag) && (defDimIdSet.size() > 1)) {
        throw new MultipleMetricsException(name, dimensions);
      }

      //
      // If we didn't find any definition dimension ids,
      // we won't have any measurements, let's just bail
      // now.
      //
      if (defDimIdSet.size() == 0) {
        return new ArrayList<>(results.values());
      }

      String defDimInClause = MetricQueries.createDefDimIdInClause(defDimIdSet);
 
      StringBuilder sb = new StringBuilder();

      if (endTime != null) {
        sb.append(" and time_stamp <= :endTime");
      }

      if (offset != null && !offset.isEmpty()) {
        sb.append(" and time_stamp > :offset");
      }

      String sql = String.format(FIND_BY_METRIC_DEF_SQL, defDimInClause, sb);

      query = h.createQuery(sql)
              .bind("startTime", new Timestamp(startTime.getMillis()))
              .bind("limit", limit + 1);

      if (endTime != null) {
        logger.debug("binding endtime: {}", endTime);
        query.bind("endTime", new Timestamp(endTime.getMillis()));
      }

      if (offset != null && !offset.isEmpty()) {
        logger.debug("binding offset: {}", offset);
        query.bind("offset", new Timestamp(DateTime.parse(offset).getMillis()));
      }

      rows = query.list();

      for (Map<String, Object> row : rows) {

        String timestamp = DATETIME_FORMATTER.print(((Timestamp) row.get("time_stamp")).getTime());

        byte[] defdimsIdBytes = (byte[]) row.get("definition_dimensions_id");
        ByteBuffer defdimsId = ByteBuffer.wrap(defdimsIdBytes);

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

        Measurements measurements = (Boolean.TRUE.equals(mergeMetricsFlag)) ? results.get(EMPTY_DEF_ID) : results.get(defdimsId);

        if (measurements == null) {
          if (Boolean.TRUE.equals(mergeMetricsFlag)) {
            measurements =
                new Measurements(name, new HashMap<String, String>(),
                                 new ArrayList<Object[]>());

            results.put(EMPTY_DEF_ID, measurements);
          } else {
            measurements =
                new Measurements(name, MetricQueries.dimensionsFor(h, (byte[]) defDimIdSet.toArray()[0]),
                                 new ArrayList<Object[]>());
            results.put(defdimsId, measurements);
          }
        }

        measurements.addMeasurement(new Object[] {timestamp, value, valueMetaMap});
      }

      return new ArrayList<>(results.values());
    }
  }
}
