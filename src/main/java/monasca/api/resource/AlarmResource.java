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
package monasca.api.resource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Strings;
import monasca.api.app.AlarmService;
import monasca.api.app.command.UpdateAlarmCommand;
import monasca.api.app.validation.Validation;
import monasca.common.model.alarm.AlarmState;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmRepository;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistory;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import monasca.api.resource.annotation.PATCH;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Alarm resource implementation.
 */
@Path("/v2.0/alarms")
@Api(value = "/v2.0/alarms", description = "Operations for accessing alarms")
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
  @Path("/{alarm_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get alarm", response = Alarm.class)
  @ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid ID supplied"),
      @ApiResponse(code = 404, message = "Alarm not found")})
  public Alarm get(
      @ApiParam(value = "ID of alarm to fetch", required = true) @Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId, @PathParam("alarm_id") String alarm_id) {
    return fixAlarmLinks(uriInfo, repo.findById(alarm_id));
  }

  private Alarm fixAlarmLinks(UriInfo uriInfo, Alarm alarm) {
    Links.hydrate(alarm.getAlarmDefinition(), uriInfo,
        AlarmDefinitionResource.ALARM_DEFINITIONS_PATH);
    return Links.hydrate(alarm, uriInfo, true);
  }

  @GET
  @Timed
  @Path("/{alarm_id}/state-history")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get alarm state history", response = AlarmStateHistory.class,
      responseContainer = "List")
  public List<AlarmStateHistory> getStateHistory(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId, @PathParam("alarm_id") String alarmId)
      throws Exception {
    return stateHistoryRepo.findById(tenantId, alarmId);
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "List alarms", response = Alarm.class, responseContainer = "List")
  public List<Alarm> list(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("alarm_definition_id") String alarmDefId,
      @QueryParam("metric_name") String metricName,
      @QueryParam("metric_dimensions") String metricDimensionsStr,
      @QueryParam("state") AlarmState state) throws Exception {
    Map<String, String> metricDimensions =
        Strings.isNullOrEmpty(metricDimensionsStr) ? null : Validation
            .parseAndValidateNameAndDimensions(metricName, metricDimensionsStr);
    final List<Alarm> alarms = repo.find(tenantId, alarmDefId, metricName, metricDimensions, state);
    for (final Alarm alarm : alarms) {
      Links.hydrate(alarm.getAlarmDefinition(), uriInfo, AlarmDefinitionResource.ALARM_DEFINITIONS_PATH);
    }
    return Links.hydrate(alarms, uriInfo);
  }

  @GET
  @Timed
  @Path("/state-history")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "List alarm state history", response = AlarmDefinition.class,
      responseContainer = "List")
  public Collection<AlarmStateHistory> listStateHistory(
      @HeaderParam("X-Tenant-Id") String tenantId, @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("start_time") String startTimeStr, @QueryParam("end_time") String endTimeStr)
      throws Exception {

    // Validate query parameters
    DateTime startTime = Validation.parseAndValidateDate(startTimeStr, "start_time", false);
    DateTime endTime = Validation.parseAndValidateDate(endTimeStr, "end_time", false);
    if (startTime != null)
      Validation.validateTimes(startTime, endTime);
    Map<String, String> dimensions =
        Strings.isNullOrEmpty(dimensionsStr) ? null : Validation
            .parseAndValidateDimensions(dimensionsStr);

    return stateHistoryRepo.find(tenantId, dimensions, startTime, endTime);
  }

  @PATCH
  @Timed
  @Path("/{alarm_id}")
  @Consumes("application/json-patch+json")
  @Produces(MediaType.APPLICATION_JSON)
  public Alarm patch(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId, @NotEmpty Map<String, Object> fields)
      throws JsonMappingException {
    String stateStr = (String) fields.get("state");
    AlarmState state =
        stateStr == null ? null : Validation.parseAndValidate(AlarmState.class, stateStr);

    return fixAlarmLinks(uriInfo, service.patch(tenantId, alarmId, state));
  }

  @PUT
  @Timed
  @Path("/{alarm_id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update alarm", response = Alarm.class)
  public Alarm update(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId, @Valid UpdateAlarmCommand command) {

    return fixAlarmLinks(uriInfo, service.update(tenantId, alarmId, command));
  }
}
