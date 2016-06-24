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
package monasca.api.infrastructure.persistence.mysql;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.sql.SQLException;
import java.sql.ResultSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.StatementContext;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.common.model.alarm.AggregateFunction;
import monasca.common.model.alarm.AlarmOperator;
import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import monasca.common.util.Conversions;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepo;
import monasca.api.infrastructure.persistence.DimensionQueries;
import monasca.api.infrastructure.persistence.SubAlarmDefinitionQueries;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * Alarm repository implementation.
 */
public class AlarmDefinitionMySqlRepoImpl implements AlarmDefinitionRepo {
  private static final Joiner COMMA_JOINER = Joiner.on(',');
  private static final String SUB_ALARM_SQL =
      "select sa.*, sad.dimensions from sub_alarm_definition as sa "
          + "left join (select sub_alarm_definition_id, group_concat(dimension_name, '=', value) as dimensions from sub_alarm_definition_dimension group by sub_alarm_definition_id ) as sad "
          + "on sad.sub_alarm_definition_id = sa.id where sa.alarm_definition_id = :alarmDefId";
  private static final String CREATE_SUB_EXPRESSION_SQL = "insert into sub_alarm_definition "
          + "(id, alarm_definition_id, function, metric_name, "
          + "operator, threshold, period, periods, is_deterministic, "
          + "created_at, updated_at) "
          + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
  private static final String UPDATE_SUB_ALARM_DEF_SQL = "update sub_alarm_definition set "
      + "operator = ?, threshold = ?, is_deterministic = ?, updated_at = NOW() where id = ?";

  private final DBI db;
  private final PersistUtils persistUtils;

  @Inject
  public AlarmDefinitionMySqlRepoImpl(@Named("mysql") DBI db, PersistUtils persistUtils) {
    this.db = db;
    this.persistUtils = persistUtils;
  }

  @Override
  public AlarmDefinition create(String tenantId, String id, String name, String description,
      String severity, String expression, Map<String, AlarmSubExpression> subExpressions,
      List<String> matchBy, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions) {
    Handle h = db.open();

    try {
      h.begin();
      h.insert(
          "insert into alarm_definition (id, tenant_id, name, description, severity, expression, match_by, actions_enabled, created_at, updated_at, deleted_at) values (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NULL)",
          id, tenantId, name, description, severity, expression,
          matchBy == null || Iterables.isEmpty(matchBy) ? null : COMMA_JOINER.join(matchBy), true);

      // Persist sub-alarms
      createSubExpressions(h, id, subExpressions);

      // Persist actions
      persistActions(h, id, AlarmState.ALARM, alarmActions);
      persistActions(h, id, AlarmState.OK, okActions);
      persistActions(h, id, AlarmState.UNDETERMINED, undeterminedActions);

      h.commit();
      return new AlarmDefinition(id, name, description, severity, expression, matchBy, true,
          alarmActions, okActions == null ? Collections.<String>emptyList() : okActions,
          undeterminedActions == null ? Collections.<String>emptyList() : undeterminedActions);
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }

  @Override
  public void deleteById(String tenantId, String alarmDefId) {
    try (Handle h = db.open()) {
      if (h
          .update(
              "update alarm_definition set deleted_at = NOW() where tenant_id = ? and id = ? and deleted_at is NULL",
              tenantId, alarmDefId) == 0)
        throw new EntityNotFoundException("No alarm definition exists for %s", alarmDefId);

      // Cascade soft delete to alarms
      h.execute("delete from alarm where alarm_definition_id = :id", alarmDefId);
    }
  }

    @Override
    public String exists(String tenantId, String name) {
        try (Handle h = db.open()) {
            Map<String, Object> map = h
                    .createQuery(
                            "select id from alarm_definition where tenant_id = :tenantId and name = :name and deleted_at is NULL")
                    .bind("tenantId", tenantId).bind("name", name).first();
            if (map != null) {
                if (map.values().size() != 0) {
                    return map.get("id").toString();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

  @SuppressWarnings("unchecked")
  @Override
  public List<AlarmDefinition> find(String tenantId, String name,
      Map<String, String> dimensions, List<AlarmSeverity> severities,
      List<String> sortBy, String offset, int limit) {


    try (Handle h = db.open()) {

      String query =
          "  SELECT t.id, t.tenant_id, t.name, t.description, t.expression, t.severity, t.match_by,"
          + "     t.actions_enabled, t.created_at, t.updated_at, t.deleted_at, "
          + "     GROUP_CONCAT(aa.alarm_state) AS states, "
          + "     GROUP_CONCAT(aa.action_id) AS notificationIds "
          + "FROM (SELECT distinct ad.id, ad.tenant_id, ad.name, ad.description, ad.expression,"
          + "        ad.severity, ad.match_by, ad.actions_enabled, ad.created_at, "
          + "        ad.updated_at, ad.deleted_at "
          + "      FROM alarm_definition AS ad "
          + "      LEFT OUTER JOIN sub_alarm_definition AS sad ON ad.id = sad.alarm_definition_id "
          + "      LEFT OUTER JOIN sub_alarm_definition_dimension AS dim ON sad.id = dim.sub_alarm_definition_id %1$s "
          + "      WHERE ad.tenant_id = :tenantId AND ad.deleted_at IS NULL %2$s) AS t "
          + "LEFT OUTER JOIN alarm_action AS aa ON t.id = aa.alarm_definition_id "
          + "GROUP BY t.id %3$s %4$s %5$s";

      StringBuilder sbWhere = new StringBuilder();

      if (name != null) {
        sbWhere.append(" and ad.name = :name");
      }

      sbWhere.append(MySQLUtils.buildSeverityAndClause(severities));

      String orderByPart = "";
      if (sortBy != null && !sortBy.isEmpty()) {
        orderByPart = " order by " + COMMA_JOINER.join(sortBy);
        if (!orderByPart.contains("id")) {
          orderByPart = orderByPart + ",id";
        }
      } else {
        orderByPart = " order by id ";
      }

      String limitPart = "";
      if (limit > 0) {
        limitPart = " limit :limit";
      }

      String offsetPart = "";
      if (offset != null) {
        offsetPart = " offset " + offset + ' ';
      }

      String sql = String.format(query,
          SubAlarmDefinitionQueries.buildJoinClauseFor(dimensions), sbWhere, orderByPart,
          limitPart, offsetPart);

      Query<?> q = h.createQuery(sql);

      q.bind("tenantId", tenantId);

      if (name != null) {
        q.bind("name", name);
      }

      MySQLUtils.bindSeverityToQuery(q, severities);

      if (limit > 0) {
        q.bind("limit", limit + 1);
      }

      q.registerMapper(new AlarmDefinitionMapper());
      q = q.mapTo(AlarmDefinition.class);
      SubAlarmDefinitionQueries.bindDimensionsToQuery(q, dimensions);
      List<AlarmDefinition> resultSet = (List<AlarmDefinition>) q.list();
      return resultSet;
    }
  }

  @Override
  public AlarmDefinition findById(String tenantId, String alarmDefId) {

    try (Handle h = db.open()) {
      String query = "SELECT alarm_definition.id, alarm_definition.tenant_id, alarm_definition.name, alarm_definition.description, "
          + "alarm_definition.expression, alarm_definition.severity, alarm_definition.match_by, alarm_definition.actions_enabled, "
          +" alarm_definition.created_at, alarm_definition.updated_at, alarm_definition.deleted_at, "
          + "GROUP_CONCAT(alarm_action.action_id) AS notificationIds,group_concat(alarm_action.alarm_state) AS states "
          + "FROM alarm_definition LEFT OUTER JOIN alarm_action ON alarm_definition.id=alarm_action.alarm_definition_id "
          + " WHERE alarm_definition.tenant_id=:tenantId AND alarm_definition.id=:alarmDefId AND alarm_definition.deleted_at "
          + " IS NULL GROUP BY alarm_definition.id";

      Query<?> q = h.createQuery(query);
      q.bind("tenantId", tenantId);
      q.bind("alarmDefId", alarmDefId);

      q.registerMapper(new AlarmDefinitionMapper());
      q = q.mapTo(AlarmDefinition.class);
      AlarmDefinition alarmDefinition = (AlarmDefinition) q.first();
     if(alarmDefinition == null)
     {
       throw new EntityNotFoundException("No alarm definition exists for %s", alarmDefId);
     }
      return alarmDefinition;
    }
  }

  @Override
  public Map<String, MetricDefinition> findSubAlarmMetricDefinitions(String alarmDefId) {
    try (Handle h = db.open()) {
      List<Map<String, Object>> rows =
          h.createQuery(SUB_ALARM_SQL).bind("alarmDefId", alarmDefId).list();
      Map<String, MetricDefinition> subAlarmMetricDefs = new HashMap<>();
      for (Map<String, Object> row : rows) {
        String id = (String) row.get("id");
        String metricName = (String) row.get("metric_name");
        Map<String, String> dimensions =
            DimensionQueries.dimensionsFor((String) row.get("dimensions"));
        subAlarmMetricDefs.put(id, new MetricDefinition(metricName, dimensions));
      }

      return subAlarmMetricDefs;
    }
  }

  @Override
  public Map<String, AlarmSubExpression> findSubExpressions(String alarmDefId) {
    try (Handle h = db.open()) {
      List<Map<String, Object>> rows =
          h.createQuery(SUB_ALARM_SQL).bind("alarmDefId", alarmDefId).list();
      Map<String, AlarmSubExpression> subExpressions = new HashMap<>();
      for (Map<String, Object> row : rows) {
        String id = (String) row.get("id");
        AggregateFunction function = AggregateFunction.fromJson((String) row.get("function"));
        String metricName = (String) row.get("metric_name");
        AlarmOperator operator = AlarmOperator.fromJson((String) row.get("operator"));
        Double threshold = (Double) row.get("threshold");
        // MySQL connector returns an Integer, Drizzle returns a Long for period and periods.
        // Need to convert the results appropriately based on type.
        Integer period = Conversions.variantToInteger(row.get("period"));
        Integer periods = Conversions.variantToInteger(row.get("periods"));
        Boolean isDeterministic = Conversions.variantToBoolean(row.get("is_deterministic"));
        Map<String, String> dimensions =
            DimensionQueries.dimensionsFor((String) row.get("dimensions"));

        subExpressions.put(
            id,
            new AlarmSubExpression(
                function,
                new MetricDefinition(metricName, dimensions),
                operator,
                threshold,
                period,
                periods,
                isDeterministic
            )
        );

      }

      return subExpressions;
    }
  }

  @Override
  public void update(String tenantId, String id, boolean patch, String name, String description,
      String expression, List<String> matchBy, String severity, boolean actionsEnabled,
      Collection<String> oldSubAlarmIds, Map<String, AlarmSubExpression> changedSubAlarms,
      Map<String, AlarmSubExpression> newSubAlarms, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    Handle h = db.open();

    try {
      h.begin();
      h.insert(
          "update alarm_definition set name = ?, description = ?, expression = ?, match_by = ?, severity = ?, actions_enabled = ?, updated_at = NOW() where tenant_id = ? and id = ?",
          name, description, expression, matchBy == null || Iterables.isEmpty(matchBy) ? null
              : COMMA_JOINER.join(matchBy), severity, actionsEnabled, tenantId, id);

      // Delete old sub-alarms
      if (oldSubAlarmIds != null)
        for (String oldSubAlarmId : oldSubAlarmIds)
          h.execute("delete from sub_alarm_definition where id = ?", oldSubAlarmId);

      // Update changed sub-alarms
      if (changedSubAlarms != null)
        for (Map.Entry<String, AlarmSubExpression> entry : changedSubAlarms.entrySet()) {
          AlarmSubExpression sa = entry.getValue();
          h.execute(
              UPDATE_SUB_ALARM_DEF_SQL,
              sa.getOperator().name(),
              sa.getThreshold(),
              sa.isDeterministic(),
              entry.getKey()
          );
        }

      // Insert new sub-alarms
      createSubExpressions(h, id, newSubAlarms);

      // Delete old actions
      if (patch) {
        deleteActions(h, id, AlarmState.ALARM, alarmActions);
        deleteActions(h, id, AlarmState.OK, okActions);
        deleteActions(h, id, AlarmState.UNDETERMINED, undeterminedActions);
      } else
        h.execute("delete from alarm_action where alarm_definition_id = ?", id);

      // Insert new actions
      persistActions(h, id, AlarmState.ALARM, alarmActions);
      persistActions(h, id, AlarmState.OK, okActions);
      persistActions(h, id, AlarmState.UNDETERMINED, undeterminedActions);

      h.commit();
    } catch (RuntimeException e) {
      h.rollback();
      throw e;
    } finally {
      h.close();
    }
  }

  private void deleteActions(Handle handle, String id, AlarmState alarmState, List<String> actions) {
    if (actions != null)
      handle.execute("delete from alarm_action where alarm_definition_id = ? and alarm_state = ?", id,
          alarmState.name());
  }

  private void persistActions(Handle handle, String id, AlarmState alarmState, List<String> actions) {
    if (actions != null)
      for (String action : actions)
        handle.insert("insert into alarm_action values (?, ?, ?)", id, alarmState.name(), action);
  }

  private void createSubExpressions(Handle handle, String id,
      Map<String, AlarmSubExpression> alarmSubExpressions) {
    if (alarmSubExpressions != null) {
      for (Map.Entry<String, AlarmSubExpression> subEntry : alarmSubExpressions.entrySet()) {
        String subAlarmId = subEntry.getKey();
        AlarmSubExpression subExpr = subEntry.getValue();
        MetricDefinition metricDef = subExpr.getMetricDefinition();

        // Persist sub-alarm
        handle.insert(CREATE_SUB_EXPRESSION_SQL, subAlarmId, id, subExpr.getFunction().name(),
            metricDef.name, subExpr.getOperator().name(), subExpr.getThreshold(),
            subExpr.getPeriod(), subExpr.getPeriods(), subExpr.isDeterministic());

        // Persist sub-alarm dimensions
        if (metricDef.dimensions != null && !metricDef.dimensions.isEmpty())
          for (Map.Entry<String, String> dimEntry : metricDef.dimensions.entrySet())
            handle.insert("insert into sub_alarm_definition_dimension values (?, ?, ?)", subAlarmId,
                dimEntry.getKey(), dimEntry.getValue());
      }
    }
  }

  private static class AlarmDefinitionMapper implements ResultSetMapper<AlarmDefinition> {

    private static final Splitter
        COMMA_SPLITTER =
        Splitter.on(',').omitEmptyStrings().trimResults();

    public AlarmDefinition map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      String notificationIds = r.getString("notificationIds");
      String states = r.getString("states");
      String matchBy = r.getString("match_by");
      List<String> notifications = splitStringIntoList(notificationIds);
      List<String> state = splitStringIntoList(states);
      List<String> match = splitStringIntoList(matchBy);

      List<String> okActionIds = new ArrayList<String>();
      List<String> alarmActionIds = new ArrayList<String>();
      List<String> undeterminedActionIds = new ArrayList<String>();

      int stateAndActionIndex = 0;
      for (String singleState : state) {
        if (singleState.equals(AlarmState.UNDETERMINED.name())) {
          undeterminedActionIds.add(notifications.get(stateAndActionIndex));
        }
        if (singleState.equals(AlarmState.OK.name())) {
          okActionIds.add(notifications.get(stateAndActionIndex));
        }
        if (singleState.equals(AlarmState.ALARM.name())) {
          alarmActionIds.add(notifications.get(stateAndActionIndex));
        }
        stateAndActionIndex++;
      }

      return new AlarmDefinition(r.getString("id"), r.getString("name"), r.getString("description"),
                                 r.getString("severity"), r.getString("expression"), match,
                                 r.getBoolean("actions_enabled"), alarmActionIds, okActionIds,
                                 undeterminedActionIds);
    }

    private List<String> splitStringIntoList(String commaDelimitedString) {
      if (commaDelimitedString == null) {
        return new ArrayList<String>();
      }
      Iterable<String> split = COMMA_SPLITTER.split(commaDelimitedString);
      return Lists.newArrayList(split);
    }
  }
}

