package com.hpcloud.mon.app;

import java.util.HashMap;
import java.util.HashSet;
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

import com.google.common.collect.Sets;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.app.command.UpdateAlarmCommand;
import com.hpcloud.mon.common.event.AlarmCreatedEvent;
import com.hpcloud.mon.common.event.AlarmDeletedEvent;
import com.hpcloud.mon.common.event.AlarmUpdatedEvent;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.exception.EntityExistsException;
import com.hpcloud.mon.domain.exception.InvalidEntityException;
import com.hpcloud.mon.domain.model.alarm.AlarmDetail;
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
   */
  public AlarmDetail create(String tenantId, String name, @Nullable String description,
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
    AlarmDetail alarm = null;

    try {
      LOG.debug("Creating alarm {} for tenant {}", name, tenantId);
      alarm = repo.create(alarmId, tenantId, name, description, expression, subAlarms,
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
   * Updates the alarm for the {@code tenantId} and {@code alarmId} to the state of the
   * {@code command}.
   */
  public AlarmDetail update(String tenantId, String alarmId, AlarmExpression alarmExpression,
      UpdateAlarmCommand command) {
    // Assert alarm and actions exist
    repo.findById(tenantId, alarmId);
    assertActionsExist(tenantId, command.alarmActions);
    assertActionsExist(tenantId, command.okActions);
    assertActionsExist(tenantId, command.undeterminedActions);

    Map<String, AlarmSubExpression> oldSubAlarms = repo.findSubExpressions(alarmId);
    Set<AlarmSubExpression> oldSet = new HashSet<>(oldSubAlarms.values());
    Set<AlarmSubExpression> newSet = new HashSet<>(alarmExpression.getSubExpressions());
    Set<AlarmSubExpression> oldExpressions = Sets.difference(oldSet, newSet);
    Set<AlarmSubExpression> newExpressions = Sets.difference(newSet, oldSet);
    oldSubAlarms.values().retainAll(oldExpressions);

    Map<String, AlarmSubExpression> newSubAlarms = new HashMap<String, AlarmSubExpression>();
    for (AlarmSubExpression expression : newExpressions)
      newSubAlarms.put(UUID.randomUUID().toString(), expression);

    try {
      LOG.debug("Updating alarm {} for tenant {}", command.name, tenantId);
      AlarmDetail updatedAlarm = repo.update(alarmId, tenantId, command.name, command.description,
          command.expression, command.state, command.enabled, oldSubAlarms.keySet(), newSubAlarms,
          command.alarmActions, command.okActions, command.undeterminedActions);

      // Notify interested parties of new alarm
      String event = Serialization.toJson(new AlarmUpdatedEvent(tenantId, alarmId, command.name,
          command.expression, oldSubAlarms, newSubAlarms));
      producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));

      return updatedAlarm;
    } catch (Exception e) {
      throw Exceptions.uncheck(e, "Error updating alarm for project / tenant %s", tenantId);
    }
  }

  /**
   * Patches the alarm for the {@code tenantId} and {@code alarmId} to the state of the
   * {@code fields}.
   */
  public AlarmDetail update(String tenantId, String alarmId, Map<String, Object> fields) {
    return null;
  }

  public void delete(String tenantId, String alarmId) {
    Map<String, MetricDefinition> subAlarmMetricDefs = repo.findSubAlarmMetricDefinitions(alarmId);
    repo.deleteById(tenantId, alarmId);

    // Notify interested parties of alarm deletion
    String event = Serialization.toJson(new AlarmDeletedEvent(tenantId, alarmId, subAlarmMetricDefs));
    producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));
  }

  private void assertActionsExist(String tenantId, List<String> actions) {
    if (actions != null)
      for (String action : actions)
        if (!notificationMethodRepo.exists(tenantId, action))
          throw new InvalidEntityException("No notification method exists for action %s", action);
  }
}
