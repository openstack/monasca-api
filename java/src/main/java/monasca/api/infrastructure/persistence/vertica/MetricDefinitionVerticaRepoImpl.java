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

import monasca.api.domain.model.metric.MetricName;
import monasca.api.resource.exception.Exceptions;
import monasca.common.model.metric.MetricDefinition;
import monasca.api.domain.model.metric.MetricDefinitionRepo;
import monasca.api.infrastructure.persistence.DimensionQueries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class MetricDefinitionVerticaRepoImpl implements MetricDefinitionRepo {

  private static final Logger logger = LoggerFactory
      .getLogger(MetricDefinitionVerticaRepoImpl.class);

  private static final String FIND_BY_METRIC_DEF_SQL =
      "select defdims.id, def.name, dims.name as dname, dims.value as dvalue "
      + "from MonMetrics.Definitions def, MonMetrics.DefinitionDimensions defdims "
      + "left outer join MonMetrics.Dimensions dims on dims.dimension_set_id = defdims.dimension_set_id "
      + "%s "
      + "where def.id = defdims.definition_id and def.tenant_id = :tenantId "
      + "%s "
      + "order by defdims.id ASC "
      + "limit :limit";

  private final DBI db;

  @Inject
  public MetricDefinitionVerticaRepoImpl(
      @Named("vertica") DBI db) {

    this.db = db;

  }

  @Override
  public List<MetricDefinition> find(
      String tenantId,
      String name,
      Map<String, String> dimensions,
      String offset, int limit) {

    StringBuilder sb = new StringBuilder();

    if (name != null && !name.isEmpty()) {

      sb.append(" and def.name = :name");

    }

    if (offset != null && !offset.isEmpty()) {

      sb.append(" and defdims.id > :offset");

    }

    String sql =
        String.format(FIND_BY_METRIC_DEF_SQL,
                      MetricQueries.buildJoinClauseFor(dimensions),
                      sb);

    try (Handle h = db.open()) {

      Query<Map<String, Object>> query =
          h.createQuery(sql)
              .bind("tenantId", tenantId)
              .bind("limit", limit + 1);

      if (name != null && !name.isEmpty()) {

        logger.debug("binding name: {}", name);

        query.bind("name", name);

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

      List<Map<String, Object>> rows = query.list();

      List<MetricDefinition> metricDefs = new ArrayList<>(rows.size());

      byte[] currentDefDimId = null;

      Map<String, String> dims = null;

      for (Map<String, Object> row : rows) {

        byte[] defDimId = (byte[]) row.get("id");

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
  }

  @Override
  public List<MetricName> findNames(
      String tenantId,
      Map<String, String> dimensions,
      String offset, int limit) throws Exception {

    throw new NotImplementedException();

  }
}
