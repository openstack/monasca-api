package com.hpcloud.mon.infrastructure.persistence;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;

import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;
import com.hpcloud.mon.domain.model.measurement.Measurements;

/**
 * Vertica measurement repository implementation.
 * 
 * @author Jonathan Halterman
 */
public class MeasurementRepositoryImpl implements MeasurementRepository {
  private static final String FIND_BY_METRIC_DEF_SQL = "select m.definition_dimensions_id, dd.dimension_set_id, m.id, m.time_stamp, m.value "
      + "from MonMetrics.Measurements m, MonMetrics.Definitions def, MonMetrics.DefinitionDimensions dd%s "
      + "where m.definition_dimensions_id = dd.id and def.id = dd.definition_id "
      + "and def.tenant_id = :tenantId and m.time_stamp >= :startTime%s order by dd.id";

  private final DBI db;

  @Inject
  public MeasurementRepositoryImpl(@Named("vertica") DBI db) {
    this.db = db;
  }

  @Override
  public Collection<Measurements> find(String tenantId, String name,
      Map<String, String> dimensions, DateTime startTime, @Nullable DateTime endTime) {
    Handle h = db.open();

    try {
      // Build sql
      StringBuilder sbWhere = new StringBuilder();
      if (name != null)
        sbWhere.append(" and def.name = :name");
      if (endTime != null)
        sbWhere.append(" and m.time_stamp <= :endTime");
      String sql = String.format(FIND_BY_METRIC_DEF_SQL,
          MetricQueries.buildJoinClauseFor(dimensions), sbWhere);

      // Build query
      Query<Map<String, Object>> query = h.createQuery(sql)
          .bind("tenantId", tenantId)
          .bind("startTime", new Timestamp(startTime.getMillis()));
      if (name != null)
        query.bind("name", name);
      if (endTime != null)
        query.bind("endTime", new Timestamp(endTime.getMillis()));
      MetricQueries.bindDimensionsToQuery(query, dimensions);

      // Execute query
      List<Map<String, Object>> rows = query.list();

      // Build results
      Map<ByteBuffer, Measurements> results = new LinkedHashMap<>();
      for (Map<String, Object> row : rows) {
        byte[] defIdBytes = (byte[]) row.get("definition_dimensions_id");
        byte[] dimSetIdBytes = (byte[]) row.get("dimension_set_id");
        ByteBuffer defId = ByteBuffer.wrap(defIdBytes);
        long measurementId = (Long) row.get("id");
        long timestamp = ((Timestamp) row.get("time_stamp")).getTime() / 1000;
        double value = (double) row.get("value");

        Measurements measurements = results.get(defId);
        if (measurements == null) {
          measurements = new Measurements(name, MetricQueries.dimensionsFor(h, dimSetIdBytes),
              new ArrayList<Object[]>());
          results.put(defId, measurements);
        }

        measurements.addMeasurement(new Object[]{measurementId, timestamp, value});
      }

      return results.values();
    } finally {
      h.close();
    }
  }
}
