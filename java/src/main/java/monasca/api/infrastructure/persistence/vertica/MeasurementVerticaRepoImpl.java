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

import monasca.api.domain.model.measurement.MeasurementRepo;
import monasca.api.domain.model.measurement.Measurements;
import monasca.api.infrastructure.persistence.DimensionQueries;

/**
 * Vertica measurement repository implementation.
 */
public class MeasurementVerticaRepoImpl implements MeasurementRepo {
  public static final DateTimeFormatter DATETIME_FORMATTER = ISODateTimeFormat.dateTimeNoMillis()
      .withZoneUTC();
  private static final String FIND_BY_METRIC_DEF_SQL =
      "select def.name, m.definition_dimensions_id, dd.dimension_set_id, m.id, m.time_stamp, m.value "
          + "from MonMetrics.Measurements m, MonMetrics.Definitions def, MonMetrics.DefinitionDimensions dd%s "
          + "where m.definition_dimensions_id = dd.id and def.id = dd.definition_id "
          + "and def.tenant_id = :tenantId and m.time_stamp >= :startTime%s order by dd.id, m.time_stamp, m.id";

  private final DBI db;

  @Inject
  public MeasurementVerticaRepoImpl(@Named("vertica") DBI db) {
    this.db = db;
  }

  @Override
  public List<Measurements> find(String tenantId, String name,
      Map<String, String> dimensions, DateTime startTime, @Nullable DateTime endTime, @Nullable String offset) {

    // Todo. Use offset for pagination.

    try (Handle h = db.open()) {
      // Build sql
      StringBuilder sbWhere = new StringBuilder();
      if (name != null)
        sbWhere.append(" and def.name = :name");
      if (endTime != null)
        sbWhere.append(" and m.time_stamp <= :endTime");
      String sql =
          String.format(FIND_BY_METRIC_DEF_SQL, MetricQueries.buildJoinClauseFor(dimensions),
              sbWhere);

      // Build query
      Query<Map<String, Object>> query =
          h.createQuery(sql).bind("tenantId", tenantId)
              .bind("startTime", new Timestamp(startTime.getMillis()));
      if (name != null)
        query.bind("name", name);
      if (endTime != null)
        query.bind("endTime", new Timestamp(endTime.getMillis()));
      DimensionQueries.bindDimensionsToQuery(query, dimensions);

      // Execute query
      List<Map<String, Object>> rows = query.list();

      // Build results
      Map<ByteBuffer, Measurements> results = new LinkedHashMap<>();
      for (Map<String, Object> row : rows) {
        String metricName = (String) row.get("name");
        byte[] defIdBytes = (byte[]) row.get("definition_dimensions_id");
        byte[] dimSetIdBytes = (byte[]) row.get("dimension_set_id");
        ByteBuffer defId = ByteBuffer.wrap(defIdBytes);
        long measurementId = (Long) row.get("id");
        String timestamp = DATETIME_FORMATTER.print(((Timestamp) row.get("time_stamp")).getTime());
        double value = (double) row.get("value");

        Measurements measurements = results.get(defId);
        if (measurements == null) {
          measurements =
              new Measurements(metricName, MetricQueries.dimensionsFor(h, dimSetIdBytes),
                  new ArrayList<Object[]>());
          results.put(defId, measurements);
        }

        // TODO - Really support valueMeta
        measurements.addMeasurement(new Object[] {measurementId, timestamp, value, new HashMap<String, String>()});
      }

      return new ArrayList<Measurements>(results.values());
    }
  }
}
