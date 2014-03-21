package com.hpcloud.mon.resource;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.codahale.metrics.annotation.Timed;
import com.hpcloud.mon.app.AlarmService;
import com.hpcloud.mon.app.command.CreateAlarmCommand;
import com.hpcloud.mon.app.validation.AlarmValidation;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.alarm.AlarmDetail;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;

/**
 * Alarm resource implementation.
 * 
 * @author Jonathan Halterman
 */
@Path("/v2.0/alarms")
public class AlarmResource {
  private final AlarmService service;
  private final AlarmRepository repo;

  @Inject
  public AlarmResource(AlarmService service, AlarmRepository repo) {
    this.service = service;
    this.repo = repo;
  }

  @POST
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @HeaderParam("X-Auth-Token") String authToken, @Valid CreateAlarmCommand command) {
    command.validate();

    AlarmExpression alarmExpression = AlarmValidation.validateNormalizeAndGet(command.expression);
    AlarmDetail alarm = Links.hydrate(service.create(tenantId, command.name, command.description,
        command.expression, alarmExpression, command.alarmActions, command.okActions,
        command.undeterminedActions), uriInfo);
    return Response.created(URI.create(alarm.getId())).entity(alarm).build();
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public List<Alarm> list(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId) {
    return Links.hydrate(repo.find(tenantId), uriInfo);
  }

  @GET
  @Timed
  @Path("{alarm_id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Alarm get(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId) {
    return Links.hydrate(repo.findById(tenantId, alarmId), uriInfo);
  }

  @DELETE
  @Timed
  @Path("{alarm_id}")
  public void delete(@HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId) {
    service.delete(tenantId, alarmId);
  }
}
