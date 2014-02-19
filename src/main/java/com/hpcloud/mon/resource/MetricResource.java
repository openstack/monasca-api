package com.hpcloud.mon.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.app.MetricService;
import com.hpcloud.mon.app.command.CreateMetricCommand;
import com.hpcloud.mon.app.validate.DimensionValidation;
import com.hpcloud.mon.app.validate.MetricValidation;
import com.hpcloud.mon.app.validate.NamespaceValidation;
import com.hpcloud.mon.app.validate.Validation;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.common.model.metric.Metric;
import com.hpcloud.mon.domain.model.metric.Datapoint;
import com.hpcloud.mon.domain.model.metric.DatapointRepository;
import com.hpcloud.mon.resource.exception.Exceptions;
import com.hpcloud.mon.resource.exception.Exceptions.FaultType;

/**
 * Metric resource implementation.
 * 
 * @author Todd Walk
 * @author Jonathan Halterman
 */
@Path("/{version: v1.[0-1]}/metrics")
public class MetricResource {
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();
  private static final Splitter COLON_SPLITTER = Splitter.on(':').omitEmptyStrings().trimResults();

  private final MetricService service;
  private final DatapointRepository datapointRepo;
  private final MonApiConfiguration config;

  @Inject
  public MetricResource(MetricService service, DatapointRepository datapointRepo,
      MonApiConfiguration config) {
    this.service = service;
    this.datapointRepo = datapointRepo;
    this.config = config;
  }

  @POST
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  public void create(@Context UriInfo uriInfo, @PathParam("version") String version,
      @HeaderParam("X-Auth-Token") String authToken, @HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("tenant_id") String crossTenantId, @Valid CreateMetricCommand newMetric) {
    Metric metric = validateAndConvert(version, newMetric, tenantId, crossTenantId);
    service.create(metric, tenantId, crossTenantId, authToken);
  }

  @POST
  @Timed
  @Path("aggregate")
  @Consumes(MediaType.APPLICATION_JSON)
  public void create(@Context UriInfo uriInfo, @PathParam("version") String version,
      @HeaderParam("X-Auth-Token") String authToken, @HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("tenant_id") String crossTenantId, @Valid CreateMetricCommand[] newMetrics) {
    if ("v1.0".equals(version))
      throw new WebApplicationException(FaultType.NOT_FOUND.statusCode);

    List<Metric> flatMetrics = new ArrayList<Metric>();
    for (CreateMetricCommand newMetric : newMetrics)
      flatMetrics.add(validateAndConvert("v1.1", newMetric, tenantId, crossTenantId));

    for (Metric metric : flatMetrics)
      service.create(metric, tenantId, crossTenantId, authToken);
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public List<Datapoint> get(@PathParam("version") String version,
      @HeaderParam("X-Auth-Token") String authToken, @HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("namespace") String namespace, @QueryParam("start_time") String startTimeStr,
      @QueryParam("end_time") String endTimeStr, @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("statistics") String statisticsStr,
      @DefaultValue("300") @QueryParam("period") String periodStr) {
    if ("v1.0".equals(version))
      throw new WebApplicationException(FaultType.NOT_FOUND.statusCode);

    // Validate parameters
    NamespaceValidation.validateSimple(namespace);
    DateTime startTime = Validation.parseAndValidateDate(startTimeStr, "start_time", true);
    DateTime endTime = Validation.parseAndValidateDate(endTimeStr, "end_time", false);
    if (!startTime.isBefore(endTime))
      throw Exceptions.badRequest("start_time must be before end_time");
    Validation.validateNotNullOrEmpty(dimensionsStr, "dimensions");
    Validation.validateNotNullOrEmpty(statisticsStr, "statistics");
    int period = Validation.parseAndValidateNumber(periodStr, "period");
    List<String> statistics = Validation.parseValidateAndNormalizeStatistics(COMMA_SPLITTER.split(statisticsStr));

    // Parse dimensions
    Map<String, String> dimensions = new HashMap<String, String>();
    for (String dimensionStr : COMMA_SPLITTER.split(dimensionsStr)) {
      String[] dimensionArr = Iterables.toArray(COLON_SPLITTER.split(dimensionStr), String.class);
      if (dimensionArr.length == 2)
        dimensions.put(dimensionArr[0], dimensionArr[1]);
    }

    // Require properly formed dimensions for cloud services, for now
    DimensionValidation.validate(namespace, dimensions, config.cloudServices.get(namespace));

    // Verify ownership
    Validation.verifyOwnership(tenantId, namespace, dimensions,
        config.cloudServices.get(namespace), authToken);

    return datapointRepo.find(authToken, namespace, startTime, endTime, dimensions, statistics,
        period);
  }

  private Metric validateAndConvert(String version, CreateMetricCommand newMetric,
      String tenantId, String crossTenantId) {
    if ("v1.1".equals(version) && !Strings.isNullOrEmpty(newMetric.type))
      throw Exceptions.unprocessableEntity(
          "The type field is not available for the 1.1 API (project: %s)", tenantId);

    if ("v1.0".equals(version) && !Strings.isNullOrEmpty(newMetric.type)) {
      if (newMetric.dimensions == null)
        newMetric.dimensions = new HashMap<String, String>();
      newMetric.dimensions.put("metric_name", newMetric.type);
    }

    newMetric.normalizeTimestamp();
    validate(newMetric, tenantId, crossTenantId);
    return newMetric.toFlatMetric();
  }

  private void validate(CreateMetricCommand newMetric, String tenantId, String crossTenantId) {
    // Normalize
    newMetric.namespace = NamespaceValidation.normalize(newMetric.namespace);

    // Validate access
    if (Namespaces.isReserved(newMetric.namespace)) {
      NamespaceValidation.validateSimple(newMetric.namespace);
    } else {
      if (!Strings.isNullOrEmpty(crossTenantId))
        throw Exceptions.forbidden("Project %s cannot POST cross tenant metrics", tenantId);
      NamespaceValidation.validate(newMetric.namespace);
    }

    // Validate dimensions and metric
    DimensionValidation.validate(newMetric.namespace, newMetric.dimensions,
        config.cloudServices.get(newMetric.namespace));
    MetricValidation.validate(newMetric);
  }
}
