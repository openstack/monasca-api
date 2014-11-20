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
package monasca.api.infrastructure.persistence.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmRepository;

import monasca.api.infrastructure.persistence.DimensionQueries;
import monasca.common.persistence.BeanMapper;
import monasca.common.persistence.SqlQueries;

/**
 * Alarmed metric repository implementation.
 */
public class AlarmMySqlRepositoryImpl implements AlarmRepository {
  private final DBI db;
  private static final String METRIC_DEFS_FOR_ALARM_SQL =
      "select md.name, md.tenant_id, mdg.dimensions from metric_definition as md "
          + "inner join metric_definition_dimensions as mdd on mdd.metric_definition_id = md.id "
          + "inner join alarm_metric as am on am.metric_definition_dimensions_id = mdd.id "
          + "left join (select dimension_set_id, group_concat(name, '=', value) as dimensions from metric_dimension "
          + "group by dimension_set_id) as mdg on mdg.dimension_set_id = mdd.metric_dimension_set_id "
          + "where am.alarm_id = :alarmId and md.tenant_id = :tenantId";
  private static final String ALARM_SQL =
      "select distinct a.id, a.alarm_definition_id, a.state, ad.severity, ad.tenant_id, ad.name as alarm_definition_name from alarm a "
          + "inner join alarm_metric am on am.alarm_id = a.id "
          + "inner join metric_definition_dimensions mdd on mdd.id = am.metric_definition_dimensions_id "
          + "inner join metric_definition md on md.id = mdd.metric_definition_id%s "
          + "inner join alarm_definition ad on ad.id = a.alarm_definition_id "
          + "where ad.tenant_id = :tenantId%s order by a.created_at";

  @Inject
  public AlarmMySqlRepositoryImpl(@Named("mysql") DBI db) {
    this.db = db;
  }

  static String buildJoinClauseFor(Map<String, String> dimensions) {
    StringBuilder sbJoin = null;
    if (dimensions != null) {
      sbJoin = new StringBuilder();
      for (int i = 0; i < dimensions.size(); i++) {
        sbJoin.append(" inner join metric_dimension d").append(i).append(" on d").append(i)
            .append(".name = :dname").append(i).append(" and d").append(i)
            .append(".value = :dvalue").append(i).append(" and mdd.metric_dimension_set_id = d")
            .append(i).append(".dimension_set_id");
      }
    }

    return sbJoin == null ? "" : sbJoin.toString();
  }

  static Map<String, String> dimensionsFor(Handle handle, byte[] dimensionSetId) {
    return SqlQueries.keyValuesFor(handle, "select name, value from metric_dimension " + "where"
        + " dimension_set_id = ?", dimensionSetId);
  }

  private static List<MetricDefinition> findMetrics(Handle handle, String tenantId, String alarmId) {
    List<MetricDefinition> metricDefs = new ArrayList<>();
    for (Map<String, Object> row : handle.createQuery(METRIC_DEFS_FOR_ALARM_SQL)
        .bind("alarmId", alarmId).bind("tenantId", tenantId).list()) {
      String metName = (String) row.get("name");
      Map<String, String> dimensions =
          DimensionQueries.dimensionsFor((String) row.get("dimensions"));
      metricDefs.add(new MetricDefinition(metName, dimensions));
    }

    return metricDefs;
  }

  @Override
  public void deleteById(String tenantId, String id) {
      String sql = "delete alarm.* from alarm " +
                   "join (select distinct a.id " +
                   "from alarm as a " +
                   "inner join alarm_definition as ad " +
                   "on ad.id = a.alarm_definition_id " +
                   "where ad.tenant_id = :tenantId and a.id = :id) as b " +
                   "on b.id = alarm.id";

      try (Handle h = db.open()) {
      h.execute(sql, tenantId, id);
    }
  }

  private static class AlarmMapper implements ResultSetMapper<Alarm> {
    public Alarm map(int rowIndex, ResultSet rs, StatementContext ctxt) throws SQLException {
      // Metrics will get set later
      final Alarm alarm =
          new Alarm(rs.getString("id"), rs.getString("alarm_definition_id"),
              rs.getString("alarm_definition_name"), rs.getString("severity"), null,
              AlarmState.valueOf((rs.getString("state"))));
      return alarm;
    }
  }

  @Override
  public List<Alarm> find(String tenantId, String alarmDefId, String metricName,
      Map<String, String> metricDimensions, AlarmState state) {
    try (Handle h = db.open()) {
      StringBuilder sbWhere = new StringBuilder();

      if (alarmDefId != null) {
        sbWhere.append(" and ad.id = :alarmDefId");
      }
      if (metricName != null) {
        sbWhere.append(" and md.name = :metricName");
      }
      if (state != null) {
        sbWhere.append(" and a.state = :state");
      }

      String sql = String.format(ALARM_SQL, buildJoinClauseFor(metricDimensions), sbWhere);
      Query<?> q = h.createQuery(sql).bind("tenantId", tenantId);

      if (alarmDefId != null) {
        q.bind("alarmDefId", alarmDefId);
      }
      if (metricName != null) {
        q.bind("metricName", metricName);
      }
      if (state != null) {
        q.bind("state", state.name());
      }

      Query<Alarm> qAlarm = q.map(new AlarmMapper());
      DimensionQueries.bindDimensionsToQuery(q, metricDimensions);

      List<Alarm> alarms = qAlarm.list();
      for (Alarm alarm : alarms) {
        alarm.setMetrics(findMetrics(h, tenantId, alarm.getId()));
      }
      return alarms;
    }
  }

  @Override
  public Alarm findById(String tenantId, String alarmId) {
    try (Handle h = db.open()) {
      Alarm alarm =
          h.createQuery("select ad.tenant_id, ad.severity, ad.name as alarm_definition_name, "
              + "a.* from alarm as a inner join alarm_definition ad on ad.id = a.alarm_definition_id "
              + "where a.id = :id and ad.tenant_id = :tenantId")
              .bind("id", alarmId)
              .bind("tenantId", tenantId)
              .map(new AlarmMapper()).first();
      if (alarm == null)
        throw new EntityNotFoundException("No alarm exists for %s", alarmId);

      // Hydrate metrics
      alarm.setMetrics(findMetrics(h, tenantId, alarm.getId()));
      return alarm;
    }
  }

  @Override
  public List<MetricDefinition> findMetrics(String tenantId, String alarmId) {
    try (Handle h = db.open()) {
      return findMetrics(h, tenantId, alarmId);
    }
  }

  @Override
  public void update(String tenantId, String id, AlarmState state) {
    Handle h = db.open();

    try {
      h.begin();
      h.insert("update alarm set state = ?, updated_at = NOW() where id = ?", state.name(), id);
      h.commit();
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }

  public static class SubAlarm {
    private String id;
    private String expression;
    public String getId() {
      return id;
    }
    public void setId(String id) {
      this.id = id;
    }
    public String getExpression() {
      return expression;
    }
    public void setExpression(String expression) {
      this.expression = expression;
    }
  }

  @Override
  public Map<String, AlarmSubExpression> findAlarmSubExpressions(String alarmId) {
    try (Handle h = db.open()) {
      final List<SubAlarm> result = h
          .createQuery("select * from sub_alarm where alarm_id = :alarmId")
          .bind("alarmId", alarmId)
          .map(new BeanMapper<SubAlarm>(SubAlarm.class)).list();
      final Map<String, AlarmSubExpression> subAlarms = new HashMap<>(result.size());

      for (SubAlarm row : result) {
        subAlarms.put(row.id, AlarmSubExpression.of(row.expression));
      }

      return subAlarms;
    }
  }
}
