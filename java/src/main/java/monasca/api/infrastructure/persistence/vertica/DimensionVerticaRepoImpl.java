/* Copyright (c) 2016 Hewlett-Packard Development Company, L.P.
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

import monasca.api.ApiConfig;
import monasca.api.domain.model.dimension.DimensionRepo;
import monasca.api.domain.model.dimension.DimensionValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.annotation.Nullable;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DimensionVerticaRepoImpl implements DimensionRepo {

  private static final Logger logger = LoggerFactory
      .getLogger(DimensionVerticaRepoImpl.class);

  private static final String FIND_DIMENSION_VALUES_SQL =
        "SELECT %s"                  // dbHint goes here
      + "  DISTINCT dims.value as dValue "
      + "FROM "
      + "  MonMetrics.Definitions def,"
      + "  MonMetrics.DefinitionDimensions defdims "
      + "LEFT OUTER JOIN"
      + "  MonMetrics.Dimensions dims"
      + "    ON dims.dimension_set_id = defdims.dimension_set_id "
      + "WHERE"
      + "  def.id = defdims.definition_id"
      + "  %s "                      // optional offset goes here
      + "  %s "                      // optional metric name goes here
      + "  and def.tenant_id = '%s'" // tenant_id goes here
      + "  and dims.name = '%s' "    // dimension name goes here
      + "ORDER BY dims.value ASC "
      + "%s ";                       // limit goes here

  private final DBI db;
  private final String dbHint;

  @Inject
  public DimensionVerticaRepoImpl(
      @Named("vertica") DBI db, ApiConfig config)
  {
    this.db = db;
    this.dbHint = config.vertica.dbHint;
  }

  @Override
  public DimensionValues find(
    String metricName,
    String tenantId,
    String dimensionName,
    String offset,
    int limit) throws Exception
  {
    List<String> values = new ArrayList<String>();
    String offsetPart = "";
    String metricNamePart = "";

    try (Handle h = db.open()) {

      if (offset != null && !offset.isEmpty()) {
        offsetPart = " and dims.value > '" + offset + "' ";
      }

      if (metricName != null && !metricName.isEmpty()) {
        metricNamePart = " and def.name = '" + metricName + "' ";
      }

      String limitPart = " limit " + Integer.toString(limit + 1);

      String sql = String.format(FIND_DIMENSION_VALUES_SQL,
                                 this.dbHint,
                                 offsetPart,
                                 metricNamePart,
                                 tenantId,
                                 dimensionName,
                                 limitPart);

      Query<Map<String, Object>> query = h.createQuery(sql);

      List<Map<String, Object>> rows = query.list();
      for (Map<String, Object> row : rows) {
        String dimValue = (String) row.get("dValue");
        values.add(dimValue);
      }
    }
    return new DimensionValues(metricName, dimensionName, values);
  }
}
