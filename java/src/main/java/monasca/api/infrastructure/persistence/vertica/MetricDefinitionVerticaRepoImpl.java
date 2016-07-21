/*
 * (C) Copyright 2014,2016 Hewlett Packard Enterprise Development Company LP
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
import monasca.api.resource.exception.Exceptions;
import monasca.api.ApiConfig;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

public class MetricDefinitionVerticaRepoImpl implements MetricDefinitionRepo {

  private static final Logger
      logger =
      LoggerFactory.getLogger(MetricDefinitionVerticaRepoImpl.class);

  private static final String METRIC_DEF_SUB_QUERY =
      "SELECT TO_HEX(defDimsSub.id) "
      + "FROM MonMetrics.Definitions defSub "
      + "JOIN MonMetrics.DefinitionDimensions defDimsSub ON defSub.id = defDimsSub.definition_id "
      + "WHERE defSub.tenant_id = :tenantId "
      + "%s " // Name goes here
      + "%s " // Offset goes here
      + "%s " // Dimensions and clause goes here
      + "%s " // Time qualifier goes here
      + "GROUP BY defDimsSub.id "
      + "ORDER BY defDimsSub.id ASC "
      + "%s "; // limit goes here

  private static final String FIND_METRIC_NAMES_SQL =
      "SELECT %s distinct def.id, def.name "
      + "FROM MonMetrics.Definitions def "
      + "WHERE def.id IN (%s) " // Subselect goes here
      + "ORDER BY def.id ASC ";

  private static final String METRIC_NAMES_SUB_SELECT =
      "SELECT distinct MAX(defSub.id) as max_id " // The aggregation function gives us one id per name
      + "FROM  MonMetrics.Definitions defSub "
      + "JOIN MonMetrics.DefinitionDimensions defDimsSub ON defDimsSub.definition_id = defSub.id "
      + "WHERE defSub.tenant_id = :tenantId "
      + "%s " // Offset goes here.
      + "%s " // Dimensions and clause goes here
      + "GROUP BY defSub.name "   // This is to reduce the (id, name) sets to only include unique names
      + "ORDER BY max_id ASC %s"; // Limit goes here.


  private static final String TABLE_TO_JOIN_ON = "defDimsSub";

  private final DBI db;

  private final String dbHint;

  @Inject
  public MetricDefinitionVerticaRepoImpl(@Named("vertica") DBI db, ApiConfig config)
  {
    this.db = db;
    this.dbHint = config.vertica.dbHint;
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
                      MetricQueries.buildDimensionAndClause(dimensions,
                                                            TABLE_TO_JOIN_ON),
                      limitPart);

    String sql = String.format(FIND_METRIC_NAMES_SQL, this.dbHint, defSubSelect);

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

      MetricQueries.bindDimensionsToQuery(query, dimensions);

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

    String currentDefDimId = null;

    Map<String, String> dims = null;

    for (Map<String, Object> row : rows) {

      String defDimId = (String) row.get("defDimsId");

      String metricName = (String) row.get("name");

      String dimName = (String) row.get("dName");

      String dimValue = (String) row.get("dValue");

      if (defDimId == null || !defDimId.equals(currentDefDimId)) {

        currentDefDimId = defDimId;

        dims = new HashMap<>();

        if (dimName != null && dimValue != null) {

          dims.put(dimName, dimValue);

        }

        MetricDefinition m = new MetricDefinition(metricName, dims);
        m.setId(defDimId);
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

      namePart = " and defSub.name = :name ";

    }

    String offsetPart = "";

    if (offset != null && !offset.isEmpty()) {

      offsetPart = " and defDimsSub.id > :offset ";

    }

    String limitPart = "";

    if (limit > 0) {

      limitPart = "limit " + Integer.toString(limit + 1);

    }

    try (Handle h = db.open()) {

      String sql =
        String.format(MetricQueries.FIND_METRIC_DEFS_SQL,
                      this.dbHint,
                      String.format(METRIC_DEF_SUB_QUERY,
                                    namePart,
                                    offsetPart,
                                    MetricQueries.buildDimensionAndClause(dimensions,
                                                                          TABLE_TO_JOIN_ON),
                                    MetricQueries.buildTimeAndClause(startTime, endTime,
                                                                     TABLE_TO_JOIN_ON),
                                    limitPart)
                      );

      Query<Map<String, Object>> query = h.createQuery(sql).bind("tenantId", tenantId);

      if (name != null && !name.isEmpty()) {
        logger.debug("binding name: {}", name);
        query.bind("name", name);
      }

      if (startTime != null) {
        query.bind("startTime", startTime);
      }

      if (endTime != null) {
        query.bind("endTime", endTime);
      }

      if (offset != null && !offset.isEmpty()) {

        logger.debug("binding offset: {}", offset);

        try {

          query.bind("offset", Hex.decodeHex(offset.toCharArray()));

        } catch (DecoderException e) {

          throw Exceptions.badRequest("failed to decode offset " + offset, e);
        }

      }

      MetricQueries.bindDimensionsToQuery(query, dimensions);

      return query.list();

    }
  }
}
