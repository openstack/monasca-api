package com.hpcloud.mon.resource;

import java.util.ArrayList;
import java.util.Arrays;
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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.hpcloud.mon.app.MetricService;
import com.hpcloud.mon.app.command.CreateMetricCommand;
import com.hpcloud.mon.app.validation.DimensionValidation;
import com.hpcloud.mon.app.validation.MetricNameValidation;
import com.hpcloud.mon.app.validation.Validation;
import com.hpcloud.mon.common.model.Services;
import com.hpcloud.mon.common.model.metric.Metric;
import com.hpcloud.mon.domain.model.metric.Measurement;
import com.hpcloud.mon.domain.model.metric.MeasurementRepository;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * Metric resource implementation.
 * 
 * @author Todd Walk
 * @author Jonathan Halterman
 */
@Path("/v2.0/metrics")
public class MetricResource {
  private static final String MONITORING_DELEGATE_ROLE = "monitoring-delegate";
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();
  private static final Splitter COLON_SPLITTER = Splitter.on(':').omitEmptyStrings().trimResults();

  private final MetricService service;
  private final MeasurementRepository measurementRepo;

  @Inject
  public MetricResource(MetricService service, MeasurementRepository measurementRepo) {
    this.service = service;
    this.measurementRepo = measurementRepo;
  }

  @POST
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  public void create(@Context UriInfo uriInfo, @HeaderParam("X-Auth-Token") String authToken,
      @HeaderParam("X-Tenant-Id") String tenantId, @HeaderParam("X-Roles") String roles,
      @QueryParam("tenant_id") String crossTenantId, @Valid CreateMetricCommand[] commands) {
    boolean isDelegate = !Strings.isNullOrEmpty(roles)
        && Arrays.asList(COMMA_SPLITTER.split(roles)).contains(MONITORING_DELEGATE_ROLE);
    List<Metric> metrics = new ArrayList<>(commands.length);
    for (CreateMetricCommand command : commands) {
      if (!isDelegate) {
        if (Services.isReserved(command.name))
          throw Exceptions.forbidden("Project %s cannot POST metrics for the hpcs name", tenantId);
        if (!Strings.isNullOrEmpty(crossTenantId))
          throw Exceptions.forbidden("Project %s cannot POST cross tenant metrics", tenantId);
      }

      command.validate();
      metrics.add(command.toMetric());
    }

    service.create(metrics, tenantId, crossTenantId, authToken);
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public List<Metric> get(@HeaderParam("X-Tenant-Id") String tenantId) {

    return null;
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public List<Measurement> get(@HeaderParam("X-Auth-Token") String authToken,
      @HeaderParam("X-Tenant-Id") String tenantId, @QueryParam("name") String name,
      @QueryParam("start_time") String startTimeStr, @QueryParam("end_time") String endTimeStr,
      @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("statistics") String statisticsStr,
      @DefaultValue("300") @QueryParam("period") String periodStr) {

    // Validate query parameters
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

    // Validate metric definition
    String service = dimensions.get(Services.SERVICE_DIMENSION);
    MetricNameValidation.validate(name, service);
    DimensionValidation.validate(dimensions, service);

    // Verify ownership
    Validation.verifyOwnership(tenantId, name, dimensions, authToken);

    // Return measurements
    return measurementRepo.find(authToken, name, startTime, endTime, dimensions, statistics, period);
  }
}
