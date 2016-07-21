/* Copyright (c) 2014, 2016 Hewlett-Packard Development Company, L.P.
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
import monasca.api.ApiConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
      "SELECT %s to_hex(mes.definition_dimensions_id) as def_dims_id, "
      + "mes.time_stamp, mes.value, mes.value_meta "
      + "FROM MonMetrics.Measurements mes "
      + "WHERE mes.time_stamp >= :startTime "
      + "%s " // endtime and offset here
      + "AND TO_HEX(definition_dimensions_id) IN (%s) " // id subquery here
      + "ORDER BY %s" // sort by id if not merging
      + "mes.time_stamp ASC "
      + "LIMIT :limit";

  private final DBI db;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final static TypeReference VALUE_META_TYPE = new TypeReference<Map<String, String>>() {};

  private final String dbHint;

  @Inject
  public MeasurementVerticaRepoImpl(
      @Named("vertica") DBI db, ApiConfig config)
  {
    this.db = db;
    this.dbHint = config.vertica.dbHint;
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
      Boolean mergeMetricsFlag,
      String groupBy) throws MultipleMetricsException {

    try (Handle h = db.open()) {

      Map<String, Measurements> results = new HashMap<>();

      if (!"*".equals(groupBy) && !Boolean.TRUE.equals(mergeMetricsFlag)) {
        MetricQueries.checkForMultipleDefinitions(h, tenantId, name, dimensions);
      }
 
      StringBuilder sb = new StringBuilder();

      if (endTime != null) {

        sb.append(" and mes.time_stamp <= :endTime");

      }

      if (offset != null && !offset.isEmpty()) {

        if ("*".equals(groupBy)) {

          sb.append(" and (TO_HEX(mes.definition_dimensions_id) > :offset_id "
                    + "or (TO_HEX(mes.definition_dimensions_id) = :offset_id and mes.time_stamp > :offset_timestamp)) ");

        } else {

          sb.append(" and mes.time_stamp > :offset_timestamp ");

        }

      }

      String orderById = "";
      if (Boolean.FALSE.equals(mergeMetricsFlag)) {

        orderById = "mes.definition_dimensions_id,";

      }


      String sql = String.format(FIND_BY_METRIC_DEF_SQL,
                                 this.dbHint,
                                 sb,
                                 MetricQueries.buildMetricDefinitionSubSql(name, dimensions,
                                                                           null, null),
                                 orderById);

      Query<Map<String, Object>> query = h.createQuery(sql)
              .bind("tenantId", tenantId)
              .bind("startTime", new Timestamp(startTime.getMillis()))
              .bind("limit", limit + 1);

      if (name != null && !name.isEmpty()) {
        query.bind("name", name);
      }

      MetricQueries.bindDimensionsToQuery(query, dimensions);

      if (endTime != null) {
        logger.debug("binding endtime: {}", endTime);

        query.bind("endTime", new Timestamp(endTime.getMillis()));

      }

      if (offset != null && !offset.isEmpty()) {
        logger.debug("binding offset: {}", offset);

        MetricQueries.bindOffsetToQuery(query, offset);

      }

      List<Map<String, Object>> rows = query.list();

      if (rows.size() == 0) {
        return new ArrayList<>();
      }

      if ("*".equals(groupBy)) {

        String currentDefId = null;

        for (Map<String, Object> row : rows) {

          String defDimsId = (String) row.get("def_dims_id");

          if (defDimsId != null && !defDimsId.equals(currentDefId)) {
            currentDefId = defDimsId;
            results.put(defDimsId, new Measurements());
          }

          List<Object> measurement = parseRow(row);

          results.get(defDimsId).addMeasurement(measurement);

        }

        MetricQueries.addDefsToResults(results, h, this.dbHint);

      } else {

        Measurements firstMeasurement = new Measurements();

        firstMeasurement.setName(name);

        String firstDefDimsId = (String) rows.get(0).get("def_dims_id");

        for (Map<String, Object> row : rows) {

          List<Object> measurement = parseRow(row);

          firstMeasurement.addMeasurement(measurement);

        }

        results.put(firstDefDimsId, firstMeasurement);

        if (!Boolean.TRUE.equals(mergeMetricsFlag)) {
          firstMeasurement.setId(firstDefDimsId);
          MetricQueries.addDefsToResults(results, h, this.dbHint);
        } else {
          if (dimensions == null) {
            dimensions = new HashMap<>();
          }
          firstMeasurement.setDimensions(dimensions);
        }

      }

      return new ArrayList<>(results.values());
    }
  }

  private List<Object> parseRow(Map<String, Object> row) {

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

    return Arrays.asList(timestamp, value, valueMetaMap);
  }
}
