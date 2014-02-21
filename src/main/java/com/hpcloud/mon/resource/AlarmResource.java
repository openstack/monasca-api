package com.hpcloud.mon.resource;

import java.net.URI;

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
import com.hpcloud.mon.app.command.CreateAlarmCommand.CreateAlarmInner;
import com.hpcloud.mon.app.representation.AlarmRepresentation;
import com.hpcloud.mon.app.representation.AlarmsRepresentation;
import com.hpcloud.mon.app.validation.AlarmValidation;
import com.hpcloud.mon.app.validation.Validation;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.model.alarm.AlarmDetail;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;

/**
 * Alarm resource implementation.
 * 
 * @author Jonathan Halterman
 */
@Path("/{version: v1.[2]}/alarms")
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
      @HeaderParam("X-Auth-Token") String authToken, @Valid CreateAlarmCommand wrapper) {
    CreateAlarmInner command = wrapper.alarm;
    command.validate();

    AlarmExpression alarmExpression = AlarmValidation.validateNormalizeAndGet(command.expression);
    for (AlarmSubExpression alarmSubExpr : alarmExpression.getSubExpressions()) {
      MetricDefinition metricDef = alarmSubExpr.getMetricDefinition();
      Validation.verifyOwnership(tenantId, metricDef.namespace, metricDef.dimensions, authToken);
    }

    AlarmDetail alarm = Links.hydrate(service.create(tenantId, command.name, command.expression,
        alarmExpression, command.alarmActions), uriInfo);
    return Response.created(URI.create(alarm.getId()))
        .entity(new AlarmRepresentation(alarm))
        .build();
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public AlarmsRepresentation list(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId) {
    return new AlarmsRepresentation(Links.hydrate(repo.find(tenantId), uriInfo));
  }

  @GET
  @Timed
  @Path("{alarm_id}")
  @Produces(MediaType.APPLICATION_JSON)
  public AlarmRepresentation get(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId, @PathParam("alarm_id") String alarmId) {
    return new AlarmRepresentation(Links.hydrate(repo.findById(tenantId, alarmId), uriInfo));
  }

  @DELETE
  @Timed
  @Path("{alarm_id}")
  public void delete(@HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId) {
    service.delete(tenantId, alarmId);
  }
}
