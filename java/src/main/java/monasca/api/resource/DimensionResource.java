/*
 * Copyright (c) 2016 Hewlett-Packard Development Company, L.P.
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
import monasca.api.domain.model.dimension.DimensionRepo;
import monasca.api.domain.model.dimension.DimensionValues;
import monasca.api.infrastructure.persistence.PersistUtils;

/**
 * Dimension resource implementation.
 */
@Path("/v2.0/metrics/dimensions/names/values")
public class DimensionResource {

  private final DimensionRepo repo;
  private final PersistUtils persistUtils;
  private final String admin_role;

  @Inject
  public DimensionResource(ApiConfig config, DimensionRepo repo, PersistUtils persistUtils) {
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
      @QueryParam("limit") String limit,
      @QueryParam("dimension_name") String dimensionName,
      @QueryParam("metric_name") String metricName,
      @QueryParam("offset") String offset,
      @QueryParam("tenant_id") String crossTenantId) throws Exception
  {
    Validation.validateNotNullOrEmpty(dimensionName, "dimension_name");
    final int paging_limit = this.persistUtils.getLimit(limit);
    String queryTenantId = Validation.getQueryProject(roles, crossTenantId, tenantId, admin_role);
    DimensionValues dimVals = repo.find(metricName, queryTenantId, dimensionName, offset, paging_limit);
    return Links.paginateDimensionValues(dimVals, paging_limit, uriInfo);
  }
}
