package com.hpcloud.mon.infrastructure.persistence;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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

import com.hpcloud.mon.domain.model.measurement.Measurement;
import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;
import com.hpcloud.mon.domain.model.measurement.Measurements;
import com.hpcloud.persistence.SqlQueries;

/**
 * Vertica measurement repository implementation.
 */
public class MeasurementRepositoryImpl implements MeasurementRepository {
  private static final String FIND_BY_METRIC_DEF_SQL = "select m.metric_definition_id, m.time_stamp, m.value "
      + "from MonMetrics.Measurements m, MonMetrics.Definitions def%s "
      + "where m.metric_definition_id = def.id%s order by m.metric_definition_id";

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
      // Build query
      StringBuilder sbFrom = new StringBuilder();
      StringBuilder sbWhere = new StringBuilder();
      if (dimensions != null) {
        for (int i = 0; i < dimensions.size(); i++) {
          sbFrom.append(", MonMetrics.Dimensions d").append(i);
          sbWhere.append(" and d")
              .append(i)
              .append(".name = :dname")
              .append(i)
              .append(" and d")
              .append(i)
              .append(".value = :dvalue")
              .append(i)
              .append(" and def.id = d")
              .append(i)
              .append(".metric_definition_id");
        }
      }

      if (name != null)
        sbWhere.append(" and def.name = :name");
      String sql = String.format(FIND_BY_METRIC_DEF_SQL, sbFrom.toString(), sbWhere.toString());
      Query<Map<String, Object>> query = h.createQuery(sql);
      if (name != null)
        query.bind("name", name);
      if (dimensions != null) {
        int i = 0;
        for (Iterator<Map.Entry<String, String>> it = dimensions.entrySet().iterator(); it.hasNext(); i++) {
          Map.Entry<String, String> entry = it.next();
          query.bind("dname" + i, entry.getKey());
          query.bind("dvalue" + i, entry.getValue());
        }
      }

      // Execute
      List<Map<String, Object>> rows = query.list();

      // Build results
      Map<ByteBuffer, Measurements> results = new LinkedHashMap<>();
      for (Map<String, Object> row : rows) {
        byte[] defIdBytes = (byte[]) row.get("metric_definition_id");
        ByteBuffer defId = ByteBuffer.wrap(defIdBytes);
        long timestamp = ((Timestamp) row.get("time_stamp")).getTime();
        double value = (double) row.get("value");

        Measurements measurements = results.get(defId);
        if (measurements == null) {
          Map<String, String> dims = SqlQueries.keyValuesFor(h,
              "select name, value from MonMetrics.Dimensions where metric_definition_id = ?",
              defIdBytes);
          measurements = new Measurements(name, dims, new ArrayList<Measurement>());
          results.put(defId, measurements);
        }

        measurements.addMeasurement(new Measurement(timestamp, value));
      }

      return results.values();
    } finally {
      h.close();
    }
  }

}
