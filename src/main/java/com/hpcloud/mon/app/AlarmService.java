/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpcloud.mon.app;

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
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.app.command.UpdateAlarmCommand;
import com.hpcloud.mon.common.event.AlarmCreatedEvent;
import com.hpcloud.mon.common.event.AlarmDeletedEvent;
import com.hpcloud.mon.common.event.AlarmStateTransitionedEvent;
import com.hpcloud.mon.common.event.AlarmUpdatedEvent;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.exception.EntityExistsException;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.exception.InvalidEntityException;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;
import com.hpcloud.util.Exceptions;
import com.hpcloud.util.Serialization;

/**
 * Services alarm related requests.
 */
public class AlarmService {
  private static final Logger LOG = LoggerFactory.getLogger(AlarmService.class);

  private final MonApiConfiguration config;
  private final Producer<String, String> producer;
  private final AlarmRepository repo;
  private final NotificationMethodRepository notificationMethodRepo;

  @Inject
  public AlarmService(MonApiConfiguration config, Producer<String, String> producer,
      AlarmRepository repo, NotificationMethodRepository notificationMethodRepo) {
    this.config = config;
    this.producer = producer;
    this.repo = repo;
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
   * Creates an alarm and publishes an AlarmCreatedEvent. Note, the event is published first since
   * chances of failure are higher.
   * 
   * @throws EntityExistsException if an alarm already exists for the name
   * @throws InvalidEntityException if one of the actions cannot be found
   */
  public Alarm create(String tenantId, String name, @Nullable String description, String severity,
      String expression, AlarmExpression alarmExpression, List<String> alarmActions,
      @Nullable List<String> okActions, @Nullable List<String> undeterminedActions) {
    // Assert no alarm exists by the name
    if (repo.exists(tenantId, name))
      throw new EntityExistsException("An alarm already exists for project / tenant: %s named: %s",
          tenantId, name);

    assertActionsExist(tenantId, alarmActions, okActions, undeterminedActions);

    Map<String, AlarmSubExpression> subAlarms = new HashMap<String, AlarmSubExpression>();
    for (AlarmSubExpression subExpression : alarmExpression.getSubExpressions())
      subAlarms.put(UUID.randomUUID().toString(), subExpression);

    String alarmId = UUID.randomUUID().toString();
    Alarm alarm = null;

    try {
      LOG.debug("Creating alarm {} for tenant {}", name, tenantId);
      alarm = repo.create(tenantId, alarmId, name, description, severity, expression, subAlarms,
          alarmActions, okActions, undeterminedActions);

      // Notify interested parties of new alarm
      String event = Serialization.toJson(new AlarmCreatedEvent(tenantId, alarmId, name,
          expression, subAlarms));
      producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));

      return alarm;
    } catch (Exception e) {
      if (alarm != null)
        try {
          repo.deleteById(tenantId, alarm.getId());
        } catch (Exception ignore) {
        }
      throw Exceptions.uncheck(e, "Error creating alarm for project / tenant %s", tenantId);
    }
  }

  /**
   * Deletes the alarm identified by the {@code alarmId}.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   */
  public void delete(String tenantId, String alarmId) {
    Map<String, MetricDefinition> subAlarmMetricDefs = repo.findSubAlarmMetricDefinitions(alarmId);
    repo.deleteById(tenantId, alarmId);

    // Notify interested parties of alarm deletion
    String event = Serialization.toJson(new AlarmDeletedEvent(tenantId, alarmId, subAlarmMetricDefs));
    producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));
  }

  /**
   * Updates the alarm for the {@code tenantId} and {@code alarmId} to the state of the
   * {@code command}.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   * @throws InvalidEntityException if one of the actions cannot be found
   */
  public Alarm update(String tenantId, String alarmId, AlarmExpression alarmExpression,
      UpdateAlarmCommand command) {
    Alarm alarm = assertAlarmExists(tenantId, alarmId, command.alarmActions, command.okActions,
        command.undeterminedActions);
    updateInternal(tenantId, alarmId, false, command.name, command.description, command.expression,
        command.severity, alarmExpression, alarm.getState(), command.state, command.actionsEnabled,
        command.alarmActions, command.okActions, command.undeterminedActions);
    return new Alarm(alarmId, command.name, command.description, command.severity,
        command.expression, command.state, command.actionsEnabled, command.alarmActions,
        command.okActions, command.undeterminedActions);
  }

  /**
   * Patches the alarm for the {@code tenantId} and {@code alarmId} to the state of the
   * {@code fields}.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   * @throws InvalidEntityException if one of the actions cannot be found
   */
  public Alarm patch(String tenantId, String alarmId, String name, String description,
      String severity, String expression, AlarmExpression alarmExpression, AlarmState state,
      Boolean enabled, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions) {
    Alarm alarm = assertAlarmExists(tenantId, alarmId, alarmActions, okActions, undeterminedActions);
    name = name == null ? alarm.getName() : name;
    description = description == null ? alarm.getDescription() : description;
    expression = expression == null ? alarm.getExpression() : expression;
    severity = severity == null ? alarm.getSeverity() : severity;
    alarmExpression = alarmExpression == null ? AlarmExpression.of(expression) : alarmExpression;
    state = state == null ? alarm.getState() : state;
    enabled = enabled == null ? alarm.isActionsEnabled() : enabled;

    updateInternal(tenantId, alarmId, true, name, description, expression, severity,
        alarmExpression, alarm.getState(), state, enabled, alarmActions, okActions,
        undeterminedActions);

    return new Alarm(alarmId, name, description, severity, expression, state, enabled,
        alarmActions == null ? alarm.getAlarmActions() : alarmActions,
        okActions == null ? alarm.getOkActions() : okActions,
        undeterminedActions == null ? alarm.getUndeterminedActions() : undeterminedActions);
  }

  private void updateInternal(String tenantId, String alarmId, boolean patch, String name,
      String description, String expression, String severity, AlarmExpression alarmExpression,
      AlarmState oldState, AlarmState newState, Boolean enabled, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    SubExpressions subExpressions = subExpressionsFor(alarmId, alarmExpression);

    try {
      LOG.debug("Updating alarm {} for tenant {}", name, tenantId);
      repo.update(tenantId, alarmId, patch, name, description, expression, severity, newState,
          enabled, subExpressions.oldAlarmSubExpressions.keySet(),
          subExpressions.changedSubExpressions, subExpressions.newAlarmSubExpressions,
          alarmActions, okActions, undeterminedActions);

      // Notify interested parties of updated alarm
      String event = Serialization.toJson(new AlarmUpdatedEvent(tenantId, alarmId, name,
          description, expression, newState, oldState, enabled, subExpressions.oldAlarmSubExpressions,
          subExpressions.changedSubExpressions, subExpressions.unchangedSubExpressions,
          subExpressions.newAlarmSubExpressions));
      producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));

      // Notify interested parties of transitioned alarm state
      if (!oldState.equals(newState)) {
        event = Serialization.toJson(new AlarmStateTransitionedEvent(tenantId, alarmId, name,
            description, oldState, newState, enabled, stateChangeReasonFor(oldState, newState),
            System.currentTimeMillis() / 1000));
        producer.send(new KeyedMessage<>(config.alarmStateTransitionsTopic, tenantId, event));
      }
    } catch (Exception e) {
      throw Exceptions.uncheck(e, "Error updating alarm for project / tenant %s", tenantId);
    }
  }

  /**
   * Returns an entry containing Maps of old, changed, and new sub expressions by comparing the
   * {@code alarmExpression} to the existing sub expressions for the {@code alarmId}.
   */
  SubExpressions subExpressionsFor(String alarmId, AlarmExpression alarmExpression) {
    BiMap<String, AlarmSubExpression> oldExpressions = HashBiMap.create(repo.findSubExpressions(alarmId));
    Set<AlarmSubExpression> oldSet = oldExpressions.inverse().keySet();
    Set<AlarmSubExpression> newSet = new HashSet<>(alarmExpression.getSubExpressions());

    // Identify old or changed expressions
    Set<AlarmSubExpression> oldOrChangedExpressions = new HashSet<>(Sets.difference(oldSet, newSet));

    // Identify new or changed expressions
    Set<AlarmSubExpression> newOrChangedExpressions = new HashSet<>(Sets.difference(newSet, oldSet));

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

  private String stateChangeReasonFor(AlarmState oldState, AlarmState newState) {
    return "Alarm state updated via API";
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
   * Asserts an alarm exists for the {@code alarmId} as well as the actions.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   */
  private Alarm assertAlarmExists(String tenantId, String alarmId, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    Alarm alarm = repo.findById(tenantId, alarmId);
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