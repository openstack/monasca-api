/*
 * Copyright 2015 FUJITSU LIMITED
 * Copyright 2016 Hewlett Packard Enterprise Development Company, L.P.
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
package monasca.api.infrastructure.persistence.hibernate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmCount;
import monasca.api.domain.model.alarm.AlarmRepo;
import monasca.common.hibernate.db.AlarmDb;
import monasca.common.hibernate.db.SubAlarmDb;
import monasca.common.hibernate.type.BinaryId;
import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import monasca.common.util.Conversions;

/**
 * Alarmed metric repository implementation.
 */
public class AlarmSqlRepoImpl
    extends BaseSqlRepo
    implements AlarmRepo {

  private static final Logger logger = LoggerFactory.getLogger(AlarmSqlRepoImpl.class);
  private static final Joiner COMMA_JOINER = Joiner.on(",");
  private static final Joiner SPACE_JOINER = Joiner.on(" ");
  private static final Splitter SPACE_SPLITTER = Splitter.on(" ");
  private static final AlarmSortByFunction ALARM_SORT_BY_FUNCTION = new AlarmSortByFunction();

  private static final String FIND_ALARM_BY_ID_SQL =
      "select distinct ad.id as alarm_definition_id, ad.severity, ad.name as alarm_definition_name, "
          + "a.id, a.state, a.updatedAt, a.createdAt as created_timestamp, "
          + "md.name as metric_name, mdg.id.name, mdg.value, a.lifecycleState, a.link, a.stateUpdatedAt, "
          + "mdg.id.dimensionSetId from AlarmDb as a "
          + ", AlarmDefinitionDb as ad "
          + ", AlarmMetricDb as am "
          + ", MetricDefinitionDimensionsDb as mdd "
          + ", MetricDefinitionDb as md "
          + ", MetricDimensionDb as mdg "
          + "where "
          + " ad.id = a.alarmDefinition.id "
          + " and am.alarmMetricId.alarm.id = a.id "
          + " and mdd.id = am.alarmMetricId.metricDefinitionDimensions.id "
          + " and md.id = mdd.metricDefinition.id "
          + " and mdg.id.dimensionSetId = mdd.metricDimensionSetId "
          + " and ad.tenantId = :tenantId "
          + " %s "
          + " and ad.deletedAt is null order by a.id, mdg.id.dimensionSetId %s";

  private static final String FIND_ALARMS_SQL =
      "select ad.id as alarm_definition_id, ad.severity, ad.name as alarm_definition_name, "
      + "a.id, a.state, a.updated_at as updated_timestamp, a.created_at as created_timestamp, "
      + "md.name as metric_name, mdg.name, mdg.value, a.lifecycle_state, a.link, a.state_updated_at as state_updated_timestamp, "
      + "mdg.dimension_set_id "
      + "from alarm as a "
      + "inner join %s as alarm_id_list on alarm_id_list.id = a.id "
      + "inner join alarm_definition ad on ad.id = a.alarm_definition_id "
      + "inner join alarm_metric as am on am.alarm_id = a.id "
      + "inner join metric_definition_dimensions as mdd on mdd.id = am.metric_definition_dimensions_id "
      + "inner join metric_definition as md on md.id = mdd.metric_definition_id "
      + "left outer join (select dimension_set_id, name, value "
      + "from metric_dimension group by dimension_set_id, name, value) as mdg on mdg.dimension_set_id = mdd.metric_dimension_set_id "
      + "%s";

  @Inject
  public AlarmSqlRepoImpl(@Named("orm") SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  @Override
  public void deleteById(String tenantId, String id) {
    logger.trace(ORM_LOG_MARKER, "deleteById(...) entering");

    Transaction tx = null;
    Session session = null;
    try {
      session = sessionFactory.openSession();
      tx = session.beginTransaction();

      final long result = (Long) session
          .createCriteria(AlarmDb.class, "a")
          .createAlias("alarmDefinition", "ad")
          .add(Restrictions.conjunction(
              Restrictions.eq("a.id", id),
              Restrictions.eq("ad.tenantId", tenantId),
              Restrictions.eqProperty("a.alarmDefinition.id", "ad.id"),
              Restrictions.isNull("ad.deletedAt")
          ))
          .setProjection(Projections.count("a.id"))
          .setReadOnly(true)
          .uniqueResult();

      // This will throw an EntityNotFoundException if Alarm doesn't exist or has a different tenant
      // id
      if (result < 1) {
        throw new EntityNotFoundException("No alarm exists for %s", id);
      }

      // delete alarm
      session
          .getNamedQuery(AlarmDb.Queries.DELETE_BY_ID)
          .setString("id", id)
          .executeUpdate();

      tx.commit();
      tx = null;
    } catch (Exception e) {
      this.rollbackIfNotNull(tx);
      throw e;
    } finally {
      if (session != null) {
        session.close();
      }
    }

  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Alarm> find(final String tenantId,
                          final String alarmDefId,
                          final String metricName,
                          final Map<String, String> metricDimensions,
                          final AlarmState state,
                          final List<AlarmSeverity> severities,
                          final String lifecycleState,
                          final String link,
                          final DateTime stateUpdatedStart,
                          final List<String> sortBy,
                          final String offset,
                          final int limit,
                          final boolean enforceLimit) {
    logger.trace(ORM_LOG_MARKER, "find(...) entering");

    Preconditions.checkNotNull(tenantId, "TenantId is required");

    Session session = null;

    List<Alarm> alarms = new LinkedList<>();

    try {
      final Query query;

      final String sortByClause = ALARM_SORT_BY_FUNCTION.apply(sortBy);
      final String alarmsSubQuery = this.getFindAlarmsSubQuery(
          alarmDefId,
          metricName,
          metricDimensions,
          state,
          severities,
          lifecycleState,
          link,
          stateUpdatedStart,
          sortBy,
          offset,
          limit,
          enforceLimit
      );

      final String sql = String.format(FIND_ALARMS_SQL, alarmsSubQuery, sortByClause);

      try {
        query = new Function<Session, Query>(){

          @Nullable
          @Override
          public Query apply(@Nullable final Session input) {
            assert input != null;
            final Query query = input.createSQLQuery(sql)
                .setReadOnly(true);

            query.setString("tenantId", tenantId);

            if (alarmDefId != null) {
              query.setString("alarmDefId", alarmDefId);
            }

            if (metricName != null) {
              query.setString("metricName", metricName);
            }

            if (state != null) {
              query.setString("state", state.name());
            }

            if (CollectionUtils.isNotEmpty(severities)) {
              if (severities.size() == 1) {
                query.setString("severity", severities.get(0).name());
              } else {
                for (int it = 0; it < severities.size(); it++) {
                  query.setString(String.format("severity_%d", it), severities.get(it).name());
                }
              }
            }

            if (link != null) {
              query.setString("link", link);
            }

            if (lifecycleState != null) {
              query.setString("lifecycleState", lifecycleState);
            }

            if (stateUpdatedStart != null) {
              query.setDate("stateUpdatedStart", stateUpdatedStart.toDateTime(DateTimeZone.UTC).toDate());
            }

            if (enforceLimit && limit > 0) {
              query.setInteger("limit", limit + 1);
            }

            bindDimensionsToQuery(query, metricDimensions);

            return query;
          }

        }.apply((session = sessionFactory.openSession()));
      } catch (Exception e) {
        logger.error("Failed to bind query {}, error is {}", sql, e.getMessage());
        throw new RuntimeException("Failed to bind query", e);
      }

      List<Object[]> alarmList = (List<Object[]>) query.list();

      if(alarmList.isEmpty()){
        return Collections.emptyList();
      }

      alarms = createAlarms(alarmList);

    } finally {
      if (session != null) {
        session.close();
      }
    }
    return alarms;

  }

  private String getFindAlarmsSubQuery(final String alarmDefId,
                                       final String metricName,
                                       final Map<String, String> metricDimensions,
                                       final AlarmState state,
                                       final List<AlarmSeverity> severities,
                                       final String lifecycleState,
                                       final String link,
                                       final DateTime stateUpdatedStart,
                                       final List<String> sortBy,
                                       final String offset,
                                       final int limit,
                                       final boolean enforceLimit) {
    final StringBuilder
        sbWhere =
        new StringBuilder("(select distinct a.id "
            + "from alarm as a, alarm_definition as ad "
            + "where ad.id = a.alarm_definition_id "
            + "  and ad.deleted_at is null "
            + "  and ad.tenant_id = :tenantId ");

    if (alarmDefId != null) {
      sbWhere.append(" and ad.id = :alarmDefId ");
    }

    if (metricName != null) {

      sbWhere.append(" and a.id in (select distinct a.id from alarm as a "
          + "inner join alarm_metric as am on am.alarm_id = a.id "
          + "inner join metric_definition_dimensions as mdd "
          + "  on mdd.id = am.metric_definition_dimensions_id "
          + "inner join (select distinct id from metric_definition "
          + "            where name = :metricName) as md "
          + "  on md.id = mdd.metric_definition_id ");

      buildJoinClauseFor(metricDimensions, sbWhere);

      sbWhere.append(")");

    } else if (metricDimensions != null) {

      sbWhere.append(" and a.id in (select distinct a.id from alarm as a "
          + "inner join alarm_metric as am on am.alarm_id = a.id "
          + "inner join metric_definition_dimensions as mdd "
          + "  on mdd.id = am.metric_definition_dimensions_id ");

      buildJoinClauseFor(metricDimensions, sbWhere);

      sbWhere.append(")");

    }

    if (state != null) {
      sbWhere.append(" and a.state = :state");
    }

    if (CollectionUtils.isNotEmpty(severities)) {
      if (severities.size() == 1) {
        sbWhere.append(" and ad.severity = :severity");
      } else {
        sbWhere.append(" and (");
        for (int i = 0; i < severities.size(); i++) {
          sbWhere.append("ad.severity = :severity_").append(i);
          if (i < severities.size() - 1) {
            sbWhere.append(" or ");
          }
        }
        sbWhere.append(")");
      }
    }

    if (lifecycleState != null) {
      sbWhere.append(" and a.lifecycle_state = :lifecycleState");
    }

    if (link != null) {
      sbWhere.append(" and a.link = :link");
    }

    if (stateUpdatedStart != null) {
      sbWhere.append(" and a.state_updated_at >= :stateUpdatedStart");
    }

    if (enforceLimit && limit > 0) {
      sbWhere.append(" limit :limit");
    }
    if (offset != null) {
      sbWhere.append(" offset ");
      sbWhere.append(offset);
      sbWhere.append(' ');
    }

    sbWhere.append(")");

    return sbWhere.toString();
  }

  private List<Alarm> createAlarms(List<Object[]> alarmList) {
    List<Alarm> alarms = Lists.newLinkedList();

    String previousAlarmId = null;
    BinaryId previousDimensionSetId = null;
    List<MetricDefinition> alarmedMetrics = null;
    Map<String, String> dimensionMap = new HashMap<>();

    for (Object[] alarmRow : alarmList) {
      String alarmDefinitionId = (String) alarmRow[0];
      AlarmSeverity severity = Conversions.variantToEnum(alarmRow[1], AlarmSeverity.class);
      AlarmState alarmState = Conversions.variantToEnum(alarmRow[4], AlarmState.class);
      DateTime updatedTimestamp = Conversions.variantToDateTime(alarmRow[5]);
      DateTime createdTimestamp = Conversions.variantToDateTime(alarmRow[6]);
      BinaryId dimensionSetId = this.convertBinaryId(alarmRow[13]);
      DateTime stateUpdatedTimestamp = Conversions.variantToDateTime(alarmRow[12]);

      String alarm_definition_name = (String) alarmRow[2];
      String id = (String) alarmRow[3];

      String lifecycle_state = (String) alarmRow[10];
      String link = (String) alarmRow[11];

      String metric_name = (String) alarmRow[7];
      String dimension_name = (String) alarmRow[8];
      String dimension_value = (String) alarmRow[9];

      if (!id.equals(previousAlarmId)) {
        alarmedMetrics = new ArrayList<>();
        dimensionMap = Maps.newHashMap();
        alarmedMetrics.add(new MetricDefinition(metric_name, dimensionMap));

        alarms.add(new Alarm(id, alarmDefinitionId, alarm_definition_name, severity.name(),
            alarmedMetrics, alarmState, lifecycle_state, link,
            stateUpdatedTimestamp, updatedTimestamp, createdTimestamp
        ));

        previousDimensionSetId = dimensionSetId;
      }

      if (!dimensionSetId.equals(previousDimensionSetId)) {
        dimensionMap = Maps.newHashMap();
        alarmedMetrics.add(new MetricDefinition(metric_name, dimensionMap));
      }

      dimensionMap.put(dimension_name, dimension_value);

      previousDimensionSetId = dimensionSetId;
      previousAlarmId = id;
    }
    return alarms;
  }

  private BinaryId convertBinaryId(final Object o) {
    final BinaryId dimensionSetId;
    if (o instanceof BinaryId) {
      dimensionSetId = (BinaryId) o;
    } else {
      dimensionSetId = new BinaryId((byte[]) o);
    }
    return dimensionSetId;
  }

  private void bindDimensionsToQuery(Query query, Map<String, String> dimensions) {
    if (dimensions != null) {
      int i = 0;
      for (Iterator<Map.Entry<String, String>> it = dimensions.entrySet().iterator(); it.hasNext(); i++) {
        Map.Entry<String, String> entry = it.next();
        query.setString("dname" + i, entry.getKey());
        query.setString("dvalue" + i, entry.getValue());
      }
    }
  }

  private void buildJoinClauseFor(Map<String, String> dimensions, StringBuilder sbJoin) {
    if (dimensions == null) {
      return;
    }
    for (int i = 0; i < dimensions.size(); i++) {
      final String indexStr = String.valueOf(i);
      sbJoin.append(" inner join metric_dimension md").append(indexStr).append(" on md")
          .append(indexStr)
          .append(".name = :dname").append(indexStr).append(" and md").append(indexStr)
          .append(".value = :dvalue").append(indexStr)
          .append(" and mdd.metric_dimension_set_id = md")
          .append(indexStr).append(".dimension_set_id");
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Alarm findById(String tenantId, String id) {
    logger.trace(ORM_LOG_MARKER, "findById(...) entering");

    Session session = null;

    final String sql = String.format(FIND_ALARM_BY_ID_SQL, " and a.id = :id", "");
    List<Alarm> alarms = new LinkedList<>();
    try {
      session = sessionFactory.openSession();
      Query qAlarmDefinition =
          session.createQuery(sql).setString("tenantId", tenantId)
              .setString("id", id);
      List<Object[]> alarmList = (List<Object[]>) qAlarmDefinition.list();

      if (alarmList.isEmpty()) {
        throw new EntityNotFoundException("No alarm exists for %s", id);
      }

      alarms = this.createAlarms(alarmList);

    } finally {
      if (session != null) {
        session.close();
      }
    }
    return alarms.get(0);
  }

  @Override
  public Alarm update(String tenantId, String id, AlarmState state, String lifecycleState, String link) {
    Session session = null;
    Alarm originalAlarm = null;
    Transaction tx = null;
    try {
      session = sessionFactory.openSession();
      tx = session.beginTransaction();
      originalAlarm = findById(tenantId, id);

      AlarmDb result = (AlarmDb) session
          .getNamedQuery(AlarmDb.Queries.FIND_BY_ID)
          .setString("id", id)
          .uniqueResult();

      if (!originalAlarm.getState().equals(state)) {
        result.setStateUpdatedAt(this.getUTCNow());
        result.setState(state);
      }

      result.setUpdatedAt(this.getUTCNow());
      result.setLink(link);
      result.setLifecycleState(lifecycleState);
      session.update(result);

      tx.commit();
      tx = null;
    } catch (Exception e) {
      this.rollbackIfNotNull(tx);
      throw e;
    } finally {
      if (session != null) {
        session.close();
      }
    }
    return originalAlarm;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, AlarmSubExpression> findAlarmSubExpressions(String alarmId) {
    Session session = null;
    final Map<String, AlarmSubExpression> subAlarms = Maps.newHashMap();
    logger.debug("AlarmSqlRepoImpl[findAlarmSubExpressions] called");
    try {

      session = sessionFactory.openSession();
      final List<SubAlarmDb> result = session
          .getNamedQuery(SubAlarmDb.Queries.BY_ALARM_ID)
          .setString("id", alarmId)
          .list();

      if (result != null) {
        for (SubAlarmDb row : result) {
          subAlarms.put(row.getId(), AlarmSubExpression.of(row.getExpression()));
        }
      }
    } finally {
      if (session != null) {
        session.close();
      }
    }
    return subAlarms;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Map<String, AlarmSubExpression>> findAlarmSubExpressionsForAlarmDefinition(
      String alarmDefinitionId) {
    logger.trace(ORM_LOG_MARKER, "findAlarmSubExpressionsForAlarmDefinition(...) entering");

    Session session = null;
    Transaction tx = null;
    Map<String, Map<String, AlarmSubExpression>> subAlarms = Maps.newHashMap();

    try {
      session = sessionFactory.openSession();
      tx = session.beginTransaction();

      final Iterator<SubAlarmDb> rows = session
          .getNamedQuery(SubAlarmDb.Queries.BY_ALARMDEFINITION_ID)
          .setString("id", alarmDefinitionId)
          .setReadOnly(true)
          .iterate();

      while (rows.hasNext()) {

        final SubAlarmDb row = rows.next();
        final String alarmId = (String) session.getIdentifier(row.getAlarm());

        Map<String, AlarmSubExpression> alarmMap = subAlarms.get(alarmId);
        if (alarmMap == null) {
          alarmMap = Maps.newHashMap();
          subAlarms.put(alarmId, alarmMap);
        }

        final String id = row.getId();
        final String expression = row.getExpression();
        alarmMap.put(id, AlarmSubExpression.of(expression));
      }

      tx.commit();
      tx = null;

    } catch (Exception exp) {
      this.rollbackIfNotNull(tx);
      throw exp;
    } finally {
      if (session != null) {
        session.close();
      }
    }

    return subAlarms;
  }

  @Override
  public AlarmCount getAlarmsCount(String tenantId, String alarmDefId, String metricName,
                                   Map<String, String> metricDimensions, AlarmState state,
                                   List<AlarmSeverity> severities, String lifecycleState, String link,
                                   DateTime stateUpdatedStart, List<String> groupBy,
                                   String offset, int limit) {
    // Not Implemented
    return null;
  }

  private static class AlarmSortByFunction
      implements Function<List<String>, String> {

    static final Map<String, List<String>> SORT_BY_TO_COLUMN_ALIAS = Maps.newHashMapWithExpectedSize(10);

    static {
      SORT_BY_TO_COLUMN_ALIAS.put("alarm_id",
          Lists.newArrayList("a.id"));
      SORT_BY_TO_COLUMN_ALIAS.put("alarm_definition_id",
          Lists.newArrayList("ad.id"));
      SORT_BY_TO_COLUMN_ALIAS.put("alarm_definition_name",
          Lists.newArrayList("ad.name"));
      SORT_BY_TO_COLUMN_ALIAS.put("created_timestamp",
          Lists.newArrayList("a.created_at"));
      SORT_BY_TO_COLUMN_ALIAS.put("updated_timestamp",
          Lists.newArrayList("a.updated_at"));
      SORT_BY_TO_COLUMN_ALIAS.put("state_updated_timestamp",
          Lists.newArrayList("a.state_updated_at"));
      SORT_BY_TO_COLUMN_ALIAS.put("state",
          Lists.newArrayList("a.state='OK'", "a.state='UNDETERMINED'", "a.state='ALARM'"));
      SORT_BY_TO_COLUMN_ALIAS.put("severity",
          Lists.newArrayList("ad.severity='LOW'", "ad.severity='MEDIUM'", "ad.severity='HIGH'", "ad.severity='CRITICAL'"));
    }

    @Nullable
    @Override
    public String apply(@Nullable final List<String> input) {
      final StringBuilder orderClause = new StringBuilder(" ORDER BY ");

      if (CollectionUtils.isEmpty(input)) {
        return orderClause.append("a.id ASC ").toString();
      }

      final List<String> sortByElements = Lists.newArrayListWithExpectedSize(input.size());
      boolean alarmIdUsed = false;

      for (final String sortByElement : input) {
        final List<String> split = SPACE_SPLITTER.splitToList(sortByElement);

        final String sortAlias = split.get(0);
        final String sortOrder = split.size() >= 2 ? split.get(1).toUpperCase() : "";

        final List<String> columnAlias = SORT_BY_TO_COLUMN_ALIAS.get(sortAlias);
        alarmIdUsed = "alarm_id".equals(sortAlias);

        if (columnAlias != null) {
          sortByElements.add(new CaseWhenSortClauseFunction(sortOrder).apply(columnAlias));
        }

      }

      if (!alarmIdUsed) {
        sortByElements.add("a.id ASC");
      }

      orderClause.append(COMMA_JOINER.join(sortByElements));

      return orderClause.toString();
    }

  }

  private static final class CaseWhenSortClauseFunction
      implements Function<List<String>, String> {

    private final String sortOrder;

    CaseWhenSortClauseFunction(final String sortOrder) {
      this.sortOrder = sortOrder;
    }

    @Nullable
    @Override
    public String apply(@Nullable final List<String> input) {
      assert input != null;
      if (input.size() == 1) {
        return String.format("%s %s", input.get(0), this.sortOrder).trim();
      } else {

        final List<String> builder = Lists.newArrayList("CASE");
        final boolean ascendingOrder = this.isAscendingOrder();

        for (int it = 0; it < input.size(); it++) {
          final String[] parts = input.get(it).split("=");

          final String columnName = parts[0];
          final String columnValue = parts[1];
          final int orderValue = ascendingOrder ? it : input.size() - it - 1;

          builder.add(String.format("WHEN %s=%s THEN %s",
              columnName, columnValue, orderValue));

        }

        builder.add(String.format("ELSE %s", input.size()));
        builder.add("END");

        return SPACE_JOINER.join(builder);
      }
    }

    private boolean isAscendingOrder() {
      return "".equals(this.sortOrder) || "ASC".equals(this.sortOrder);
    }

  }

}
