/*
 * Copyright (c) 2014, 2016 Hewlett-Packard Development Company, L.P.
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

import com.google.common.base.Strings;

import com.codahale.metrics.annotation.Timed;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import monasca.api.app.validation.MetricNameValidation;
import monasca.api.ApiConfig;
import monasca.api.app.validation.Validation;
import monasca.api.domain.model.measurement.MeasurementRepo;
import monasca.api.domain.model.measurement.Measurements;
import monasca.api.infrastructure.persistence.PersistUtils;

/**
 * Measurement resource implementation.
 */
@Path("/v2.0/metrics/measurements")
public class MeasurementResource {

  private final MeasurementRepo repo;
  private final PersistUtils persistUtils;
  private final String admin_role;

  @Inject
  public MeasurementResource(ApiConfig config, MeasurementRepo repo, PersistUtils persistUtils) {
    this.admin_role = (config.middleware == null || config.middleware.adminRole == null)
                      ? DEFAULT_ADMIN_ROLE : config.middleware.adminRole;
    this.repo = repo;
    this.persistUtils = persistUtils;
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public Object get(
      @Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId,
      @HeaderParam("X-Roles") String roles,
      @QueryParam("name") String name,
      @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("start_time") String startTimeStr,
      @QueryParam("end_time") String endTimeStr,
      @QueryParam("offset") String offset,
      @QueryParam("limit") String limit,
      @QueryParam("tenant_id") String crossTenantId,
      @QueryParam("merge_metrics") String mergeMetricsFlag,
      @QueryParam("group_by") String groupBy) throws Exception {

    // Validate query parameters
    DateTime startTime = Validation.parseAndValidateDate(startTimeStr, "start_time", true);
    DateTime endTime = Validation.parseAndValidateDate(endTimeStr, "end_time", false);
    Validation.validateTimes(startTime, endTime);
    Map<String, String>
        dimensions =
          Strings.isNullOrEmpty(dimensionsStr) ? null : Validation
              .parseAndValidateDimensions(dimensionsStr);
    MetricNameValidation.validate(name, true);
    Boolean mergeMetricsFlagBool = Validation.validateAndParseMergeMetricsFlag(mergeMetricsFlag);
    Validation.validateMetricsGroupBy(groupBy);

    String queryTenantId = Validation.getQueryProject(roles, crossTenantId, tenantId, admin_role);

    final int paging_limit = this.persistUtils.getLimit(limit);
    final List<Measurements> resources = repo.find(queryTenantId,
        name,
        dimensions,
        startTime,
        endTime,
        offset,
        paging_limit,
        mergeMetricsFlagBool,
        groupBy
    );
    return Links.paginateMeasurements(paging_limit, resources, uriInfo);
  }

}
