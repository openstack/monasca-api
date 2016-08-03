/*
 * (C) Copyright 2014-2016 Hewlett Packard Enterprise Development LP
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

import com.codahale.metrics.annotation.Timed;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import monasca.api.ApiConfig;
import monasca.api.app.command.CreateNotificationMethodCommand;
import monasca.api.app.command.PatchNotificationMethodCommand;
import monasca.api.app.command.UpdateNotificationMethodCommand;
import monasca.api.app.validation.NotificationMethodValidation;
import monasca.api.app.validation.Validation;
import monasca.api.domain.model.notificationmethod.NotificationMethod;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepo;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.api.resource.annotation.PATCH;

/**
 * Notification Method resource implementation.
 */
@Path("/v2.0/notification-methods")
public class NotificationMethodResource {
  private final NotificationMethodRepo repo;
  private final PersistUtils persistUtils;
  private final static List<String> ALLOWED_SORT_BY = Arrays.asList("id", "name", "type",
                                                                    "address", "updated_at",
                                                                    "created_at");

  private final List<Integer> validPeriods;


  @Inject
  public NotificationMethodResource(ApiConfig config, NotificationMethodRepo repo,
                                    PersistUtils persistUtils) {
    this.repo = repo;
    this.persistUtils = persistUtils;
    this.validPeriods = config.validNotificationPeriods == null ? Arrays.asList(0, 60):
            config.validNotificationPeriods;

  }

  @POST
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @Valid CreateNotificationMethodCommand command) {
    command.validate(this.validPeriods);

    NotificationMethod notificationMethod =
        Links.hydrate(repo.create(tenantId, command.name, command.type,
                command.address, command.getConvertedPeriod()), uriInfo,
            false);
    return Response.created(URI.create(notificationMethod.getId())).entity(notificationMethod)
        .build();
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public Object list(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
                     @QueryParam("sort_by") String sortByStr,
                     @QueryParam("offset") String offset,
                     @QueryParam("limit") String limit) throws UnsupportedEncodingException {

    List<String> sortByList = Validation.parseAndValidateSortBy(sortByStr, ALLOWED_SORT_BY);

    final int paging_limit = this.persistUtils.getLimit(limit);
    final List<NotificationMethod> resources = repo.find(tenantId, sortByList, offset,
                                                         paging_limit);
    return Links.paginate(paging_limit,
                          Links.hydrate(resources, uriInfo),
                          uriInfo);

  }

  @GET
  @Timed
  @Path("/{notification_method_id}")
  @Produces(MediaType.APPLICATION_JSON)
  public NotificationMethod get(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("notification_method_id") String notificationMethodId) {
    return Links.hydrate(repo.findById(tenantId, notificationMethodId), uriInfo, true);
  }

  @PUT
  @Timed
  @Path("/{notification_method_id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public NotificationMethod update(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("notification_method_id") String notificationMethodId,
      @Valid UpdateNotificationMethodCommand command) {
    command.validate(this.validPeriods);

    return Links.hydrate(
        repo.update(tenantId, notificationMethodId, command.name, command.type,
                command.address, command.getConvertedPeriod()),
        uriInfo, true);
  }

  @PATCH
  @Timed
  @Path("/{notification_method_id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public NotificationMethod patch(@Context UriInfo uriInfo,
                                  @HeaderParam("X-Tenant-Id") String tenantId,
                                  @PathParam("notification_method_id") String notificationMethodId,
                                  @Valid PatchNotificationMethodCommand command) {
    NotificationMethod originalNotificationMethod = repo.findById(tenantId, notificationMethodId);
    String name = command.name == null ? originalNotificationMethod.getName()
            : command.name;
    String type = command.type == null ? originalNotificationMethod.getType()
            : command.type;
    String address = command.address == null ? originalNotificationMethod.getAddress()
            : command.address;
    int period = command.period == null ? originalNotificationMethod.getPeriod()
            : command.getConvertedPeriod();

    NotificationMethodValidation.validate(type, address, period, this.validPeriods);

    return Links.hydrate(
            repo.update(tenantId, notificationMethodId, name, type,
                    address, period),
            uriInfo, true);
  }

  @DELETE
  @Timed
  @Path("/{notification_method_id}")
  public void delete(@HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("notification_method_id") String notificationMethodId) {
    repo.deleteById(tenantId, notificationMethodId);
  }
}
