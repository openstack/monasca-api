package com.hpcloud.mon.resource;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.joda.time.DateTime;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Splitter;
import com.hpcloud.mon.app.validation.Validation;
import com.hpcloud.mon.domain.model.statistic.StatisticRepository;
import com.hpcloud.util.stats.Statistic;

/**
 * Metric resource implementation.
 * 
 * @author Jonathan Halterman
 */
@Path("/v2.0/metrics/statistics")
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
  public List<Statistic> getStatistics(@HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("name") String name, @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("start_time") String startTimeStr, @QueryParam("end_time") String endTimeStr,
      @QueryParam("statistics") String statisticsStr,
      @DefaultValue("300") @QueryParam("period") String periodStr) {

    // Validate query parameters
    DateTime startTime = Validation.parseAndValidateDate(startTimeStr, "start_time", true);
    DateTime endTime = Validation.parseAndValidateDate(endTimeStr, "end_time", false);
    Validation.validateTimes(startTime, endTime);
    Validation.validateNotNullOrEmpty(statisticsStr, "statistics");
    int period = Validation.parseAndValidateNumber(periodStr, "period");
    List<String> statistics = Validation.parseValidateAndNormalizeStatistics(COMMA_SPLITTER.split(statisticsStr));
    Map<String, String> dimensions = Validation.parseAndValidateNameAndDimensions(name,
        dimensionsStr);

    return repo.find(name, dimensions, startTime, endTime, statistics, period);
  }
}
