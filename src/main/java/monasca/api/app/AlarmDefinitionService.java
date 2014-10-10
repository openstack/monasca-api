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
package monasca.api.app;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;

import monasca.api.MonApiConfiguration;
import monasca.api.app.command.UpdateAlarmDefinitionCommand;
import com.hpcloud.mon.common.event.AlarmDefinitionCreatedEvent;
import com.hpcloud.mon.common.event.AlarmDefinitionDeletedEvent;
import com.hpcloud.mon.common.event.AlarmDefinitionUpdatedEvent;
import com.hpcloud.mon.common.event.AlarmDeletedEvent;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import monasca.api.domain.exception.EntityExistsException;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.exception.InvalidEntityException;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmRepository;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepository;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepository;
import com.hpcloud.util.Exceptions;
import com.hpcloud.util.Serialization;

/**
 * Services alarm definition related requests.
 */
public class AlarmDefinitionService {
  private static final Logger LOG = LoggerFactory.getLogger(AlarmService.class);

  private final MonApiConfiguration config;
  private final Producer<String, String> producer;
  private final AlarmDefinitionRepository repo;
  private final AlarmRepository alarmRepo;
  private final NotificationMethodRepository notificationMethodRepo;

  @Inject
  public AlarmDefinitionService(MonApiConfiguration config, Producer<String, String> producer,
      AlarmDefinitionRepository repo, AlarmRepository alarmRepo,
      NotificationMethodRepository notificationMethodRepo) {
    this.config = config;
    this.producer = producer;
    this.repo = repo;
    this.alarmRepo = alarmRepo;
    this.notificationMethodRepo = notificationMethodRepo;
  }

  static class SubExpressions {
    /** Sub expressions which have been removed from an updated alarm expression. */
    Map<String, AlarmSubExpression> oldAlarmSubExpressions;
    /** Sub expressions which have had their operator or threshold changed. */
    Map<String, AlarmSubExpression> changedSubExpressions;
    /** Sub expressions which have not changed. */
    Map<String, AlarmSubExpression> unchangedSubExpressions;
    /** Sub expressions which have been added to an updated alarm expression. */
    Map<String, AlarmSubExpression> newAlarmSubExpressions;
  }

  /**
   * Creates an alarm definition and publishes an AlarmDefinitionCreatedEvent. Note, the event is
   * published first since chances of failure are higher.
   * 
   * @throws EntityExistsException if an alarm already exists for the name
   * @throws InvalidEntityException if one of the actions cannot be found
   */
  public AlarmDefinition create(String tenantId, String name, @Nullable String description,
      String severity, String expression, AlarmExpression alarmExpression, List<String> matchBy,
      List<String> alarmActions, @Nullable List<String> okActions,
      @Nullable List<String> undeterminedActions) {
    // Assert no alarm exists by the name
    if (repo.exists(tenantId, name))
      throw new EntityExistsException(
          "An alarm definition already exists for project / tenant: %s named: %s", tenantId, name);

    assertActionsExist(tenantId, alarmActions, okActions, undeterminedActions);

    Map<String, AlarmSubExpression> subAlarms = new HashMap<String, AlarmSubExpression>();
    for (AlarmSubExpression subExpression : alarmExpression.getSubExpressions())
      subAlarms.put(UUID.randomUUID().toString(), subExpression);

    String alarmDefId = UUID.randomUUID().toString();
    AlarmDefinition alarm = null;

    try {
      LOG.debug("Creating alarm definition {} for tenant {}", name, tenantId);
      alarm =
          repo.create(tenantId, alarmDefId, name, description, severity, expression, subAlarms,
              matchBy, alarmActions, okActions, undeterminedActions);

      // Notify interested parties of new alarm
      String event =
          Serialization.toJson(new AlarmDefinitionCreatedEvent(tenantId, alarmDefId, name,
              description, expression, subAlarms, matchBy));
      producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));

      return alarm;
    } catch (Exception e) {
      if (alarm != null)
        try {
          repo.deleteById(tenantId, alarm.getId());
        } catch (Exception ignore) {
        }
      throw Exceptions.uncheck(e, "Error creating alarm definition for project / tenant %s",
          tenantId);
    }
  }

  /**
   * Deletes the alarm definition identified by the {@code alarmDefId}.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   */
  public void delete(String tenantId, String alarmDefId) {
    Map<String, MetricDefinition> subAlarmMetricDefs =
        repo.findSubAlarmMetricDefinitions(alarmDefId);

    // Have to get information about the Alarms before they are deleted. They will be deleted
    // by the database as a cascade delete from the Alarm Definition delete
    final List<Alarm> alarms = alarmRepo.find(tenantId, alarmDefId, null, null, null);
    final Map<String, Map<String, AlarmSubExpression>> alarmSubExpressions = new HashMap<>(alarms.size());
    for (final Alarm alarm : alarms) {
      alarmSubExpressions.put(alarm.getId(), alarmRepo.findAlarmSubExpressions(alarm.getId()));
    }

    repo.deleteById(tenantId, alarmDefId);

    // Notify interested parties of alarm definition deletion
    String event =
        Serialization.toJson(new AlarmDefinitionDeletedEvent(alarmDefId, subAlarmMetricDefs));
    producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));

    // Notify about the Deletion of the Alarms second because that is the order that thresh
    // wants it so Alarms don't get recreated
    for (final Alarm alarm : alarms) {
      String alarmDeletedEvent =
          Serialization.toJson(new AlarmDeletedEvent(tenantId, alarm.getId(), alarm.getMetrics(),
              alarmDefId, alarmSubExpressions.get(alarm.getId())));
      producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, alarmDeletedEvent));
    }
  }

  /**
   * Updates the alarm definition for the {@code tenantId} and {@code alarmDefId} to the state of
   * the {@code command}.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   * @throws InvalidEntityException if one of the actions cannot be found
   */
  public AlarmDefinition update(String tenantId, String alarmDefId,
      AlarmExpression alarmExpression, UpdateAlarmDefinitionCommand command) {
    assertAlarmDefinitionExists(tenantId, alarmDefId, command.alarmActions, command.okActions,
        command.undeterminedActions);
    updateInternal(tenantId, alarmDefId, false, command.name, command.description,
        command.expression, command.matchBy, command.severity, alarmExpression,
        command.actionsEnabled, command.alarmActions, command.okActions,
        command.undeterminedActions);
    return new AlarmDefinition(alarmDefId, command.name, command.description, command.severity,
        command.expression, command.matchBy, command.actionsEnabled, command.alarmActions,
        command.okActions, command.undeterminedActions);
  }

  /**
   * Patches the alarm definition for the {@code tenantId} and {@code alarmDefId} to the state of
   * the {@code fields}.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   * @throws InvalidEntityException if one of the actions cannot be found
   */
  public AlarmDefinition patch(String tenantId, String alarmDefId, String name, String description,
      String severity, String expression, AlarmExpression alarmExpression, List<String> matchBy,
      AlarmState state, Boolean enabled, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions) {
    AlarmDefinition alarm =
        assertAlarmDefinitionExists(tenantId, alarmDefId, alarmActions, okActions,
            undeterminedActions);
    name = name == null ? alarm.getName() : name;
    description = description == null ? alarm.getDescription() : description;
    expression = expression == null ? alarm.getExpression() : expression;
    severity = severity == null ? alarm.getSeverity() : severity;
    alarmExpression = alarmExpression == null ? AlarmExpression.of(expression) : alarmExpression;
    enabled = enabled == null ? alarm.isActionsEnabled() : enabled;

    updateInternal(tenantId, alarmDefId, true, name, description, expression, matchBy, severity,
        alarmExpression, enabled, alarmActions, okActions, undeterminedActions);

    return new AlarmDefinition(alarmDefId, name, description, severity, expression, matchBy,
        enabled, alarmActions == null ? alarm.getAlarmActions() : alarmActions,
        okActions == null ? alarm.getOkActions() : okActions,
        undeterminedActions == null ? alarm.getUndeterminedActions() : undeterminedActions);
  }

  private void updateInternal(String tenantId, String alarmDefId, boolean patch, String name,
      String description, String expression, List<String> matchBy, String severity,
      AlarmExpression alarmExpression, Boolean enabled, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    SubExpressions subExpressions = subExpressionsFor(alarmDefId, alarmExpression);

    try {
      LOG.debug("Updating alarm definition {} for tenant {}", name, tenantId);
      repo.update(tenantId, alarmDefId, patch, name, description, expression, matchBy, severity,
          enabled, subExpressions.oldAlarmSubExpressions.keySet(),
          subExpressions.changedSubExpressions, subExpressions.newAlarmSubExpressions,
          alarmActions, okActions, undeterminedActions);

      // Notify interested parties of updated alarm
      String event =
          Serialization.toJson(new AlarmDefinitionUpdatedEvent(tenantId, alarmDefId, name,
              description, expression, matchBy, enabled, severity, subExpressions.oldAlarmSubExpressions,
              subExpressions.changedSubExpressions, subExpressions.unchangedSubExpressions,
              subExpressions.newAlarmSubExpressions));
      producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));
    } catch (Exception e) {
      throw Exceptions.uncheck(e, "Error updating alarm definition for project / tenant %s",
          tenantId);
    }
  }

  /**
   * Returns an entry containing Maps of old, changed, and new sub expressions by comparing the
   * {@code alarmExpression} to the existing sub expressions for the {@code alarmDefId}.
   */
  SubExpressions subExpressionsFor(String alarmDefId, AlarmExpression alarmExpression) {
    BiMap<String, AlarmSubExpression> oldExpressions =
        HashBiMap.create(repo.findSubExpressions(alarmDefId));
    Set<AlarmSubExpression> oldSet = oldExpressions.inverse().keySet();
    Set<AlarmSubExpression> newSet = new HashSet<>(alarmExpression.getSubExpressions());

    // Identify old or changed expressions
    Set<AlarmSubExpression> oldOrChangedExpressions =
        new HashSet<>(Sets.difference(oldSet, newSet));

    // Identify new or changed expressions
    Set<AlarmSubExpression> newOrChangedExpressions =
        new HashSet<>(Sets.difference(newSet, oldSet));

    // Find changed expressions
    Map<String, AlarmSubExpression> changedExpressions = new HashMap<>();
    for (Iterator<AlarmSubExpression> oldIt = oldOrChangedExpressions.iterator(); oldIt.hasNext();) {
      AlarmSubExpression oldExpr = oldIt.next();
      for (Iterator<AlarmSubExpression> newIt = newOrChangedExpressions.iterator(); newIt.hasNext();) {
        AlarmSubExpression newExpr = newIt.next();
        if (sameKeyFields(oldExpr, newExpr)) {
          oldIt.remove();
          newIt.remove();
          changedExpressions.put(oldExpressions.inverse().get(oldExpr), newExpr);
        }
      }
    }

    // Create the list of unchanged expressions
    BiMap<String, AlarmSubExpression> unchangedExpressions = HashBiMap.create(oldExpressions);
    unchangedExpressions.values().removeAll(oldOrChangedExpressions);
    unchangedExpressions.keySet().removeAll(changedExpressions.keySet());

    // Remove old sub expressions
    oldExpressions.values().retainAll(oldOrChangedExpressions);

    // Create IDs for new expressions
    Map<String, AlarmSubExpression> newExpressions = new HashMap<>();
    for (AlarmSubExpression expression : newOrChangedExpressions)
      newExpressions.put(UUID.randomUUID().toString(), expression);

    SubExpressions subExpressions = new SubExpressions();
    subExpressions.oldAlarmSubExpressions = oldExpressions;
    subExpressions.changedSubExpressions = changedExpressions;
    subExpressions.unchangedSubExpressions = unchangedExpressions;
    subExpressions.newAlarmSubExpressions = newExpressions;
    return subExpressions;
  }

  /**
   * Returns whether all of the fields of {@code a} and {@code b} are the same except the operator
   * and threshold.
   */
  private boolean sameKeyFields(AlarmSubExpression a, AlarmSubExpression b) {
    return a.getMetricDefinition().equals(b.getMetricDefinition())
        && a.getFunction().equals(b.getFunction()) && a.getPeriod() == b.getPeriod()
        && a.getPeriods() == b.getPeriods();
  }

  /**
   * Asserts an alarm definition exists for the {@code alarmDefId} as well as the actions.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   */
  private AlarmDefinition assertAlarmDefinitionExists(String tenantId, String alarmDefId,
      List<String> alarmActions, List<String> okActions, List<String> undeterminedActions) {
    AlarmDefinition alarm = repo.findById(tenantId, alarmDefId);
    assertActionsExist(tenantId, alarmActions, okActions, undeterminedActions);
    return alarm;
  }

  private void assertActionsExist(String tenantId, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    Set<String> actions = new HashSet<>();
    if (alarmActions != null)
      actions.addAll(alarmActions);
    if (okActions != null)
      actions.addAll(okActions);
    if (undeterminedActions != null)
      actions.addAll(undeterminedActions);
    if (!actions.isEmpty())
      for (String action : actions)
        if (!notificationMethodRepo.exists(tenantId, action))
          throw new InvalidEntityException("No notification method exists for action %s", action);
  }
}
