package com.hpcloud.mon.resource;

import java.util.ArrayList;
import java.util.Arrays;
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

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.hpcloud.mon.app.MetricService;
import com.hpcloud.mon.app.command.CreateMetricCommand;
import com.hpcloud.mon.app.validation.Validation;
import com.hpcloud.mon.common.model.Services;
import com.hpcloud.mon.common.model.metric.Metric;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.model.metric.MetricDefinitionRepository;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * Metric resource implementation.
 * 
 * @author Jonathan Halterman
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
      @HeaderParam("X-Roles") String roles, @QueryParam("tenant_id") String crossTenantId,
      @Valid CreateMetricCommand[] commands) {
    boolean isDelegate = !Strings.isNullOrEmpty(roles)
        && Arrays.asList(COMMA_SPLITTER.split(roles)).contains(MONITORING_DELEGATE_ROLE);
    List<Metric> metrics = new ArrayList<>(commands.length);
    for (CreateMetricCommand command : commands) {
      if (!isDelegate) {
        if (command.dimensions != null) {
          String service = command.dimensions.get(Services.SERVICE_DIMENSION);
          if (service != null && Services.isReserved(service))
            throw Exceptions.forbidden("Project %s cannot POST metrics for the hpcs service",
                tenantId);
        }
        if (!Strings.isNullOrEmpty(crossTenantId))
          throw Exceptions.forbidden("Project %s cannot POST cross tenant metrics", tenantId);
      }

      command.validate();
      metrics.add(command.toMetric());
    }

    service.create(metrics, tenantId, crossTenantId);
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public List<MetricDefinition> getMetrics(@HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("name") String name, @QueryParam("dimensions") String dimensionsStr) {
    Map<String, String> dimensions = Validation.parseAndValidateNameAndDimensions(name,
        dimensionsStr);
    return metricRepo.find(tenantId, name, dimensions);
  }
}
