/*
 * Copyright (c) 2014-2016 Hewlett Packard Enterprise Development Company, L.P.
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

import java.util.Map;

import javax.inject.Inject;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import monasca.api.ApiConfig;
import monasca.api.app.command.UpdateAlarmCommand;
import monasca.common.model.event.AlarmDeletedEvent;
import monasca.common.model.event.AlarmStateTransitionedEvent;
import monasca.common.model.event.AlarmUpdatedEvent;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.exception.InvalidEntityException;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmRepo;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepo;
import monasca.common.util.Exceptions;
import monasca.common.util.Serialization;

/**
 * Services alarmed metric related requests.
 */
public class AlarmService {
  private static final Logger LOG = LoggerFactory.getLogger(AlarmService.class);

  private final ApiConfig config;
  private final Producer<String, String> producer;
  private final AlarmRepo repo;
  private final AlarmDefinitionRepo alarmDefRepo;
  private long messageCount = 0;

  @Inject
  public AlarmService(ApiConfig config, Producer<String, String> producer,
      AlarmRepo repo, AlarmDefinitionRepo alarmDefRepo) {
    this.config = config;
    this.producer = producer;
    this.repo = repo;
    this.alarmDefRepo = alarmDefRepo;
  }

  /**
   * Deletes the alarm identified by the {@code alarmId
   * }.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   */
  public void delete(String tenantId, String alarmId) {
    Alarm alarm = repo.findById(tenantId, alarmId);
    Map<String, AlarmSubExpression> subAlarmMetricDefs = repo.findAlarmSubExpressions(alarmId);
    repo.deleteById(tenantId, alarmId);

    // Notify interested parties of alarm deletion
    String event =
        Serialization.toJson(new AlarmDeletedEvent(tenantId, alarmId, alarm.getMetrics(), alarm
            .getAlarmDefinition().getId(), subAlarmMetricDefs));
    producer.send(new KeyedMessage<>(config.eventsTopic, String.valueOf(messageCount++), event));
  }

  /**
   * Patches the alarm for the {@code tenantId} and {@code alarmId} to the state of the
   * {@code fields}.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   * @throws InvalidEntityException if one of the actions cannot be found
   */
  public Alarm patch(String tenantId, String alarmId, AlarmState state, String lifecycleState,
                     String link) {
    Alarm oldAlarm = repo.findById(tenantId, alarmId);

    if (state == null && lifecycleState == null && link == null) {
      return oldAlarm;
    }

    state = (state == null) ? oldAlarm.getState() : state;
    lifecycleState = (lifecycleState == null) ? oldAlarm.getLifecycleState() : lifecycleState;
    link = (link == null) ? oldAlarm.getLink() : link;

    Alarm alarm = updateInternal(tenantId, alarmId, state, lifecycleState, link);
    return alarm;
  }

  /**
   * Updates the alarmed metric for the {@code tenantId} and {@code alarmedMetricId} to the state of
   * the {@code command}.
   * 
   * @throws EntityNotFoundException if the alarmed metric cannot be found
   */
  public Alarm update(String tenantId, String alarmId, UpdateAlarmCommand command) {
    Alarm alarm = updateInternal(tenantId, alarmId, command.state, command.lifecycleState, command.link);
    return alarm;
  }

  private String stateChangeReasonFor(AlarmState oldState, AlarmState newState) {
    return "Alarm state updated via API";
  }

  private Alarm updateInternal(String tenantId, String alarmId, AlarmState newState,
                               String newLifecycleState, String newLink) {
    try {
      LOG.debug("Updating alarm {} for tenant {}", alarmId, tenantId);
      final Alarm alarm = repo.update(tenantId, alarmId, newState, newLifecycleState, newLink);
      final AlarmState oldState = alarm.getState();
      // Notify interested parties of updated alarm
      AlarmDefinition alarmDef = alarmDefRepo.findById(tenantId, alarm.getAlarmDefinition().getId());
      Map<String, AlarmSubExpression> subAlarms = repo.findAlarmSubExpressions(alarmId);
      String event =
          Serialization.toJson(new AlarmUpdatedEvent(alarmId, alarmDef.getId(),
              tenantId, alarm.getMetrics(), subAlarms, newState, oldState, newLink, newLifecycleState));
      producer.send(new KeyedMessage<>(config.eventsTopic, String.valueOf(messageCount++), event));

      // Notify interested parties of transitioned alarm state
      if (!oldState.equals(newState)) {
        event =
            Serialization.toJson(new AlarmStateTransitionedEvent(tenantId, alarmId, alarmDef
                .getId(), alarm.getMetrics(), alarmDef.getName(), alarmDef.getDescription(),
                oldState, newState, alarmDef.getSeverity(), newLink, newLifecycleState, alarmDef.isActionsEnabled(),
                stateChangeReasonFor(oldState, newState), null, System.currentTimeMillis()));
        producer.send(new KeyedMessage<>(config.alarmStateTransitionsTopic, String.valueOf(messageCount++), event));
      }
      alarm.setState(newState);
      alarm.setLifecycleState(newLifecycleState);
      alarm.setLink(newLink);
      return alarm;
    } catch (EntityNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw Exceptions.uncheck(e, "Error updating alarm for project / tenant %s", tenantId);
    }
  }
}
