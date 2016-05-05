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

import monasca.api.ApiConfig;
import monasca.api.app.command.UpdateAlarmDefinitionCommand;
import monasca.api.app.validation.DimensionValidation;
import monasca.common.model.event.AlarmDefinitionCreatedEvent;
import monasca.common.model.event.AlarmDefinitionDeletedEvent;
import monasca.common.model.event.AlarmDefinitionUpdatedEvent;
import monasca.common.model.event.AlarmDeletedEvent;
import monasca.common.model.alarm.AlarmExpression;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import monasca.api.domain.exception.EntityExistsException;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.exception.InvalidEntityException;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmRepo;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepo;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepo;
import monasca.common.util.Exceptions;
import monasca.common.util.Serialization;

/**
 * Services alarm definition related requests.
 */
public class AlarmDefinitionService {
  private static final Logger LOG = LoggerFactory.getLogger(AlarmService.class);

  private final ApiConfig config;
  private final Producer<String, String> producer;
  private final AlarmDefinitionRepo repo;
  private final AlarmRepo alarmRepo;
  private final NotificationMethodRepo notificationMethodRepo;
  long eventCount;

  @Inject
  public AlarmDefinitionService(ApiConfig config, Producer<String, String> producer,
      AlarmDefinitionRepo repo, AlarmRepo alarmRepo,
      NotificationMethodRepo notificationMethodRepo) {
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
    String alarmDefID=repo.exists(tenantId, name);
    if (alarmDefID!=null) {
      throw new EntityExistsException(
          "An alarm definition already exists for project / tenant: %s named: %s", tenantId, name);
    }
    DimensionValidation.validateNames(matchBy);
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
      producer.send(new KeyedMessage<>(config.eventsTopic, String.valueOf(eventCount++), event));

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
    final List<Alarm> alarms = alarmRepo.find(tenantId, alarmDefId, null, null, null, null, null, null, null, null, null, 1, false);
    final Map<String, Map<String, AlarmSubExpression>> alarmSubExpressions =
                                          alarmRepo.findAlarmSubExpressionsForAlarmDefinition(alarmDefId);

    repo.deleteById(tenantId, alarmDefId);

    // Notify interested parties of alarm definition deletion
    String event =
        Serialization.toJson(new AlarmDefinitionDeletedEvent(alarmDefId, subAlarmMetricDefs));
    producer.send(new KeyedMessage<>(config.eventsTopic, String.valueOf(eventCount++), event));

    // Notify about the Deletion of the Alarms second because that is the order that thresh
    // wants it so Alarms don't get recreated
    for (final Alarm alarm : alarms) {
      String alarmDeletedEvent =
          Serialization.toJson(new AlarmDeletedEvent(tenantId, alarm.getId(), alarm.getMetrics(),
              alarmDefId, alarmSubExpressions.get(alarm.getId())));
      producer.send(new KeyedMessage<>(config.eventsTopic, String.valueOf(eventCount++), alarmDeletedEvent));
    }
  }

    /**
     * Updates the alarm definition for the {@code tenantId} and
     * {@code alarmDefId} to the state of the {@code command}.
     *
     * @throws EntityNotFoundException
     *             if the alarm cannot be found
     * @throws InvalidEntityException
     *             if one of the actions cannot be found
     */
  public AlarmDefinition update(String tenantId, String alarmDefId,
          AlarmExpression alarmExpression,
          UpdateAlarmDefinitionCommand command) {
      final AlarmDefinition oldAlarmDefinition = assertAlarmDefinitionExists(
              tenantId, alarmDefId, command.alarmActions, command.okActions,
              command.undeterminedActions);
      final SubExpressions subExpressions = subExpressionsFor(
              repo.findSubExpressions(alarmDefId), alarmExpression);
      String alarmID = repo.exists(tenantId, command.name);
      if (alarmID != null && !alarmID.equalsIgnoreCase(alarmDefId)) {
          throw new EntityExistsException(
                  "An alarm definition with the same name already exists for project / tenant: %s named: %s",
                  tenantId, command.name);
      }
      validateChangesAllowed(command.matchBy, oldAlarmDefinition,
              subExpressions);
      updateInternal(tenantId, alarmDefId, false, command.name,
              command.description, command.expression, command.matchBy,
              command.severity, alarmExpression, command.actionsEnabled,
              command.alarmActions, command.okActions,
              command.undeterminedActions, subExpressions);
      return new AlarmDefinition(alarmDefId, command.name,
              command.description, command.severity, command.expression,
              command.matchBy, command.actionsEnabled, command.alarmActions,
              command.okActions, command.undeterminedActions);
  }

  /**
   * Don't allow changes that would cause existing Alarms for this AlarmDefinition to be invalidated.
   *
   * matchBy can't change and the expression can't change the metrics used or number of subexpressions
   */
  private void validateChangesAllowed(final List<String> newMatchBy,
      final AlarmDefinition oldAlarmDefinition, final SubExpressions subExpressions) {
    final boolean matchBySame;
    if (oldAlarmDefinition.getMatchBy() == null || oldAlarmDefinition.getMatchBy().isEmpty()) {
      matchBySame = newMatchBy == null || newMatchBy.isEmpty();
    }
    else {
      matchBySame = oldAlarmDefinition.getMatchBy().equals(newMatchBy);
    }
    if (!matchBySame) {
      throw monasca.api.resource.exception.Exceptions.unprocessableEntity("match_by must not change");
    }
    if (!subExpressions.oldAlarmSubExpressions.isEmpty() || !subExpressions.newAlarmSubExpressions.isEmpty()) {
      final int newCount = subExpressions.newAlarmSubExpressions.size() +
                           subExpressions.changedSubExpressions.size() +
                           subExpressions.unchangedSubExpressions.size();
      if (newCount != AlarmExpression.of(oldAlarmDefinition.getExpression()).getSubExpressions().size()) {
        throw monasca.api.resource.exception.Exceptions.unprocessableEntity("number of subexpressions must not change");
      }
      else {
        throw monasca.api.resource.exception.Exceptions.unprocessableEntity("metrics in subexpression must not change");
      }
    }
  }

  /**
   * Patches the alarm definition for the {@code tenantId} and {@code alarmDefId} to the state of
   * the {@code fields}.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   * @throws InvalidEntityException if one of the actions cannot be found
   */
    public AlarmDefinition patch(String tenantId, String alarmDefId,
            String name, String description, String severity,
            String expression, AlarmExpression alarmExpression,
            List<String> matchBy, Boolean enabled, List<String> alarmActions,
            List<String> okActions, List<String> undeterminedActions) {
        AlarmDefinition oldAlarmDefinition = assertAlarmDefinitionExists(
                tenantId, alarmDefId, alarmActions, okActions,
                undeterminedActions);
        name = name == null ? oldAlarmDefinition.getName() : name;
        String alarmID = repo.exists(tenantId, name);
        if (alarmID != null && !alarmID.equalsIgnoreCase(alarmDefId)) {
            throw new EntityExistsException(
                    "An alarm definition with the same name already exists for project / tenant: %s named: %s",
                    tenantId, name);
        }
        description = description == null ? oldAlarmDefinition.getDescription()
                : description;
        expression = expression == null ? oldAlarmDefinition.getExpression()
                : expression;
        severity = severity == null ? oldAlarmDefinition.getSeverity()
                : severity;
        alarmExpression = alarmExpression == null ? AlarmExpression
                .of(expression) : alarmExpression;
        enabled = enabled == null ? oldAlarmDefinition.isActionsEnabled()
                : enabled;
        matchBy = matchBy == null ? oldAlarmDefinition.getMatchBy() : matchBy;

        final SubExpressions subExpressions = subExpressionsFor(
                repo.findSubExpressions(alarmDefId), alarmExpression);
        validateChangesAllowed(matchBy, oldAlarmDefinition, subExpressions);
        updateInternal(tenantId, alarmDefId, true, name, description,
                expression, matchBy, severity, alarmExpression, enabled,
                alarmActions, okActions, undeterminedActions, subExpressions);

        return new AlarmDefinition(alarmDefId, name, description, severity,
                expression, matchBy, enabled,
                alarmActions == null ? oldAlarmDefinition.getAlarmActions()
                        : alarmActions,
                okActions == null ? oldAlarmDefinition.getOkActions()
                        : okActions,
                undeterminedActions == null ? oldAlarmDefinition
                        .getUndeterminedActions() : undeterminedActions);
    }

    private void updateInternal(String tenantId, String alarmDefId,
            boolean patch, String name, String description, String expression,
            List<String> matchBy, String severity,
            AlarmExpression alarmExpression, Boolean enabled,
            List<String> alarmActions, List<String> okActions,
            List<String> undeterminedActions, SubExpressions subExpressions) {

        try {
            LOG.debug("Updating alarm definition {} for tenant {}", name,
                    tenantId);
            repo.update(tenantId, alarmDefId, patch, name, description,
                    expression, matchBy, severity, enabled,
                    subExpressions.oldAlarmSubExpressions.keySet(),
                    subExpressions.changedSubExpressions,
                    subExpressions.newAlarmSubExpressions, alarmActions,
                    okActions, undeterminedActions);

            // Notify interested parties of updated alarm
            String event = Serialization
                    .toJson(new AlarmDefinitionUpdatedEvent(tenantId,
                            alarmDefId, name, description, expression, matchBy,
                            enabled, severity,
                            subExpressions.oldAlarmSubExpressions,
                            subExpressions.changedSubExpressions,
                            subExpressions.unchangedSubExpressions,
                            subExpressions.newAlarmSubExpressions));
            producer.send(new KeyedMessage<>(config.eventsTopic, String
                    .valueOf(eventCount++), event));
        } catch (Exception e) {
            throw Exceptions.uncheck(e,
                    "Error updating alarm definition for project / tenant %s",
                    tenantId);
        }
    }

  /**
   * Returns an entry containing Maps of old, changed, and new sub expressions by comparing the
   * {@code alarmExpression} to the existing sub expressions for the {@code alarmDefId}.
   */
  SubExpressions subExpressionsFor(final Map<String, AlarmSubExpression> initialSubExpressions,
      AlarmExpression alarmExpression) {
    BiMap<String, AlarmSubExpression> oldExpressions =
        HashBiMap.create(initialSubExpressions);
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
   * Returns whether all of the metrics of {@code a} and {@code b} are the same. The Threshold
   * Engine can handle any other type of change to the expression
   */
  private boolean sameKeyFields(AlarmSubExpression a, AlarmSubExpression b) {
    return a.getMetricDefinition().equals(b.getMetricDefinition());
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
            throw monasca.api.resource.exception.Exceptions.unprocessableEntity(
            "No notification method exists for action %s", action);
  }
}
