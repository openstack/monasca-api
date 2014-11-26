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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import monasca.api.MonApiConfiguration;
import monasca.api.app.command.UpdateAlarmCommand;
import monasca.common.model.event.AlarmDeletedEvent;
import monasca.common.model.event.AlarmStateTransitionedEvent;
import monasca.common.model.event.AlarmUpdatedEvent;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.exception.InvalidEntityException;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmRepository;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepository;
import monasca.common.util.Exceptions;
import monasca.common.util.Serialization;

/**
 * Services alarmed metric related requests.
 */
public class AlarmService {
  private static final Logger LOG = LoggerFactory.getLogger(AlarmService.class);

  private final MonApiConfiguration config;
  private final Producer<String, String> producer;
  private final AlarmRepository repo;
  private final AlarmDefinitionRepository alarmDefRepo;

  @Inject
  public AlarmService(MonApiConfiguration config, Producer<String, String> producer,
      AlarmRepository repo, AlarmDefinitionRepository alarmDefRepo) {
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
    List<MetricDefinition> metrics = repo.findMetrics(tenantId, alarmId);
    repo.deleteById(tenantId, alarmId);

    // Notify interested parties of alarm deletion
    String event =
        Serialization.toJson(new AlarmDeletedEvent(tenantId, alarmId, metrics, alarm
            .getAlarmDefinition().getId(), subAlarmMetricDefs));
    producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));
  }

  /**
   * Patches the alarm for the {@code tenantId} and {@code alarmId} to the state of the
   * {@code fields}.
   * 
   * @throws EntityNotFoundException if the alarm cannot be found
   * @throws InvalidEntityException if one of the actions cannot be found
   */
  public Alarm patch(String tenantId, String alarmId, AlarmState state) {
    Alarm alarm = repo.findById(tenantId, alarmId);
    state = state == null ? alarm.getState() : state;
    updateInternal(tenantId, alarm, alarm.getState(), state);
    alarm.setState(state);
    return alarm;
  }

  /**
   * Updates the alarmed metric for the {@code tenantId} and {@code alarmedMetricId} to the state of
   * the {@code command}.
   * 
   * @throws EntityNotFoundException if the alarmed metric cannot be found
   */
  public Alarm update(String tenantId, String alarmId, UpdateAlarmCommand command) {
    Alarm alarm = repo.findById(tenantId, alarmId);
    updateInternal(tenantId, alarm, alarm.getState(), command.state);
    alarm.setState(command.state);
    return alarm;
  }

  private String stateChangeReasonFor(AlarmState oldState, AlarmState newState) {
    return "Alarm state updated via API";
  }

  private void updateInternal(String tenantId, Alarm alarm, AlarmState oldState, AlarmState newState) {
    try {
      LOG.debug("Updating alarm {} for tenant {}", alarm.getId(), tenantId);
      repo.update(tenantId, alarm.getId(), newState);

      // Notify interested parties of updated alarm
      AlarmDefinition alarmDef = alarmDefRepo.findById(tenantId, alarm.getAlarmDefinition().getId());
      List<MetricDefinition> metrics = repo.findMetrics(tenantId, alarm.getId());
      Map<String, AlarmSubExpression> subAlarms = repo.findAlarmSubExpressions(alarm.getId());
      String event =
          Serialization.toJson(new AlarmUpdatedEvent(alarm.getId(), alarm.getAlarmDefinition().getId(),
              tenantId, metrics, subAlarms, newState, oldState));
      producer.send(new KeyedMessage<>(config.eventsTopic, tenantId, event));

      // Notify interested parties of transitioned alarm state
      if (!oldState.equals(newState)) {
        event =
            Serialization.toJson(new AlarmStateTransitionedEvent(tenantId, alarm.getId(), alarmDef
                .getId(), alarm.getMetrics(), alarmDef.getName(), alarmDef.getDescription(),
                oldState, newState, alarmDef.getSeverity(), alarmDef.isActionsEnabled(),
                stateChangeReasonFor(oldState, newState), System.currentTimeMillis() / 1000));
        producer.send(new KeyedMessage<>(config.alarmStateTransitionsTopic, tenantId, event));
      }
    } catch (Exception e) {
      throw Exceptions.uncheck(e, "Error updating alarm for project / tenant %s", tenantId);
    }
  }
}
