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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import com.codahale.metrics.annotation.Timed;

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

import monasca.api.app.MetricService;
import monasca.api.app.command.CreateMetricCommand;
import monasca.api.app.validation.Validation;
import monasca.api.domain.model.metric.MetricDefinitionRepository;
import monasca.api.resource.exception.Exceptions;
import monasca.common.model.Services;
import monasca.common.model.metric.Metric;
import monasca.common.model.metric.MetricDefinition;

/**
 * Metric resource implementation.
 */
@Path("/v2.0/metrics")
public class MetricResource {
  private static final String MONITORING_DELEGATE_ROLE = "monitoring-delegate";
    private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

    private final MetricService service;
  private final MetricDefinitionRepository metricRepo;

  @Inject
  public MetricResource(MetricService service, MetricDefinitionRepository metricRepo) {
    this.service = service;
    this.metricRepo = metricRepo;
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
            .contains(MONITORING_DELEGATE_ROLE);
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
        if (!Strings.isNullOrEmpty(crossTenantId)) {
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
  public Object getMetrics(@Context UriInfo uriInfo,
                                           @HeaderParam("X-Tenant-Id") String tenantId,
                                           @QueryParam("name") String name,
                                           @QueryParam("dimensions") String dimensionsStr,
                                           @QueryParam("offset") String offset) throws Exception {
    Map<String, String>
        dimensions =
        Strings.isNullOrEmpty(dimensionsStr) ? null : Validation
            .parseAndValidateNameAndDimensions(name, dimensionsStr);

    return Links.paginate(offset, metricRepo.find(tenantId, name, dimensions, offset), uriInfo);
  }
}
