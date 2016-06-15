/*
 * Copyright (c) 2014,2016 Hewlett Packard Enterprise Development Company, L.P.
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

import com.google.common.base.Strings;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
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

import monasca.api.app.AlarmDefinitionService;
import monasca.api.app.command.CreateAlarmDefinitionCommand;
import monasca.api.app.command.PatchAlarmDefinitionCommand;
import monasca.api.app.command.UpdateAlarmDefinitionCommand;
import monasca.api.app.validation.AlarmValidation;
import monasca.api.app.validation.Validation;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepo;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.api.resource.annotation.PATCH;
import monasca.common.model.alarm.AlarmExpression;
import monasca.common.model.alarm.AlarmSeverity;

/**
 * Alarm definition resource implementation.
 */
@Path(AlarmDefinitionResource.ALARM_DEFINITIONS_PATH)
public class AlarmDefinitionResource {
  private final AlarmDefinitionService service;
  private final AlarmDefinitionRepo repo;
  private final PersistUtils persistUtils;
  public final static String ALARM_DEFINITIONS = "alarm-definitions";
  public final static String ALARM_DEFINITIONS_PATH = "/v2.0/" + ALARM_DEFINITIONS;
  private final static List<String> ALLOWED_SORT_BY = Arrays.asList("id", "name", "severity",
                                                                    "updated_at", "created_at");

  @Inject
  public AlarmDefinitionResource(AlarmDefinitionService service,
                                 AlarmDefinitionRepo repo,
                                 PersistUtils persistUtils) {
    this.service = service;
    this.repo = repo;
    this.persistUtils = persistUtils;
  }

  @POST
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @Valid CreateAlarmDefinitionCommand command) {
    command.validate();
    AlarmExpression alarmExpression = AlarmValidation.validateNormalizeAndGet(command.expression);
    AlarmDefinition alarm =
        Links.hydrate(service.create(tenantId, command.name, command.description, command.severity,
            command.expression, alarmExpression, command.matchBy, command.alarmActions,
            command.okActions, command.undeterminedActions), uriInfo, false);
    return Response.created(URI.create(alarm.getId())).entity(alarm).build();
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public Object list(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId, @QueryParam("name") String name,
      @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("severity") String severityStr,
      @QueryParam("sort_by") String sortByStr,
      @QueryParam("offset") String offset,
      @QueryParam("limit") String limit) throws UnsupportedEncodingException {
    Map<String, String> dimensions =
        Strings.isNullOrEmpty(dimensionsStr) ? null : Validation
            .parseAndValidateDimensions(dimensionsStr);

    List<String> sortByList = Validation.parseAndValidateSortBy(sortByStr, ALLOWED_SORT_BY);
    if (!Strings.isNullOrEmpty(offset)) {
      Validation.parseAndValidateNumber(offset, "offset");
    }

    List<AlarmSeverity> severityList = Validation.parseAndValidateSeverity(severityStr);

    final int paging_limit = this.persistUtils.getLimit(limit);
    final List<AlarmDefinition> resources = repo.find(tenantId,
                                                      name,
                                                      dimensions,
                                                      severityList,
                                                      sortByList,
                                                      offset,
                                                      paging_limit
    );
    return Links.paginateAlarming(paging_limit, Links.hydrate(resources, uriInfo), uriInfo);
  }

  @GET
  @Timed
  @Path("/{alarm_definition_id}")
  @Produces(MediaType.APPLICATION_JSON)
  public AlarmDefinition get(
      @Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_definition_id") String alarmDefinitionId) {
    return Links.hydrate(repo.findById(tenantId, alarmDefinitionId), uriInfo, true);
  }

  @PUT
  @Timed
  @Path("/{alarm_definition_id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public AlarmDefinition update(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_definition_id") String alarmDefinitionId,
      @Valid UpdateAlarmDefinitionCommand command) {
    command.validate();
    AlarmExpression alarmExpression = AlarmValidation.validateNormalizeAndGet(command.expression);
    return Links.hydrate(service.update(tenantId, alarmDefinitionId, alarmExpression, command),
                         uriInfo, true);
  }

  @PATCH
  @Timed
  @Path("/{alarm_definition_id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public AlarmDefinition patch(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_definition_id") String alarmDefinitionId,
      @Valid PatchAlarmDefinitionCommand command) throws JsonMappingException {
    command.validate();
    AlarmExpression alarmExpression =
        command.expression == null ? null : AlarmValidation
            .validateNormalizeAndGet(command.expression);

    return Links.hydrate(service.patch(tenantId, alarmDefinitionId, command.name,
                                       command.description, command.severity, command.expression,
                                       alarmExpression, command.matchBy, command.actionsEnabled,
                                       command.alarmActions, command.okActions,
                                       command.undeterminedActions),
                         uriInfo, true);
  }

  @DELETE
  @Timed
  @Path("/{alarm_definition_id}")
  public void delete(@HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_definition_id") String alarmDefinitionId) {
    service.delete(tenantId, alarmDefinitionId);
  }
}
