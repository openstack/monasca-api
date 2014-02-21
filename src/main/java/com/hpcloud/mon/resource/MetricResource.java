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
import javax.ws.rs.PathParam;
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
import com.hpcloud.mon.app.validation.NamespaceValidation;
import com.hpcloud.mon.app.validation.Validation;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.common.model.metric.Metric;
import com.hpcloud.mon.domain.model.metric.Datapoint;
import com.hpcloud.mon.domain.model.metric.DatapointRepository;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * Metric resource implementation.
 * 
 * @author Todd Walk
 * @author Jonathan Halterman
 */
@Path("/{version: v1.[2]}/metrics")
public class MetricResource {
  private static final String MONITORING_DELEGATE_ROLE = "monitoring-delegate";
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();
  private static final Splitter COLON_SPLITTER = Splitter.on(':').omitEmptyStrings().trimResults();

  private final MetricService service;
  private final DatapointRepository datapointRepo;

  @Inject
  public MetricResource(MetricService service, DatapointRepository datapointRepo) {
    this.service = service;
    this.datapointRepo = datapointRepo;
  }

  @POST
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  public void create(@Context UriInfo uriInfo, @HeaderParam("X-Auth-Token") String authToken,
      @HeaderParam("X-Tenant-Id") String tenantId, @HeaderParam("X-Roles") String roles,
      @QueryParam("tenant_id") String crossTenantId, @Valid CreateMetricCommand[] commands) {
    boolean isDelegate = Arrays.asList(COMMA_SPLITTER.split(roles)).contains(
        MONITORING_DELEGATE_ROLE);
    List<Metric> metrics = new ArrayList<>(commands.length);
    for (CreateMetricCommand command : commands) {
      if (!isDelegate) {
        if (Namespaces.isReserved(command.namespace))
          throw Exceptions.forbidden("Project %s cannot POST metrics for the hpcs namespace",
              tenantId);
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
  public List<Datapoint> get(@PathParam("version") String version,
      @HeaderParam("X-Auth-Token") String authToken, @HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("namespace") String namespace, @QueryParam("start_time") String startTimeStr,
      @QueryParam("end_time") String endTimeStr, @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("statistics") String statisticsStr,
      @DefaultValue("300") @QueryParam("period") String periodStr) {

    // Validate parameters
    NamespaceValidation.validate(namespace);
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

    // Validate dimensions
    DimensionValidation.validate(namespace, dimensions);

    // Verify ownership
    Validation.verifyOwnership(tenantId, namespace, dimensions, authToken);

    // Return datapoints
    return datapointRepo.find(authToken, namespace, startTime, endTime, dimensions, statistics,
        period);
  }
}
