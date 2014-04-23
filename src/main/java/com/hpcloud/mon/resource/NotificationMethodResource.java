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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.codahale.metrics.annotation.Timed;
import com.hpcloud.mon.app.command.CreateNotificationMethodCommand;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * Notification Method resource implementation.
 * 
 * @author Jonathan Halterman
 */
@Path("/v2.0/notification-methods")
@Api(value = "/v2.0/notification-methods",
    description = "Operations for working with notification methods")
public class NotificationMethodResource {
  private final NotificationMethodRepository repo;

  @Inject
  public NotificationMethodResource(NotificationMethodRepository repo) {
    this.repo = repo;
  }

  @POST
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create notification method", response = NotificationMethod.class)
  public Response create(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @Valid CreateNotificationMethodCommand command) {
    command.validate();

    NotificationMethod notificationMethod = Links.hydrate(
        repo.create(tenantId, command.name, command.type, command.address), uriInfo);
    return Response.created(URI.create(notificationMethod.getId()))
        .entity(notificationMethod)
        .build();
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "List notification methods", response = NotificationMethod.class,
      responseContainer = "List")
  public List<NotificationMethod> list(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId) {
    return Links.hydrate(repo.find(tenantId), uriInfo);
  }

  @GET
  @Timed
  @Path("/{notification_method_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get notification method", response = NotificationMethod.class)
  public NotificationMethod get(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("notification_method_id") String notificationMethodId) {
    return Links.hydrate(repo.findById(tenantId, notificationMethodId), uriInfo);
  }

  @PUT
  @Timed
  @Path("/{notification_method_id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update notification method", response = NotificationMethod.class)
  public NotificationMethod update(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("notification_method_id") String notificationMethodId,
      @Valid CreateNotificationMethodCommand command) {
    command.validate();

    return Links.hydrate(
        repo.update(tenantId, notificationMethodId, command.name, command.type, command.address),
        uriInfo);
  }

  @DELETE
  @Timed
  @Path("/{notification_method_id}")
  @ApiOperation(value = "Delete notification method")
  public void delete(@HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("notification_method_id") String notificationMethodId) {
    repo.deleteById(tenantId, notificationMethodId);
  }
}
