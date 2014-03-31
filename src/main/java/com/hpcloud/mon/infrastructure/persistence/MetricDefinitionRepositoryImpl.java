package com.hpcloud.mon.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;

import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.model.metric.MetricDefinitionRepository;

/**
 * Vertica metric definition repository implementation.
 * 
 * @author Jonathan Halterman
 */
public class MetricDefinitionRepositoryImpl implements MetricDefinitionRepository {
  private static final String FIND_BY_METRIC_DEF_SQL = "select def.id, def.name, d.name as dname, d.value as dvalue "
      + "from MonMetrics.Definitions def, MonMetrics.Dimensions d%s "
      + "where def.tenant_id = :tenantId and d.definition_id = def.id%s order by def.id";

  private final DBI db;

  @Inject
  public MetricDefinitionRepositoryImpl(@Named("vertica") DBI db) {
    this.db = db;
  }

  @Override
  public List<MetricDefinition> find(String tenantId, String name, Map<String, String> dimensions) {
    Handle h = db.open();

    try {
      // Build query
      StringBuilder sbFrom = new StringBuilder();
      StringBuilder sbWhere = new StringBuilder();
      MetricQueries.buildClausesForDimensions(sbFrom, sbWhere, dimensions);

      if (name != null)
        sbWhere.append(" and def.name = :name");
      String sql = String.format(FIND_BY_METRIC_DEF_SQL, sbFrom.toString(), sbWhere.toString());
      Query<Map<String, Object>> query = h.createQuery(sql).bind("tenantId", tenantId);
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
      List<MetricDefinition> metricDefs = new ArrayList<>(rows.size());
      byte[] currentId = null;
      Map<String, String> dims = null;
      for (Map<String, Object> row : rows) {
        byte[] defId = (byte[]) row.get("id");
        String metricName = (String) row.get("name");
        String dName = (String) row.get("dname");
        String dValue = (String) row.get("dvalue");

        if (defId == null || !Arrays.equals(currentId, defId)) {
          currentId = defId;
          dims = new HashMap<>();
          dims.put(dName, dValue);
          metricDefs.add(new MetricDefinition(metricName, dims));
        } else
          dims.put(dName, dValue);
      }

      return metricDefs;
    } finally {
      h.close();
    }
  }
}
