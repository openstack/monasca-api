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
import com.hpcloud.mon.app.command.CreateNotificationMethodCommand;
import com.hpcloud.mon.app.command.CreateNotificationMethodCommand.CreateNotificationMethodInner;
import com.hpcloud.mon.app.representation.NotificationMethodRepresentation;
import com.hpcloud.mon.app.representation.NotificationMethodsRepresentation;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;

/**
 * Notification Method resource implementation.
 * 
 * @author Jonathan Halterman
 */
@Path("/{version: v1.[2]}/notification-methods")
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
  public Response create(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @Valid CreateNotificationMethodCommand wrapper) {
    CreateNotificationMethodInner command = wrapper.notificationMethod;
    command.validate();

    NotificationMethod notificationMethod = Links.hydrate(
        repo.create(tenantId, command.name, command.type, command.address), uriInfo);
    return Response.created(URI.create(notificationMethod.getId()))
        .entity(new NotificationMethodRepresentation(notificationMethod))
        .build();
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public NotificationMethodsRepresentation list(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId) {
    return new NotificationMethodsRepresentation(Links.hydrate(repo.find(tenantId), uriInfo));
  }

  @GET
  @Timed
  @Path("{notification_method_id}")
  @Produces(MediaType.APPLICATION_JSON)
  public NotificationMethodRepresentation get(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("notification_method_id") String notificationMethodId) {
    return new NotificationMethodRepresentation(Links.hydrate(
        repo.findById(tenantId, notificationMethodId), uriInfo));
  }

  @DELETE
  @Timed
  @Path("{notification_method_id}")
  public void delete(@HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("notification_method_id") String notificationMethodId) {
    repo.deleteById(tenantId, notificationMethodId);
  }
}
