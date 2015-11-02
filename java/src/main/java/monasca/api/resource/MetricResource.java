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

import static monasca.api.app.validation.Validation.DEFAULT_ADMIN_ROLE;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import com.codahale.metrics.annotation.Timed;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import monasca.api.ApiConfig;
import monasca.api.app.MetricService;
import monasca.api.app.command.CreateMetricCommand;
import monasca.api.app.validation.MetricNameValidation;
import monasca.api.app.validation.Validation;
import monasca.api.domain.model.metric.MetricDefinitionRepo;
import monasca.api.domain.model.metric.MetricName;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.api.resource.exception.Exceptions;
import monasca.common.model.Services;
import monasca.common.model.metric.Metric;
import monasca.common.model.metric.MetricDefinition;

/**
 * Metric resource implementation.
 */
@Path("/v2.0/metrics")
public class MetricResource {

  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

  private final String monitoring_delegate_role;
  private final String admin_role;
  private final MetricService service;
  private final MetricDefinitionRepo metricRepo;
  private final PersistUtils persistUtils;

  @Inject
  public MetricResource(ApiConfig config, MetricService service, MetricDefinitionRepo metricRepo,
                        PersistUtils persistUtils) {

    this.monitoring_delegate_role = (config.middleware == null || config.middleware.delegateAuthorizedRole == null)
                                    ? "monitoring-delegate" : config.middleware.delegateAuthorizedRole;

    this.admin_role = (config.middleware == null || config.middleware.adminRole == null)
                      ? DEFAULT_ADMIN_ROLE : config.middleware.adminRole;

    this.service = service;
    this.metricRepo = metricRepo;
    this.persistUtils = persistUtils;
  }

  @POST
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  public void create(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
                     @HeaderParam("X-Roles") String roles,
                     @QueryParam("tenant_id") String crossTenantId,
                     @Valid CreateMetricCommand[] commands) {
    boolean
        isDelegate =
        !Strings.isNullOrEmpty(roles) && COMMA_SPLITTER.splitToList(roles)
            .contains(monitoring_delegate_role);
    List<Metric> metrics = new ArrayList<>(commands.length);
    for (CreateMetricCommand command : commands) {
      if (!isDelegate) {
        if (command.dimensions != null) {
          String service = command.dimensions.get(Services.SERVICE_DIMENSION);
          if (service != null && Services.isReserved(service)) {
            throw Exceptions
                .forbidden("Project %s cannot POST metrics for the hpcs service", tenantId);
          }
        }
        if (Validation.isCrossProjectRequest(crossTenantId, tenantId)) {
          throw Exceptions.forbidden("Project %s cannot POST cross tenant metrics", tenantId);
        }
      }

      command.validate();
      metrics.add(command.toMetric());
    }

    service.create(metrics, tenantId, crossTenantId);
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public Object getMetrics(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
                           @HeaderParam("X-Roles") String roles,
                           @QueryParam("name") String name,
                           @QueryParam("dimensions") String dimensionsStr,
                           @QueryParam("offset") String offset,
                           @QueryParam("limit") String limit,
                           @QueryParam("start_time") String startTimeStr,
                           @QueryParam("end_time") String endTimeStr,
                           @QueryParam("tenant_id") String crossTenantId) throws Exception
  {
      Map<String, String>
        dimensions =
          Strings.isNullOrEmpty(dimensionsStr) ? null : Validation
              .parseAndValidateDimensions(dimensionsStr);
      MetricNameValidation.validate(name, false);

    DateTime startTime = Validation.parseAndValidateDate(startTimeStr, "start_time", false);
    DateTime endTime = Validation.parseAndValidateDate(endTimeStr, "end_time", false);

    if ((startTime != null) && (endTime != null)) {
      //
      // If both times are specified, make sure start is before end
      //
      Validation.validateTimes(startTime, endTime);
    }

    final String queryTenantId = Validation.getQueryProject(roles, crossTenantId, tenantId,
        admin_role);
    final int paging_limit = this.persistUtils.getLimit(limit);
    final List<MetricDefinition> resources = metricRepo.find(
        queryTenantId,
        name,
        dimensions,
        startTime,
        endTime,
        offset,
        paging_limit
    );

    return Links.paginate(paging_limit, resources, uriInfo);
  }

  @GET
  @Path("/names")
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public Object getMetricNames(@Context UriInfo uriInfo,
                               @HeaderParam("X-Tenant-Id") String tenantId,
                               @HeaderParam("X-Roles") String roles,
                               @QueryParam("dimensions") String dimensionsStr,
                               @QueryParam("offset") String offset,
                               @QueryParam("limit") String limit,
                               @QueryParam("tenant_id") String crossTenantId) throws Exception
  {
    Map<String, String>
        dimensions =
        Strings.isNullOrEmpty(dimensionsStr) ? null : Validation
            .parseAndValidateDimensions(dimensionsStr);

    String queryTenantId = Validation.getQueryProject(roles, crossTenantId, tenantId, admin_role);

    final int paging_limit = this.persistUtils.getLimit(limit);
    final List<MetricName> resources = metricRepo.findNames(
        queryTenantId,
        dimensions,
        offset,
        paging_limit
    );
    return Links.paginate(paging_limit, resources, uriInfo);
  }

}
