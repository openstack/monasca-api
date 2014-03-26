package com.hpcloud.mon.app;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.app.command.UpdateAlarmCommand;
import com.hpcloud.mon.common.event.AlarmCreatedEvent;
import com.hpcloud.mon.common.event.AlarmDeletedEvent;
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
 * 
 * @author Jonathan Halterman
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

  /**
   * Creates an alarm and publishes an AlarmCreatedEvent. Note, the event is published first since
   * chances of failure are higher.
   * 
   * @throws EntityExistsException if an alarm already exists for the name
   * @throws InvalidEntityException if one of the actions cannot be found
   */
  public Alarm create(String tenantId, String name, @Nullable String description,
      String expression, AlarmExpression alarmExpression, List<String> alarmActions,
      @Nullable List<String> okActions, @Nullable List<String> undeterminedActions) {
    // Assert no alarm exists by the name
    if (repo.exists(tenantId, name))
      throw new EntityExistsException("An alarm already exists for project / tenant: %s named: %s",
          tenantId, name);

    assertActionsExist(tenantId, alarmActions);
    assertActionsExist(tenantId, okActions);
    assertActionsExist(tenantId, undeterminedActions);

    Map<String, AlarmSubExpression> subAlarms = new HashMap<String, AlarmSubExpression>();
    for (AlarmSubExpression subExpression : alarmExpression.getSubExpressions())
      subAlarms.put(UUID.randomUUID().toString(), subExpression);

    String alarmId = UUID.randomUUID().toString();
    Alarm alarm = null;

    try {
      LOG.debug("Creating alarm {} for tenant {}", name, tenantId);
      alarm = repo.create(tenantId, alarmId, name, description, expression, subAlarms,
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
    assertAlarmExists(tenantId, alarmId, command.alarmActions, command.okActions,
        command.undeterminedActions);
    updateInternal(tenantId, alarmId, false, command.name, command.description, command.expression,
        alarmExpression, command.state, command.enabled, command.alarmActions, command.okActions,
        command.undeterminedActions);
    return new Alarm(alarmId, command.name, command.description, command.expression, command.state,
        command.enabled, command.alarmActions, command.okActions, command.undeterminedActions);
  }

  /**
   * Patches the alarm for the {@code tenantId} and {@code alarmId} to the state of the
   * {@code fields}.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   * @throws InvalidEntityException if one of the actions cannot be found
   */
  public Alarm patch(String tenantId, String alarmId, String name, String description,
      String expression, AlarmExpression alarmExpression, AlarmState state, Boolean enabled,
      List<String> alarmActions, List<String> okActions, List<String> undeterminedActions) {
    Alarm alarm = assertAlarmExists(tenantId, alarmId, alarmActions, okActions, undeterminedActions);
    name = name == null ? alarm.getName() : name;
    description = description == null ? alarm.getDescription() : description;
    expression = expression == null ? alarm.getExpression() : expression;
    alarmExpression = alarmExpression == null ? AlarmExpression.of(expression) : alarmExpression;
    state = state == null ? alarm.getState() : state;
    enabled = enabled == null ? alarm.isEnabled() : enabled;

    updateInternal(tenantId, alarmId, true, name, description, expression, alarmExpression, state,
        enabled, alarmActions, okActions, undeterminedActions);

    return new Alarm(alarmId, name, description, expression, state, enabled,
        alarmActions == null ? alarm.getAlarmActions() : alarmActions,
        okActions == null ? alarm.getOkActions() : okActions,
        undeterminedActions == null ? alarm.getUndeterminedActions() : undeterminedActions);
  }

  private void updateInternal(String tenantId, String alarmId, boolean patch, String name,
      String description, String expression, AlarmExpression alarmExpression, AlarmState state,
      Boolean enabled, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions) {
    Entry<Map<String, AlarmSubExpression>, Map<String, AlarmSubExpression>> subAlarms = oldAndNewSubExpressionsFor(
        alarmId, alarmExpression);

    try {
      LOG.debug("Updating alarm {} for tenant {}", name, tenantId);
      repo.update(tenantId, alarmId, patch, name, description, expression, state, enabled,
          subAlarms.getKey().keySet(), subAlarms.getValue(), alarmActions, okActions,
          undeterminedActions);

      // Notify interested parties of new alarm
      String event = Serialization.toJson(new AlarmUpdatedEvent(tenantId, alarmId, name,
          expression, state, enabled, subAlarms.getKey(), subAlarms.getValue()));
      producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));
    } catch (Exception e) {
      throw Exceptions.uncheck(e, "Error updating alarm for project / tenant %s", tenantId);
    }
  }

  /**
   * Returns an entry containing Maps of old and new sub expressions by comparing the
   * {@code alarmExpression} to the existing sub expressions for the {@code alarmId}.
   */
  Entry<Map<String, AlarmSubExpression>, Map<String, AlarmSubExpression>> oldAndNewSubExpressionsFor(
      String alarmId, AlarmExpression alarmExpression) {
    Map<String, AlarmSubExpression> oldSubAlarms = repo.findSubExpressions(alarmId);
    Set<AlarmSubExpression> oldSet = new HashSet<>(oldSubAlarms.values());
    Set<AlarmSubExpression> newSet = new HashSet<>(alarmExpression.getSubExpressions());

    // Filter old sub expressions
    Set<AlarmSubExpression> oldExpressions = Sets.difference(oldSet, newSet);
    oldSubAlarms.values().retainAll(oldExpressions);

    // Identify new sub expressions
    Map<String, AlarmSubExpression> newSubAlarms = new HashMap<>();
    Set<AlarmSubExpression> newExpressions = Sets.difference(newSet, oldSet);
    for (AlarmSubExpression expression : newExpressions)
      newSubAlarms.put(UUID.randomUUID().toString(), expression);

    return new SimpleEntry<>(oldSubAlarms, newSubAlarms);
  }

  /**
   * Asserts an alarm exists for the {@code alarmId} as well as the actions.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   */
  private Alarm assertAlarmExists(String tenantId, String alarmId, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    Alarm alarm = repo.findById(tenantId, alarmId);
    assertActionsExist(tenantId, alarmActions);
    assertActionsExist(tenantId, okActions);
    assertActionsExist(tenantId, undeterminedActions);
    return alarm;
  }

  private void assertActionsExist(String tenantId, List<String> actions) {
    if (actions != null)
      for (String action : actions)
        if (!notificationMethodRepo.exists(tenantId, action))
          throw new InvalidEntityException("No notification method exists for action %s", action);
  }
}
