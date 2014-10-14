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
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import monasca.api.app.validation.Validation;
import monasca.api.domain.model.statistic.StatisticRepository;
import monasca.api.domain.model.statistic.Statistics;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

// import monasca.common.util.stats.Statistics;

/**
 * Statistics resource implementation.
 */
@Path("/v2.0/metrics/statistics")
@Api(value = "/v2.0/statistics", description = "Operations for accessing statistics")
public class StatisticResource {
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

  private final StatisticRepository repo;

  @Inject
  public StatisticResource(StatisticRepository repo) {
    this.repo = repo;
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get statistics", response = Statistics.class, responseContainer = "List")
  public List<Statistics> get(@HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("name") String name, @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("start_time") String startTimeStr, @QueryParam("end_time") String endTimeStr,
      @QueryParam("statistics") String statisticsStr,
      @DefaultValue("300") @QueryParam("period") String periodStr) throws Exception {

    // Validate query parameters
    DateTime startTime = Validation.parseAndValidateDate(startTimeStr, "start_time", true);
    DateTime endTime = Validation.parseAndValidateDate(endTimeStr, "end_time", false);
    Validation.validateTimes(startTime, endTime);
    Validation.validateNotNullOrEmpty(statisticsStr, "statistics");
    int period = Validation.parseAndValidateNumber(periodStr, "period");
    List<String> statistics =
        Validation.parseValidateAndNormalizeStatistics(COMMA_SPLITTER.split(statisticsStr));
    Map<String, String> dimensions =
        Strings.isNullOrEmpty(dimensionsStr) ? null : Validation.parseAndValidateNameAndDimensions(
            name, dimensionsStr);

    return repo.find(tenantId, name, dimensions, startTime, endTime, statistics, period);
  }
}
