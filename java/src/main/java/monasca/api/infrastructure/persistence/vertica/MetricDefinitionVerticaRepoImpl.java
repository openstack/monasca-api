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

import monasca.api.domain.model.metric.MetricDefinitionRepo;
import monasca.api.domain.model.metric.MetricName;
import monasca.api.infrastructure.persistence.DimensionQueries;
import monasca.api.resource.exception.Exceptions;
import monasca.common.model.metric.MetricDefinition;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

public class MetricDefinitionVerticaRepoImpl implements MetricDefinitionRepo {

  private static final Logger
      logger =
      LoggerFactory.getLogger(MetricDefinitionVerticaRepoImpl.class);

  private static final String
      FIND_METRIC_DEFS_SQL =
      "SELECT defDims.id as defDimsId, def.name, dims.name as dName, dims.value AS dValue "
      + "FROM MonMetrics.Definitions def, MonMetrics.DefinitionDimensions defDims "
      // Outer join needed in case there are no dimensions for a definition.
      + "LEFT OUTER JOIN MonMetrics.Dimensions dims ON dims.dimension_set_id = defDims"
      + ".dimension_set_id WHERE def.id = defDims.definition_id "
      + "and def.tenant_id = :tenantId "
      + "%s " // Name goes here.
      + "%s " // Offset goes here.
      + "%s " // Dimensions and clause goes here
      + "%s " // Optional timestamp qualifier goes here
      + "ORDER BY defDims.id ASC %s"; // Limit goes here.

  private static final String
      FIND_METRIC_NAMES_SQL =
      "SELECT distinct def.id, def.name "
      + "FROM MonMetrics.Definitions def "
      + "WHERE def.id IN (%s) " // Subselect goes here
      + "ORDER BY def.id ASC ";

  private static final String
      METRIC_NAMES_SUB_SELECT =
      "SELECT defSub.id "
      + "FROM  MonMetrics.Definitions defSub, MonMetrics.DefinitionDimensions defDimsSub "
      + "WHERE defDimsSub.definition_id = defSub.id "
      + "AND defSub.tenant_id = :tenantId "
      + "%s " // Offset goes here.
      + "%s " // Dimensions and clause goes here
      + "ORDER BY defSub.id ASC %s"; // Limit goes here.

  private static final String
      DEFDIM_IDS_SELECT =
      "SELECT defDims.id "
      + "FROM MonMetrics.Definitions def, MonMetrics.DefinitionDimensions defDims "
      + "WHERE defDims.definition_id = def.id "
      + "AND def.tenant_id = :tenantId "
      + "%s "  // Name and clause here
      + "%s;"; // Dimensions and clause goes here

  private static final String
      MEASUREMENT_AND_CLAUSE =
      "AND defDims.id IN ("
      + "SELECT definition_dimensions_id FROM "
      + "MonMetrics.Measurements "
      + "WHERE to_hex(definition_dimensions_id) "
      + "%s "    // List of definition dimension ids here
      + "%s ) "; // start or start and end time here

  private static final String TABLE_TO_JOIN_DIMENSIONS_ON = "defDimsSub";

  private final DBI db;

  @Inject
  public MetricDefinitionVerticaRepoImpl(@Named("vertica") DBI db) {

    this.db = db;

  }

  @Override
  public List<MetricName> findNames(
      String tenantId, Map<String,
      String> dimensions,
      String offset,
      int limit) throws Exception {

    List<Map<String, Object>> rows = executeMetricNamesQuery(tenantId, dimensions, offset, limit);

    List<MetricName> metricNameList = new ArrayList<>(rows.size());

    for (Map<String, Object> row : rows) {

      byte[] defId = (byte[]) row.get("id");

      String name = (String) row.get("name");

      MetricName metricName = new MetricName(Hex.encodeHexString(defId), name);

      metricNameList.add(metricName);

    }

    return metricNameList;

  }

  private List<Map<String, Object>> executeMetricNamesQuery(
      String tenantId,
      Map<String, String> dimensions,
      String offset,
      int limit) {

    String offsetPart = "";

    if (offset != null && !offset.isEmpty()) {

      offsetPart = " and defSub.id > :offset ";

    }

    // Can't bind limit in a nested sub query. So, just tack on as String.
    String limitPart = " limit " + Integer.toString(limit + 1);

    String defSubSelect =
        String.format(METRIC_NAMES_SUB_SELECT,
                      offsetPart,
                      MetricQueries.buildDimensionAndClause(dimensions, TABLE_TO_JOIN_DIMENSIONS_ON),
                      limitPart);

    String sql = String.format(FIND_METRIC_NAMES_SQL, defSubSelect);

    try (Handle h = db.open()) {

      Query<Map<String, Object>> query = h.createQuery(sql).bind("tenantId", tenantId);

      if (offset != null && !offset.isEmpty()) {

        logger.debug("binding offset: {}", offset);

        try {

          query.bind("offset", Hex.decodeHex(offset.toCharArray()));

        } catch (DecoderException e) {

          throw Exceptions.badRequest("failed to decode offset " + offset, e);
        }

      }

      DimensionQueries.bindDimensionsToQuery(query, dimensions);

      return query.list();

    }
  }

  @Override
  public List<MetricDefinition> find(
      String tenantId,
      String name,
      Map<String, String> dimensions,
      DateTime startTime,
      DateTime endTime,
      String offset,
      int limit) {

    List<Map<String, Object>>
        rows =
        executeMetricDefsQuery(tenantId, name, dimensions, startTime, endTime, offset, limit);

    List<MetricDefinition> metricDefs = new ArrayList<>(rows.size());

    byte[] currentDefDimId = null;

    Map<String, String> dims = null;

    for (Map<String, Object> row : rows) {

      byte[] defDimId = (byte[]) row.get("defdimsid");

      String metricName = (String) row.get("name");

      String dimName = (String) row.get("dname");

      String dimValue = (String) row.get("dvalue");

      if (defDimId == null || !Arrays.equals(currentDefDimId, defDimId)) {

        currentDefDimId = defDimId;

        dims = new HashMap<>();

        if (dimName != null && dimValue != null) {

          dims.put(dimName, dimValue);

        }

        MetricDefinition m = new MetricDefinition(metricName, dims);
        m.setId(Hex.encodeHexString(defDimId));
        metricDefs.add(m);


      } else {

        dims.put(dimName, dimValue);

      }
    }

    return metricDefs;
  }

  private List<Map<String, Object>> executeMetricDefsQuery(
      String tenantId,
      String name,
      Map<String, String> dimensions,
      DateTime startTime,
      DateTime endTime,
      String offset,
      int limit) {

    String namePart = "";

    if (name != null && !name.isEmpty()) {

      namePart = " and def.name = :name ";

    }

    String offsetPart = "";

    if (offset != null && !offset.isEmpty()) {

      offsetPart = " and defDims.id > :offset ";

    }

    // Can't bind limit in a nested sub query. So, just tack on as String.
    String limitPart = " limit " + Integer.toString(limit + 1);

    try (Handle h = db.open()) {

      // If startTime/endTime is specified, create the 'IN' select statement
      String timeInClause = createTimeInClause(h, startTime, endTime, tenantId, name, dimensions);

      String sql =
          String.format(FIND_METRIC_DEFS_SQL,
                        namePart, offsetPart,
                        MetricQueries.buildDimensionAndClause(dimensions, "defDims"),
                        timeInClause,
                        limitPart);


      Query<Map<String, Object>> query = h.createQuery(sql).bind("tenantId", tenantId);

      if (name != null && !name.isEmpty()) {
        logger.debug("binding name: {}", name);
        query.bind("name", name);
      }

      if (startTime != null) {
        query.bind("start_time", startTime);
      }

      if (endTime != null) {
        query.bind("end_time", endTime);
      }

      if (offset != null && !offset.isEmpty()) {

        logger.debug("binding offset: {}", offset);

        try {

          query.bind("offset", Hex.decodeHex(offset.toCharArray()));

        } catch (DecoderException e) {

          throw Exceptions.badRequest("failed to decode offset " + offset, e);
        }

      }

      DimensionQueries.bindDimensionsToQuery(query, dimensions);

      return query.list();

    }
  }

  private String createTimeInClause(
      Handle dbHandle,
      DateTime startTime,
      DateTime endTime,
      String tenantId,
      String metricName,
      Map<String, String> dimensions)
  {

    if (startTime == null) {
      return "";
    }

    Set<byte[]> defDimIdSet = new HashSet<>();

    String namePart = "";

    if (metricName != null && !metricName.isEmpty()) {
      namePart = "AND def.name = :name ";
    }

    String defDimSql = String.format(DEFDIM_IDS_SELECT, namePart,
    MetricQueries.buildDimensionAndClause(dimensions, "defDims"));

    Query<Map<String, Object>> query = dbHandle.createQuery(defDimSql).bind("tenantId", tenantId);

    DimensionQueries.bindDimensionsToQuery(query, dimensions);

    if (metricName != null && !metricName.isEmpty()) {
      query.bind("name", metricName);
    }

    List<Map<String, Object>> rows = query.list();

    for (Map<String, Object> row : rows) {
      byte[] defDimId = (byte[]) row.get("id");
      defDimIdSet.add(defDimId);
    }

    //
    // If we didn't find any definition dimension ids,
    // we won't add the time clause.
    //
    if (defDimIdSet.size() == 0) {
      return "";
    }

    String timeAndClause = "";

    if (endTime != null) {
      timeAndClause = "AND time_stamp >= :start_time AND time_stamp <= :end_time ";
    } else {
      timeAndClause = "AND time_stamp >= :start_time ";
    }

    String defDimInClause = MetricQueries.createDefDimIdInClause(defDimIdSet);
    return String.format(MEASUREMENT_AND_CLAUSE, defDimInClause, timeAndClause);
  }

}
