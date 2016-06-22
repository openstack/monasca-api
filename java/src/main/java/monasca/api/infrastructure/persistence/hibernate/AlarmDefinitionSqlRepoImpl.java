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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepo;
import monasca.api.infrastructure.persistence.SubAlarmDefinitionQueries;
import monasca.common.hibernate.db.AlarmActionDb;
import monasca.common.hibernate.db.AlarmDb;
import monasca.common.hibernate.db.AlarmDefinitionDb;
import monasca.common.hibernate.db.SubAlarmDefinitionDb;
import monasca.common.hibernate.db.SubAlarmDefinitionDimensionDb;
import monasca.common.hibernate.db.SubAlarmDefinitionDimensionId;
import monasca.common.model.alarm.AggregateFunction;
import monasca.common.model.alarm.AlarmOperator;
import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;

/**
 * Alarm repository implementation.
 */
public class AlarmDefinitionSqlRepoImpl
    extends BaseSqlRepo
    implements AlarmDefinitionRepo {
  private static final ResultTransformer ALARM_DEF_RESULT_TRANSFORMER = getAlarmDefResultTransformer();
  private static final String ID = "ID";
  private static final String NAME = "NAME";
  private static final String DESCRIPTION = "DESCRIPTION";
  private static final String EXPRESSION = "EXPRESSION";
  private static final String SEVERITY = "SEVERITY";
  private static final String MATCH_BY = "MATCH_BY";
  private static final String ACTIONS_ENABLED = "ACTIONS_ENABLED";
  private static final String STATE = "STATE";
  private static final String NOTIFICATION_ID = "NOTIFICATIONIDS";
  private static final Joiner COMMA_JOINER = Joiner.on(',');
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();
  private static final Logger logger = LoggerFactory.getLogger(AlarmDefinitionSqlRepoImpl.class);
  private static final String FIND_ALARM_DEF_SQL = "SELECT t.id, t.tenant_id, t.name, "
      + "t.description, t.expression, t.severity, t.match_by, "
      + "t.actions_enabled, aa.alarm_state AS state, aa.action_id AS notificationIds "
      + "FROM (SELECT distinct ad.id, ad.tenant_id, ad.name, ad.description, ad.expression, "
      + "ad.severity, ad.match_by, ad.actions_enabled, ad.created_at, ad.updated_at, ad.deleted_at "
      + "FROM alarm_definition AS ad LEFT OUTER JOIN sub_alarm_definition AS sad ON ad.id = sad.alarm_definition_id "
      + "LEFT OUTER JOIN sub_alarm_definition_dimension AS dim ON sad.id = dim.sub_alarm_definition_id %1$s "
      + "WHERE ad.tenant_id = :tenantId AND ad.deleted_at IS NULL %2$s ORDER BY ad.id %3$s) AS t "
      + "LEFT OUTER JOIN alarm_action AS aa ON t.id = aa.alarm_definition_id %4$s";

  @Inject
  public AlarmDefinitionSqlRepoImpl(@Named("orm") SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  @Override
  public AlarmDefinition create(String tenantId, String id, String name, String description, String severity, String expression,
                                Map<String, AlarmSubExpression> subExpressions, List<String> matchBy, List<String> alarmActions, List<String> okActions,
                                List<String> undeterminedActions) {
    logger.trace(ORM_LOG_MARKER, "create(...) entering...");

    Transaction tx = null;
    Session session = null;
    try {
      session = sessionFactory.openSession();
      tx = session.beginTransaction();

      final DateTime now = this.getUTCNow();
      final AlarmDefinitionDb alarmDefinition = new AlarmDefinitionDb(
          id,
          tenantId,
          name,
          description,
          expression,
          AlarmSeverity.valueOf(severity.toUpperCase()),
          matchBy == null || Iterables.isEmpty(matchBy) ? null : COMMA_JOINER.join(matchBy),
          true,
          now,
          now,
          null
      );
      session.save(alarmDefinition);

      this.createSubExpressions(session, alarmDefinition, subExpressions);

      // Persist actions
      this.persistActions(session, alarmDefinition, AlarmState.ALARM, alarmActions);
      this.persistActions(session, alarmDefinition, AlarmState.OK, okActions);
      this.persistActions(session, alarmDefinition, AlarmState.UNDETERMINED, undeterminedActions);

      tx.commit();
      tx = null;

      logger.debug(ORM_LOG_MARKER, "AlarmDefinition [ {} ] has been committed to database", alarmDefinition);

      return new AlarmDefinition(
          id,
          name,
          description,
          severity,
          expression,
          matchBy,
          true,
          alarmActions == null ? Collections.<String>emptyList() : alarmActions,
          okActions == null ? Collections.<String>emptyList() : okActions,
          undeterminedActions == null ? Collections.<String>emptyList() : undeterminedActions
      );

    } catch (RuntimeException e) {
      this.rollbackIfNotNull(tx);
      throw e;
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Override
  public void deleteById(String tenantId, String alarmDefId) {
    logger.trace(ORM_LOG_MARKER, "deleteById(...) entering...");

    Session session = null;
    Transaction tx = null;
    try {
      session = sessionFactory.openSession();
      tx = session.beginTransaction();

      final AlarmDefinitionDb result = (AlarmDefinitionDb) session
          .getNamedQuery(AlarmDefinitionDb.Queries.FIND_BY_TENANT_AND_ID_NOT_DELETED)
          .setString("tenant_id", tenantId)
          .setString("id", alarmDefId)
          .uniqueResult();

      result.setDeletedAt(this.getUTCNow());
      session.update(result);

      // Cascade soft delete to alarms
      session
          .getNamedQuery(AlarmDb.Queries.DELETE_BY_ALARMDEFINITION_ID)
          .setString("alarmDefinitionId", alarmDefId)
          .executeUpdate();

      tx.commit();
      tx = null;

      logger.debug(ORM_LOG_MARKER, "AlarmDefinition [ {} ] has been deleted from database", result);

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
  public String exists(final String tenantId,
                       final String name) {
    logger.trace(ORM_LOG_MARKER, "exists(...) entering...");

    StatelessSession session = null;
    try {
      session = sessionFactory.openStatelessSession();

      List<?> ids = session
          .createCriteria(AlarmDefinitionDb.class)
          .add(Restrictions.eq("tenantId", tenantId))
          .add(Restrictions.eq("name", name))
          .add(Restrictions.isNull("deletedAt"))
          .setProjection(Projections.property("id"))
          .setMaxResults(1)
          .list();

      final String existingId = CollectionUtils.isEmpty(ids) ? null : (String) ids.get(0);

      if (null == existingId) {
        logger.debug(ORM_LOG_MARKER, "No AlarmDefinition matched tenantId={} and name={}", tenantId, name);
      }

      return existingId;
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<AlarmDefinition> find(String tenantId, String name, Map<String, String> dimensions,
                                    List<AlarmSeverity> severities, List<String> sortBy,
                                    String offset, int limit) {
    logger.trace(ORM_LOG_MARKER, "find(...) entering...");

    Session session = null;
    List<AlarmDefinition> resultSet = Lists.newArrayList();

    final StringBuilder sbWhere = new StringBuilder();
    final StringBuilder limitOffset = new StringBuilder();
    final StringBuilder orderByPart = new StringBuilder();

    if (name != null) {
      sbWhere.append(" and ad.name = :name");
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

    if (limit > 0) {
      limitOffset.append(" limit :limit");
    }

    if (offset != null) {
      limitOffset.append(" offset :offset ");
    }

    if (sortBy != null && !sortBy.isEmpty()) {
      orderByPart.append(" order by ").append(COMMA_JOINER.join(sortBy));
      if (!sortBy.contains("id")) {
        orderByPart.append(",id");
      }
    } else {
      orderByPart.append(" order by id ");
    }

    final String sql = String.format(
        FIND_ALARM_DEF_SQL,
        SubAlarmDefinitionQueries.buildJoinClauseFor(dimensions),
        sbWhere,
        limitOffset,
        orderByPart
    );

    try {
      session = sessionFactory.openSession();

      final Query qAlarmDefinition = session
          .createSQLQuery(sql)
          .setString("tenantId", tenantId)
          .setReadOnly(true)
          .setResultTransformer(ALARM_DEF_RESULT_TRANSFORMER);

      if (name != null) {
        qAlarmDefinition.setString("name", name);
      }

      if (CollectionUtils.isNotEmpty(severities)) {
        if (severities.size() == 1) {
          qAlarmDefinition.setString("severity", severities.get(0).name());
        } else {
          for (int it = 0; it < severities.size(); it++) {
            qAlarmDefinition.setString(String.format("severity_%d", it), severities.get(it).name());
          }
        }
      }

      if (limit > 0) {
        qAlarmDefinition.setInteger("limit", limit + 1);
      }

      if (offset != null) {
        qAlarmDefinition.setInteger("offset", Integer.parseInt(offset));
      }

      this.bindDimensionsToQuery(qAlarmDefinition, dimensions);

      final List<Map<?,?>> alarmDefinitionDbList = qAlarmDefinition.list();

      resultSet = CollectionUtils.isEmpty(alarmDefinitionDbList) ?
          Lists.<AlarmDefinition>newArrayList() :
          this.createAlarmDefinitions(alarmDefinitionDbList);

    } finally {
      if (session != null) {
        session.close();
      }
    }

    return resultSet;
  }

  @Override
  @SuppressWarnings("unchecked")
  public AlarmDefinition findById(String tenantId, String alarmDefId) {
    logger.trace(ORM_LOG_MARKER, "findById(...) entering...");

    Session session = null;
    List<String> okActionIds = null;
    List<String> alarmActionIds = null;
    List<String> undeterminedActionIds = null;

    try {
      session = sessionFactory.openSession();

      final AlarmDefinitionDb alarmDefinitionDb = (AlarmDefinitionDb) session
          .getNamedQuery(AlarmDefinitionDb.Queries.FIND_BY_TENANT_AND_ID_NOT_DELETED)
          .setString("tenant_id", tenantId)
          .setString("id", alarmDefId).uniqueResult();

      if (alarmDefinitionDb == null) {
        throw new EntityNotFoundException("No alarm definition exists for tenantId=%s and id=%s", tenantId, alarmDefId);
      }

      final List<AlarmActionDb> alarmActionList = session
          .getNamedQuery(AlarmActionDb.Queries.FIND_BY_TENANT_ID_AND_ALARMDEFINITION_ID_DISTINCT)
          .setString("tenantId", tenantId)
          .setString("alarmDefId", alarmDefId)
          .list();

      if(!CollectionUtils.isEmpty(alarmActionList)) {

        logger.debug(ORM_LOG_MARKER, "Located {} AlarmActions for AlarmDefinition {}", alarmActionList.size(), alarmDefinitionDb);

        okActionIds = Lists.newArrayList();
        alarmActionIds = Lists.newArrayList();
        undeterminedActionIds = Lists.newArrayList();

        for (final AlarmActionDb alarmAction : alarmActionList) {
          if (alarmAction.isInAlarmState(AlarmState.UNDETERMINED)) {
            undeterminedActionIds.add(alarmAction.getAlarmActionId().getActionId());
          } else if (alarmAction.isInAlarmState(AlarmState.OK)) {
            okActionIds.add(alarmAction.getAlarmActionId().getActionId());
          } else if (alarmAction.isInAlarmState(AlarmState.ALARM)) {
            alarmActionIds.add(alarmAction.getAlarmActionId().getActionId());
          }
        }

      }

      return new AlarmDefinition(
          alarmDefinitionDb.getId(),
          alarmDefinitionDb.getName(),
          alarmDefinitionDb.getDescription(),
          alarmDefinitionDb.getSeverity().name(),
          alarmDefinitionDb.getExpression(),
          this.splitStringIntoList(alarmDefinitionDb.getMatchBy()),
          alarmDefinitionDb.isActionsEnabled(),
          alarmActionIds == null ? Collections.<String>emptyList() : alarmActionIds,
          okActionIds == null ? Collections.<String>emptyList() : okActionIds,
          undeterminedActionIds == null ? Collections.<String>emptyList() : undeterminedActionIds
      );

    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, MetricDefinition> findSubAlarmMetricDefinitions(String alarmDefId) {
    logger.trace(ORM_LOG_MARKER, "findSubAlarmMetricDefinitions(...) entering...");

    Session session = null;
    Map<String, MetricDefinition> subAlarmMetricDefs = Maps.newHashMap();

    try {

      session = sessionFactory.openSession();

      final List<SubAlarmDefinitionDb> subAlarmDefList = session
          .getNamedQuery(SubAlarmDefinitionDb.Queries.BY_ALARMDEFINITION_ID)
          .setString("id", alarmDefId)
          .list();
      final List<SubAlarmDefinitionDimensionDb> subAlarmDefDimensionList = session
          .getNamedQuery(SubAlarmDefinitionDb.Queries.BY_ALARMDEFINITIONDIMENSION_SUBEXPRESSION_ID)
          .setString("id", alarmDefId)
          .list();

      final Map<String, Map<String, String>> subAlarmDefDimensionMapExpression = this.mapAlarmDefDimensionExpression(
          subAlarmDefDimensionList
      );

      for (SubAlarmDefinitionDb subAlarmDef : subAlarmDefList) {
        String id = subAlarmDef.getId();
        String metricName = subAlarmDef.getMetricName();
        Map<String, String> dimensions = Collections.emptyMap();
        if (subAlarmDefDimensionMapExpression.containsKey(id)) {
          dimensions = subAlarmDefDimensionMapExpression.get(id);
        }
        subAlarmMetricDefs.put(id, new MetricDefinition(metricName, dimensions));
      }
      return subAlarmMetricDefs;

    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, AlarmSubExpression> findSubExpressions(String alarmDefId) {
    logger.trace(ORM_LOG_MARKER, "findSubExpressions(...) entering...");

    Session session = null;
    Map<String, AlarmSubExpression> subExpressions = Maps.newHashMap();
    try {

      session = sessionFactory.openSession();

      List<SubAlarmDefinitionDb> subAlarmDefList = session
          .getNamedQuery(SubAlarmDefinitionDb.Queries.BY_ALARMDEFINITION_ID)
          .setString("id", alarmDefId)
          .list();

      Query querySybAlarmDefDimension = session
          .getNamedQuery(SubAlarmDefinitionDb.Queries.BY_ALARMDEFINITIONDIMENSION_SUBEXPRESSION_ID)
          .setString("id", alarmDefId);

      List<SubAlarmDefinitionDimensionDb> subAlarmDefDimensionList = querySybAlarmDefDimension.list();

      Map<String, Map<String, String>> subAlarmDefDimensionMapExpression = mapAlarmDefDimensionExpression(subAlarmDefDimensionList);

      for (SubAlarmDefinitionDb subAlarmDef : subAlarmDefList) {
        String id = subAlarmDef.getId();
        AggregateFunction function = AggregateFunction.fromJson(subAlarmDef.getFunction());
        String metricName = subAlarmDef.getMetricName();
        AlarmOperator operator = AlarmOperator.fromJson(subAlarmDef.getOperator());
        double threshold = subAlarmDef.getThreshold();
        int period = subAlarmDef.getPeriod();
        int periods = subAlarmDef.getPeriods();
        boolean isDeterministic = subAlarmDef.isDeterministic();
        Map<String, String> dimensions = Collections.emptyMap();

        if (subAlarmDefDimensionMapExpression.containsKey(id)) {
          dimensions = subAlarmDefDimensionMapExpression.get(id);
        }

        subExpressions.put(id,
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

    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Override
  public void update(String tenantId, String id, boolean patch, String name, String description, String expression, List<String> matchBy,
                     String severity, boolean actionsEnabled, Collection<String> oldSubAlarmIds, Map<String, AlarmSubExpression> changedSubAlarms,
                     Map<String, AlarmSubExpression> newSubAlarms, List<String> alarmActions, List<String> okActions, List<String> undeterminedActions) {
    logger.trace(ORM_LOG_MARKER, "update(...) entering...");

    Transaction tx = null;
    Session session = null;
    try {
      session = sessionFactory.openSession();
      tx = session.beginTransaction();

      final AlarmDefinitionDb alarmDefinitionDb = this.updateAlarmDefinition(
          tenantId,
          id,
          name,
          description,
          expression,
          matchBy,
          severity,
          actionsEnabled,
          session
      );

      this.deleteOldSubAlarms(oldSubAlarmIds, session);
      this.updateChangedSubAlarms(changedSubAlarms, session);
      this.createSubExpressions(session, alarmDefinitionDb, newSubAlarms);
      this.deleteOldAlarmActions(id, patch, alarmActions, okActions, undeterminedActions, session);

      // Insert new actions
      this.persistActions(session, alarmDefinitionDb, AlarmState.ALARM, alarmActions);
      this.persistActions(session, alarmDefinitionDb, AlarmState.OK, okActions);
      this.persistActions(session, alarmDefinitionDb, AlarmState.UNDETERMINED, undeterminedActions);
      // Insert new actions

      tx.commit();
      tx = null;
    } catch (RuntimeException e) {
      this.rollbackIfNotNull(tx);
      throw e;
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  private void deleteOldAlarmActions(final String id,
                                     final boolean patch,
                                     final List<String> alarmActions,
                                     final List<String> okActions,
                                     final List<String> undeterminedActions,
                                     final Session session) {
    if (patch) {
      this.deleteActions(session, id, AlarmState.ALARM, alarmActions);
      this.deleteActions(session, id, AlarmState.OK, okActions);
      this.deleteActions(session, id, AlarmState.UNDETERMINED, undeterminedActions);
    } else {
      session
          .getNamedQuery(AlarmActionDb.Queries.DELETE_BY_ALARMDEFINITION_ID)
          .setString("id", id)
          .executeUpdate();
    }
  }

  private void updateChangedSubAlarms(final Map<String, AlarmSubExpression> changedSubAlarms,
                                      final Session session) {
    if (!MapUtils.isEmpty(changedSubAlarms))
      for (Map.Entry<String, AlarmSubExpression> entry : changedSubAlarms.entrySet()) {
        final AlarmSubExpression sa = entry.getValue();
        final String subAlarmDefinitionId = entry.getKey();

        SubAlarmDefinitionDb subAlarmDefinitionDb = session.get(SubAlarmDefinitionDb.class, subAlarmDefinitionId);
        subAlarmDefinitionDb.setOperator(sa.getOperator().name());
        subAlarmDefinitionDb.setThreshold(sa.getThreshold());
        subAlarmDefinitionDb.setUpdatedAt(this.getUTCNow());
        subAlarmDefinitionDb.setDeterministic(sa.isDeterministic());
        session.saveOrUpdate(subAlarmDefinitionDb);
      }
  }

  private void deleteOldSubAlarms(final Collection<String> oldSubAlarmIds,
                                  final Session session) {
    if (!CollectionUtils.isEmpty(oldSubAlarmIds)) {
      session
          .getNamedQuery(SubAlarmDefinitionDb.Queries.DELETE_BY_IDS)
          .setParameterList("ids", oldSubAlarmIds)
          .executeUpdate();
    }
  }

  private AlarmDefinitionDb updateAlarmDefinition(final String tenantId,
                                                  final String id,
                                                  final String name,
                                                  final String description,
                                                  final String expression,
                                                  final List<String> matchBy,
                                                  final String severity,
                                                  final boolean actionsEnabled,
                                                  final Session session) {
    final AlarmDefinitionDb alarmDefinitionDb = (AlarmDefinitionDb) session
        .getNamedQuery(AlarmDefinitionDb.Queries.FIND_BY_TENANT_ID_AND_ID)
        .setString("tenantId", tenantId)
        .setString("id", id)
        .uniqueResult();

    alarmDefinitionDb.setName(name);
    alarmDefinitionDb.setDescription(description);
    alarmDefinitionDb.setExpression(expression);
    alarmDefinitionDb.setMatchBy(matchBy == null || Iterables.isEmpty(matchBy) ? null : COMMA_JOINER.join(matchBy));
    alarmDefinitionDb.setSeverity(AlarmSeverity.valueOf(severity));
    alarmDefinitionDb.setActionsEnabled(actionsEnabled);
    alarmDefinitionDb.setUpdatedAt(this.getUTCNow());

    session.saveOrUpdate(alarmDefinitionDb);

    return alarmDefinitionDb;
  }

  private void deleteActions(final Session session,
                             final String id,
                             final AlarmState alarmState,
                             final List<String> actions) {
    if (!CollectionUtils.isEmpty(actions))
      session
          .getNamedQuery(AlarmActionDb.Queries.DELETE_BY_ALARMDEFINITION_ID_AND_ALARMSTATE)
          .setString("id", id)
          .setString("alarmState", alarmState.name())
          .executeUpdate();
  }

  private Map<String, Map<String, String>> mapAlarmDefDimensionExpression(List<SubAlarmDefinitionDimensionDb> subAlarmDefDimensionList) {
    Map<String, Map<String, String>> subAlarmDefDimensionMapExpression = Maps.newHashMapWithExpectedSize(subAlarmDefDimensionList.size());

    // Map expressions on sub_alarm_definition_dimension.sub_alarm_definition_id =
    // sub_alarm_definition.id
    for (SubAlarmDefinitionDimensionDb subAlarmDefDimension : subAlarmDefDimensionList) {
      String subAlarmDefId = subAlarmDefDimension.getSubAlarmDefinitionDimensionId().getSubExpression().getId();
      String name = subAlarmDefDimension.getSubAlarmDefinitionDimensionId().getDimensionName();
      String value = subAlarmDefDimension.getValue();

      if (subAlarmDefDimensionMapExpression.containsKey(subAlarmDefId)) {
        subAlarmDefDimensionMapExpression.get(subAlarmDefId).put(name, value);
      } else {
        Map<String, String> expressionMap = Maps.newHashMap();
        expressionMap.put(name, value);
        subAlarmDefDimensionMapExpression.put(subAlarmDefId, expressionMap);
      }
    }

    return subAlarmDefDimensionMapExpression;
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

  private List<AlarmDefinition> createAlarmDefinitions(List<Map<?,?>> rows) {
    final List<AlarmDefinition> result = new ArrayList<>();
    Map<String, List<String>> okActionIdsMap = Maps.newHashMap();
    Map<String, List<String>> alarmActionIdsMap = Maps.newHashMap();
    Map<String, List<String>> undeterminedActionIdsMap = Maps.newHashMap();
    Set<String> alarmDefinitionSet = Sets.newHashSet();

    for (Map<?,?> row : rows) {

      String alarmDefId = (String) row.get(ID);
      String singleState = (String) row.get(STATE);
      String notificationId = (String) row.get(NOTIFICATION_ID);

      if (!okActionIdsMap.containsKey(alarmDefId)) {
        okActionIdsMap.put(alarmDefId, Lists.<String>newArrayList());
      }
      if (!alarmActionIdsMap.containsKey(alarmDefId)) {
        alarmActionIdsMap.put(alarmDefId, Lists.<String>newArrayList());
      }
      if (!undeterminedActionIdsMap.containsKey(alarmDefId)) {
        undeterminedActionIdsMap.put(alarmDefId, Lists.<String>newArrayList());
      }

      if (singleState != null && notificationId != null) {
        if (singleState.equals(AlarmState.UNDETERMINED.name())) {
          undeterminedActionIdsMap.get(alarmDefId).add(notificationId);
        }
        if (singleState.equals(AlarmState.OK.name())) {
          okActionIdsMap.get(alarmDefId).add(notificationId);
        }
        if (singleState.equals(AlarmState.ALARM.name())) {
          alarmActionIdsMap.get(alarmDefId).add(notificationId);
        }
      }
    }

    for (Map<?,?> row : rows) {
      String alarmDefId = (String) row.get(ID);

      if (!alarmDefinitionSet.contains(alarmDefId)) {
        String name = (String) row.get(NAME);
        String description = (String) row.get(DESCRIPTION);
        String severity = (String) row.get(SEVERITY);
        String expression = (String) row.get(EXPRESSION);
        List<String> match = this.splitStringIntoList((String) row.get(MATCH_BY));
        Boolean actionEnabled = (Boolean) row.get(ACTIONS_ENABLED);

        AlarmDefinition ad = new AlarmDefinition(
            alarmDefId,
            name,
            description,
            severity,
            expression,
            match,
            actionEnabled,
            alarmActionIdsMap.get(alarmDefId),
            okActionIdsMap.get(alarmDefId),
            undeterminedActionIdsMap.get(alarmDefId)
        );

        result.add(ad);
      }

      alarmDefinitionSet.add(alarmDefId);
    }
    return result;
  }

  private List<String> splitStringIntoList(String str) {
    return str == null ? Lists.<String>newArrayList() : Lists.newArrayList(COMMA_SPLITTER.split(str));
  }

  private void createSubExpressions(Session session,
                                    AlarmDefinitionDb alarmDefinition,
                                    Map<String, AlarmSubExpression> alarmSubExpressions) {

    if (alarmSubExpressions != null) {
      for (Map.Entry<String, AlarmSubExpression> subEntry : alarmSubExpressions.entrySet()) {
        String subAlarmId = subEntry.getKey();
        AlarmSubExpression subExpr = subEntry.getValue();
        MetricDefinition metricDef = subExpr.getMetricDefinition();

        // Persist sub-alarm
        final DateTime now = this.getUTCNow();
        final SubAlarmDefinitionDb subAlarmDefinitionDb = new SubAlarmDefinitionDb(
            subAlarmId,
            alarmDefinition,
            subExpr.getFunction().name(),
            metricDef.name,
            subExpr.getOperator().name(),
            subExpr.getThreshold(),
            subExpr.getPeriod(),
            subExpr.getPeriods(),
            now,
            now,
            subExpr.isDeterministic()
        );
        session.save(subAlarmDefinitionDb);

        // Persist sub-alarm dimensions
        if (!MapUtils.isEmpty(metricDef.dimensions)) {
          SubAlarmDefinitionDimensionDb definitionDimension;
          SubAlarmDefinitionDimensionId definitionDimensionId;

          for (Map.Entry<String, String> dimEntry : metricDef.dimensions.entrySet()) {
            definitionDimensionId = new SubAlarmDefinitionDimensionId(subAlarmDefinitionDb, dimEntry.getKey());
            definitionDimension = new SubAlarmDefinitionDimensionDb(definitionDimensionId, dimEntry.getValue());
            session.save(definitionDimension);
          }

        }
      }
    }

  }

  private void persistActions(final Session session,
                              final AlarmDefinitionDb alarmDefinition,
                              final AlarmState alarmState,
                              final List<String> actions) {
    if (actions != null) {
      for (String action : actions) {
        session.save(new AlarmActionDb(alarmDefinition, alarmState, action));
      }
    }
  }

  // method extracted for code-readability
  private static ResultTransformer getAlarmDefResultTransformer() {
    return new ResultTransformer() {
      private static final long serialVersionUID = -3052468375925339521L;

      @Override
      public Object transformTuple(final Object[] tuple, final String[] aliases) {
        for (int i = 0, length = aliases.length; i < length; i++) {
          aliases[i] = aliases[i].toUpperCase();
        }
        return AliasToEntityMapResultTransformer
            .INSTANCE
            .transformTuple(tuple, aliases);
      }

      @Override
      public List transformList(final List collection) {
        return AliasToEntityMapResultTransformer
            .INSTANCE
            .transformList(collection);
      }
    };
  }
}
