/*
 * Copyright 2015 FUJITSU LIMITED
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

  private static final String ALARM_SQL =
      "select distinct ad.id as alarm_definition_id, ad.severity, ad.name as alarm_definition_name, "
          + "a.id, a.state, a.updated_at, a.created_at as created_timestamp, "
          + "md.name as metric_name, mdg.name, mdg.value, a.lifecycle_state, a.link, a.state_updated_at, "
          + "mdg.dimension_set_id "
          + "from alarm as a "
          + "inner join alarm_definition ad on ad.id = a.alarm_definition_id "
          + "inner join alarm_metric as am on am.alarm_id = a.id "
          + "inner join metric_definition_dimensions as mdd on mdd.id = am.metric_definition_dimensions_id "
          + "inner join metric_definition as md on md.id = mdd.metric_definition_id "
          + "left join (select dimension_set_id, name, value "
          + "from metric_dimension group by dimension_set_id, name, value) as mdg on mdg.dimension_set_id = mdd.metric_dimension_set_id "
          + "where ad.tenant_id = :tenantId and ad.deleted_at is null %s order by a.id ASC, mdg.dimension_set_id %s ";

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

  @Override
  public List<Alarm> find(String tenantId, String alarmDefId, String metricName,
                          Map<String, String> metricDimensions, AlarmState state,
                          String lifecycleState, String link, DateTime stateUpdatedStart,
                          String offset, int limit, boolean enforceLimit) {
    logger.trace(ORM_LOG_MARKER, "find(...) entering");

    List<Alarm> alarms = this.findInternal(tenantId, alarmDefId, metricName, metricDimensions, state,
        lifecycleState, link, stateUpdatedStart, offset, (3 * limit / 2), enforceLimit);

    if (limit == 0 || !enforceLimit)
      return alarms;

    if (alarms.size() > limit) {
      for (int i = alarms.size() - 1; i > limit; i--) {
        alarms.remove(i);
      }
    } else if (alarms.size() > 0) {
      while (alarms.size() < limit) {
        List<Alarm> alarms2;
        int diff = limit - alarms.size();
        String offset2 = alarms.get(alarms.size() - 1).getId();
        alarms2 = this.findInternal(tenantId, alarmDefId, metricName, metricDimensions, state,
            lifecycleState, link, stateUpdatedStart, offset2, (2 * diff), enforceLimit);
        if (alarms2.size() == 0)
          break;
        for (int i = 0; i < alarms2.size() && i < diff; i++)
          alarms.add(alarms2.get(i));
      }
    }

    return alarms;
  }

  private List<Alarm> findInternal(String tenantId, String alarmDefId, String metricName,
                                   Map<String, String> metricDimensions, AlarmState state,
                                   String lifecycleState, String link, DateTime stateUpdatedStart,
                                   String offset, int limit, boolean enforceLimit) {
    Session session = null;

    List<Alarm> alarms = new LinkedList<>();

    try {
      Query query;
      session = sessionFactory.openSession();

      StringBuilder sbWhere = new StringBuilder();

      if (alarmDefId != null) {
        sbWhere.append("and ad.id = :alarmDefId ");
      }
      if (metricName != null) {
        sbWhere.append(" and a.id in (select distinct a.id from alarm as a "
                       + "inner join alarm_metric as am on am.alarm_id = a.id "
                       + "inner join metric_definition_dimensions as mdd "
                       + "  on mdd.id = am.metric_definition_dimensions_id "
                       + "inner join (select distinct id from metric_definition "
                       + "            where name = :metricName) as md "
                       + "on md.id = mdd.metric_definition_id ");

        buildJoinClauseFor(metricDimensions, sbWhere);

        sbWhere.append(")");

      }
      if (state != null) {
        sbWhere.append(" and a.state = :state");
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
      if (offset != null) {
        sbWhere.append(" and a.id > :offset");
      }

      String limitPart = "";
      if (enforceLimit && limit > 0) {
        limitPart = " limit :limit";
      }

      String sql = String.format(ALARM_SQL, sbWhere, limitPart);

      try {
        query = session.createSQLQuery(sql);
      } catch (Exception e) {
        logger.error("Failed to bind query {}, error is {}", sql, e.getMessage());
        throw new RuntimeException("Failed to bind query", e);
      }

      query.setString("tenantId", tenantId);
      if (alarmDefId != null) {
        query.setString("alarmDefId", alarmDefId);
      }

      if (offset != null) {
        query.setString("offset", offset);
      }

      if (metricName != null) {
        query.setString("metricName", metricName);
      }

      if (state != null) {
        query.setString("state", state.name());
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
      if (metricName != null) {
        bindDimensionsToQuery(query, metricDimensions);
      }

      List<Object[]> alarmList = (List<Object[]>) query.list();
      alarms = createAlarms(alarmList);

    } finally {
      if (session != null) {
        session.close();
      }
    }
    return alarms;
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

  private void bindDimensionsToQuery(
      Query query,
      Map<String, String> dimensions) {

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
}
