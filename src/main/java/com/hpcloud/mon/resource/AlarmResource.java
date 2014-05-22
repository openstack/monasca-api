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
package com.hpcloud.mon.resource;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Strings;
import com.hpcloud.mon.app.AlarmService;
import com.hpcloud.mon.app.command.CreateAlarmCommand;
import com.hpcloud.mon.app.command.UpdateAlarmCommand;
import com.hpcloud.mon.app.validation.AlarmValidation;
import com.hpcloud.mon.app.validation.Validation;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistory;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import com.hpcloud.mon.resource.annotation.PATCH;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Alarm resource implementation.
 */
@Path("/v2.0/alarms")
@Api(value = "/v2.0/alarms", description = "Operations for working with alarms")
public class AlarmResource {
  private final AlarmService service;
  private final AlarmRepository repo;
  private final AlarmStateHistoryRepository stateHistoryRepo;

  @Inject
  public AlarmResource(AlarmService service, AlarmRepository repo,
      AlarmStateHistoryRepository stateHistoryRepo) {
    this.service = service;
    this.repo = repo;
    this.stateHistoryRepo = stateHistoryRepo;
  }

  @POST
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create alarm", response = Alarm.class)
  public Response create(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @Valid CreateAlarmCommand command) {
    command.validate();
    AlarmExpression alarmExpression = AlarmValidation.validateNormalizeAndGet(command.expression);
    Alarm alarm = Links.hydrate(service.create(tenantId, command.name, command.description, command.severity,
      command.expression, alarmExpression, command.alarmActions, command.okActions,
      command.undeterminedActions), uriInfo, false, "history");
    return Response.created(URI.create(alarm.getId())).entity(alarm).build();
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "List alarms", response = Alarm.class, responseContainer = "List")
  public List<Alarm> list(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("dimensions") String dimensionsStr, @QueryParam("state") String state) {

    Map<String, String> dimensions = Strings.isNullOrEmpty(dimensionsStr) ? null
        : Validation.parseAndValidateDimensions(dimensionsStr);
    if (state != null) {
      Validation.validateAlarmState(state);
    }
    return Links.hydrate(repo.find(tenantId, dimensions, state), uriInfo, "history");

  }

  @GET
  @Timed
  @Path("/state-history")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "List alarm state history", response = Alarm.class,
      responseContainer = "List")
  public Collection<AlarmStateHistory> listStateHistory(
      @HeaderParam("X-Tenant-Id") String tenantId, @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("start_time") String startTimeStr, @QueryParam("end_time") String endTimeStr) {

    // Validate query parameters
    DateTime startTime = Validation.parseAndValidateDate(startTimeStr, "start_time", false);
    DateTime endTime = Validation.parseAndValidateDate(endTimeStr, "end_time", false);
    if (startTime != null)
      Validation.validateTimes(startTime, endTime);
    Map<String, String> dimensions = Strings.isNullOrEmpty(dimensionsStr) ? null
        : Validation.parseAndValidateDimensions(dimensionsStr);

    return stateHistoryRepo.find(tenantId, dimensions, startTime, endTime);
  }

  @GET
  @Timed
  @Path("/{alarm_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get alarm", response = Alarm.class)
  @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid ID supplied"),
      @ApiResponse(code = 404, message = "Alarm not found") })
  public Alarm get(
      @ApiParam(value = "ID of alarm to fetch", required = true) @Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId, @PathParam("alarm_id") String alarmId) {
    return Links.hydrate(repo.findById(tenantId, alarmId), uriInfo, true, "history");
  }

  @PUT
  @Timed
  @Path("/{alarm_id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update alarm", response = Alarm.class)
  public Alarm update(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId, @Valid UpdateAlarmCommand command) {
    command.validate();
    AlarmExpression alarmExpression = AlarmValidation.validateNormalizeAndGet(command.expression);
    return Links.hydrate(service.update(tenantId, alarmId, alarmExpression, command), uriInfo,
        true, "history");
  }

  @PATCH
  @Timed
  @Path("/{alarm_id}")
  @Consumes("application/json-patch+json")
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings("unchecked")
  public Alarm patch(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId, @NotEmpty Map<String, Object> fields)
      throws JsonMappingException {
    String name = (String) fields.get("name");
    String description = (String) fields.get("description");
    String severity = (String) fields.get("severity");
    String expression = (String) fields.get("expression");
    String stateStr = (String) fields.get("state");
    AlarmState state = stateStr == null ? null : Validation.parseAndValidate(AlarmState.class,
        stateStr);
    Boolean enabled = (Boolean) fields.get("actions_enabled");
    List<String> alarmActions = (List<String>) fields.get("alarm_actions");
    List<String> okActions = (List<String>) fields.get("ok_actions");
    List<String> undeterminedActions = (List<String>) fields.get("undetermined_actions");
    AlarmValidation.validate(name, description, severity, alarmActions, okActions, undeterminedActions);
    AlarmExpression alarmExpression = expression == null ? null
        : AlarmValidation.validateNormalizeAndGet(expression);

    return Links.hydrate(service.patch(tenantId, alarmId, name, description, severity, expression,
      alarmExpression, state, enabled, alarmActions, okActions, undeterminedActions), uriInfo,
      true, "history");
  }

  @DELETE
  @Timed
  @Path("/{alarm_id}")
  @ApiOperation(value = "Delete alarm")
  public void delete(@HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId) {
    service.delete(tenantId, alarmId);
  }

  @GET
  @Timed
  @Path("/{alarm_id}/state-history")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get alarm state history", response = AlarmStateHistory.class,
      responseContainer = "List")
  public List<AlarmStateHistory> getStateHistory(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId, @PathParam("alarm_id") String alarmId) {
    return stateHistoryRepo.findById(tenantId, alarmId);
  }
}
