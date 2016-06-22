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
import java.util.HashMap;
import java.util.Iterator;
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

  private static final String FIND_BY_METRIC_DEF_SQL =
      "SELECT %s to_hex(mes.definition_dimensions_id) as def_dims_id, "
      + "mes.time_stamp, mes.value, mes.value_meta "
      + "FROM MonMetrics.Measurements mes "
      + "WHERE to_hex(mes.definition_dimensions_id) %s " // Sub select query
      + "%s " // endtime and offset here
      + "AND mes.time_stamp >= :startTime "
      + "ORDER BY %s" // sort by id if not merging
      + "mes.time_stamp ASC "
      + "LIMIT :limit";

  private static final String
      DEFDIM_IDS_SELECT =
      "SELECT %s defDims.id "
      + "FROM MonMetrics.DefinitionDimensions defDims "
      + "WHERE defDims.id IN (%s)";

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

      Map<String, Measurements> results = findDefIds(h, tenantId, name, dimensions);

      Set<String> defDimsIdSet = results.keySet();

      if (!"*".equals(groupBy) && !Boolean.TRUE.equals(mergeMetricsFlag) && (defDimsIdSet.size() > 1)) {
        throw new MultipleMetricsException(name, dimensions);
      }

      //
      // If we didn't find any definition dimension ids,
      // we won't have any measurements, let's just bail
      // now.
      //
      if (defDimsIdSet.size() == 0) {

        return new ArrayList<>(results.values());

      }

      String defDimInClause = MetricQueries.createDefDimIdInClause(defDimsIdSet);
 
      StringBuilder sb = new StringBuilder();

      if (endTime != null) {

        sb.append(" and mes.time_stamp <= :endTime");

      }

      if (offset != null && !offset.isEmpty()) {

        if (Boolean.TRUE.equals(mergeMetricsFlag)) {

          sb.append(" and mes.time_stamp > :offset_timestamp ");

        } else {

          sb.append(" and (TO_HEX(mes.definition_dimensions_id) > :offset_id "
                    + "or (TO_HEX(mes.definition_dimensions_id) = :offset_id and mes.time_stamp > :offset_timestamp)) ");

        }

      }

      String orderById = "";
      if (Boolean.FALSE.equals(mergeMetricsFlag)) {

        orderById = "mes.definition_dimensions_id,";

      }

      String sql = String.format(FIND_BY_METRIC_DEF_SQL, this.dbHint, defDimInClause, sb, orderById);

      Query<Map<String, Object>> query = h.createQuery(sql)
              .bind("startTime", new Timestamp(startTime.getMillis()))
              .bind("limit", limit + 1);

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

        for (Map<String, Object> row : rows) {

          String defDimsId = (String) row.get("def_dims_id");

          Object[] measurement = parseRow(row);

          results.get(defDimsId).addMeasurement(measurement);

        }

      } else {

        String firstDefDimsId = (String) rows.get(0).get("def_dims_id");

        Measurements firstMeasurement = results.get(firstDefDimsId);

        // clear dimensions
        firstMeasurement.setDimensions(new HashMap<String, String>());

        results.clear();

        results.put(firstDefDimsId, firstMeasurement);

        for (Map<String, Object> row : rows) {

          Object[] measurement = parseRow(row);

          results.get(firstDefDimsId).addMeasurement(measurement);

        }

      }

      // clean up any empty measurements
      Iterator<Map.Entry<String, Measurements>> it = results.entrySet().iterator();
      while (it.hasNext())
      {
        Map.Entry<String, Measurements> entry = it.next();
        if (entry.getValue().getMeasurements().size() == 0) {
          it.remove();
        }
      }

      return new ArrayList<>(results.values());
    }
  }

  private Object[] parseRow(Map<String, Object> row) {

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

    return new Object[]{timestamp, value, valueMetaMap};

  }

  private Map<String, Measurements> findDefIds(Handle h, String tenantId,
                                              String name, Map<String, String> dimensions) {

    String defDimSql = String.format(
        MetricQueries.FIND_METRIC_DEFS_SQL,
        this.dbHint,
        MetricQueries.buildMetricDefinitionSubSql(name, dimensions));

    Query<Map<String, Object>> query = h.createQuery(defDimSql).bind("tenantId", tenantId);

    MetricQueries.bindDimensionsToQuery(query, dimensions);

    if (name != null && !name.isEmpty()) {
      query.bind("name", name);
    }

    List<Map<String, Object>> rows = query.list();

    Map<String, Measurements> stringIdMap = new HashMap<>();

    String currentDefDimId = null;

    Map<String, String> dims = null;

    for (Map<String, Object> row : rows) {

      String defDimId = (String) row.get("defDimsId");

      String defName = (String) row.get("name");

      String dimName = (String) row.get("dName");

      String dimValue = (String) row.get("dValue");

      if (defDimId == null || !defDimId.equals(currentDefDimId)) {

        currentDefDimId = defDimId;

        dims = new HashMap<>();

        if (dimName != null && dimValue != null)
          dims.put(dimName, dimValue);

        Measurements measurements = new Measurements();

        measurements.setId(defDimId);

        measurements.setName(defName);

        measurements.setDimensions(dims);

        stringIdMap.put(currentDefDimId, measurements);

      } else {

        if (dimName != null && dimValue != null)
          dims.put(dimName, dimValue);

      }

    }

    return stringIdMap;
  }
}
