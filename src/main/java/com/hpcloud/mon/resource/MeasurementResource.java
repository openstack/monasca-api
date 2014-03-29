package com.hpcloud.mon.resource;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.joda.time.DateTime;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import com.hpcloud.mon.app.validation.Validation;
import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;
import com.hpcloud.mon.domain.model.measurement.Measurements;

/**
 * Measurement resource implementation.
 * 
 * @author Jonathan Halterman
 */
@Path("/v2.0/metrics/measurements")
public class MeasurementResource {
  private final MeasurementRepository repo;

  @Inject
  public MeasurementResource(MeasurementRepository repo) {
    this.repo = repo;
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<Measurements> get(@HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("name") String name, @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("start_time") String startTimeStr, @QueryParam("end_time") String endTimeStr) {

    // Validate query parameters
    DateTime startTime = Validation.parseAndValidateDate(startTimeStr, "start_time", true);
    DateTime endTime = Validation.parseAndValidateDate(endTimeStr, "end_time", false);
    Validation.validateTimes(startTime, endTime);
    Map<String, String> dimensions = Strings.isNullOrEmpty(dimensionsStr) ? null
        : Validation.parseAndValidateNameAndDimensions(name, dimensionsStr);

    return repo.find(tenantId, name, dimensions, startTime, endTime);
  }
}
