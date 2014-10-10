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

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import monasca.api.app.validation.Validation;
import monasca.api.domain.model.measurement.MeasurementRepository;
import monasca.api.domain.model.measurement.Measurements;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Map;

/**
 * Measurement resource implementation.
 */
@Path("/v2.0/metrics/measurements")
@Api(value = "/v2.0/measurements", description = "Operations for accessing measurements")
public class MeasurementResource {
  private final MeasurementRepository repo;

  @Inject
  public MeasurementResource(MeasurementRepository repo) {
    this.repo = repo;
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get measurements", response = Measurements.class,
      responseContainer = "List")
  public Collection<Measurements> get(@HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("name") String name, @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("start_time") String startTimeStr, @QueryParam("end_time") String endTimeStr)
      throws Exception {

    // Validate query parameters
    DateTime startTime = Validation.parseAndValidateDate(startTimeStr, "start_time", true);
    DateTime endTime = Validation.parseAndValidateDate(endTimeStr, "end_time", false);
    Validation.validateTimes(startTime, endTime);
    Map<String, String> dimensions =
        Strings.isNullOrEmpty(dimensionsStr) ? null : Validation.parseAndValidateNameAndDimensions(
            name, dimensionsStr);

    return repo.find(tenantId, name, dimensions, startTime, endTime);
  }
}
