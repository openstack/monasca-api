package com.hpcloud.mon.resource;

import java.net.URI;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.hibernate.validator.constraints.NotEmpty;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.hpcloud.mon.app.AlarmService;
import com.hpcloud.mon.app.command.CreateAlarmCommand;
import com.hpcloud.mon.app.command.UpdateAlarmCommand;
import com.hpcloud.mon.app.validation.AlarmValidation;
import com.hpcloud.mon.app.validation.Validation;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.alarmhistory.AlarmHistory;
import com.hpcloud.mon.domain.model.alarmhistory.AlarmHistoryRepository;
import com.hpcloud.mon.resource.annotation.PATCH;

/**
 * Alarm resource implementation.
 * 
 * @author Jonathan Halterman
 */
@Path("/v2.0/alarms")
public class AlarmResource {
  private final AlarmService service;
  private final AlarmRepository repo;
  private final AlarmHistoryRepository alarmHistoryRepo;

  @Inject
  public AlarmResource(AlarmService service, AlarmRepository repo,
      AlarmHistoryRepository alarmHistoryRepo) {
    this.service = service;
    this.repo = repo;
    this.alarmHistoryRepo = alarmHistoryRepo;
  }

  @POST
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @Valid CreateAlarmCommand command) {
    command.validate();
    AlarmExpression alarmExpression = AlarmValidation.validateNormalizeAndGet(command.expression);
    Alarm alarm = Links.hydrate(service.create(tenantId, command.name, command.description,
        command.expression, alarmExpression, command.alarmActions, command.okActions,
        command.undeterminedActions), uriInfo, "history");
    return Response.created(URI.create(alarm.getId())).entity(alarm).build();
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public List<Alarm> list(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId) {
    return Links.hydrate(repo.find(tenantId), uriInfo, "history");
  }

  @GET
  @Timed
  @Path("{alarm_id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Alarm get(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId) {
    return Links.hydrate(repo.findById(tenantId, alarmId), uriInfo, "history");
  }

  @GET
  @Timed
  @Path("{alarm_id}/history")
  @Produces(MediaType.APPLICATION_JSON)
  public List<AlarmHistory> getHistory(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId, @PathParam("alarm_id") String alarmId) {
    // return Links.hydrate(repo.findById(tenantId, alarmId), uriInfo);
    return null;
  }

  @PUT
  @Timed
  @Path("{alarm_id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Alarm update(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId, @Valid UpdateAlarmCommand command) {
    command.validate();
    AlarmExpression alarmExpression = AlarmValidation.validateNormalizeAndGet(command.expression);
    return Links.hydrate(service.update(tenantId, alarmId, alarmExpression, command), uriInfo,
        "history");
  }

  @PATCH
  @Timed
  @Path("{alarm_id}")
  @Consumes("application/json-patch+json")
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings("unchecked")
  public Alarm patch(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId, @NotEmpty Map<String, Object> fields)
      throws JsonMappingException {
    String name = (String) fields.get("name");
    String description = (String) fields.get("description");
    String expression = (String) fields.get("name");
    String stateStr = (String) fields.get("state");
    AlarmState state = stateStr == null ? null : Validation.parseAndValidate(AlarmState.class,
        stateStr);
    Boolean enabled = (Boolean) fields.get("enabled");
    List<String> alarmActions = (List<String>) fields.get("alarm_actions");
    List<String> okActions = (List<String>) fields.get("ok_actions");
    List<String> undeterminedActions = (List<String>) fields.get("undetermined_actions");
    AlarmValidation.validate(name, description, alarmActions, okActions, undeterminedActions);
    AlarmExpression alarmExpression = expression == null ? null
        : AlarmValidation.validateNormalizeAndGet(expression);

    return Links.hydrate(service.patch(tenantId, alarmId, name, description, expression,
        alarmExpression, state, enabled, alarmActions, okActions, undeterminedActions), uriInfo,
        "history");
  }

  @DELETE
  @Timed
  @Path("{alarm_id}")
  public void delete(@HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId) {
    service.delete(tenantId, alarmId);
  }
}
