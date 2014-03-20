package com.hpcloud.mon.app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.common.event.AlarmCreatedEvent;
import com.hpcloud.mon.common.event.AlarmDeletedEvent;
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
      throw new EntityExistsException("An alarm already exists for project / tenant: %s, name: %s",
          tenantId, name);

    // Assert notification methods exist for tenant
    for (String alarmAction : alarmActions)
      if (!notificationMethodRepo.exists(tenantId, alarmAction))
        throw new InvalidEntityException("No notification method exists for %s", alarmAction);

    Map<String, AlarmSubExpression> subAlarms = new HashMap<String, AlarmSubExpression>();
    for (AlarmSubExpression subExpression : alarmExpression.getSubExpressions()) {
      String subAlarmId = UUID.randomUUID().toString();
      subAlarms.put(subAlarmId, subExpression);
    }

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

  public void delete(String tenantId, String alarmId) {
    Map<String, MetricDefinition> subAlarmMetricDefs = repo.findSubAlarmMetricDefinitions(alarmId);
    repo.deleteById(tenantId, alarmId);

    // Notify interested parties of alarm deletion
    String event = Serialization.toJson(new AlarmDeletedEvent(tenantId, alarmId, subAlarmMetricDefs));
    producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));
  }
}
