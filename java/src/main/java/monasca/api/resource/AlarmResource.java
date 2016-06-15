/*
 * Copyright (c) 2014-2016 Hewlett Packard Enterprise Development Company, L.P.
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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import monasca.api.app.AlarmService;
import monasca.api.app.command.UpdateAlarmCommand;
import monasca.api.app.validation.MetricNameValidation;
import monasca.api.app.validation.Validation;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmCount;
import monasca.api.domain.model.alarm.AlarmRepo;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistory;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistoryRepo;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.api.resource.annotation.PATCH;
import monasca.api.resource.exception.Exceptions;
import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmState;

/**
 * Alarm resource implementation.
 */
@Path("/v2.0/alarms")
public class AlarmResource {
  private final AlarmService service;
  private final AlarmRepo repo;
  private final PersistUtils persistUtils;
  private final AlarmStateHistoryRepo stateHistoryRepo;

  private final static List<String> ALLOWED_GROUP_BY = Arrays.asList("alarm_definition_id", "name",
                                                                     "state", "severity", "link",
                                                                     "lifecycle_state",
                                                                     "metric_name", "dimension_name",
                                                                     "dimension_value");
  private final static List<String> ALLOWED_SORT_BY = Arrays.asList("alarm_id", "alarm_definition_id",
                                                                    "alarm_definition_name", "state",
                                                                    "severity", "lifecycle_state", "link",
                                                                    "state_updated_timestamp", "updated_timestamp",
                                                                    "created_timestamp");

  @Inject
  public AlarmResource(AlarmService service, AlarmRepo repo,
      AlarmStateHistoryRepo stateHistoryRepo,
      PersistUtils persistUtils) {
    this.service = service;
    this.repo = repo;
    this.stateHistoryRepo = stateHistoryRepo;
    this.persistUtils = persistUtils;
  }

  @DELETE
  @Timed
  @Path("/{alarm_id}")
  public void delete(@HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId) {
    service.delete(tenantId, alarmId);
  }

  @GET
  @Timed
  @Path("/{alarm_id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Alarm get(
      @Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId, @PathParam("alarm_id") String alarm_id) {
    return fixAlarmLinks(uriInfo, repo.findById(tenantId, alarm_id));
  }

  private Alarm fixAlarmLinks(UriInfo uriInfo, Alarm alarm) {
    Links.hydrate(alarm.getAlarmDefinition(), uriInfo,
                  AlarmDefinitionResource.ALARM_DEFINITIONS_PATH);
    return Links.hydrate(alarm, uriInfo, true);
  }

  @GET
  @Timed
  @Path("/{alarm_id}/state-history")
  @Produces(MediaType.APPLICATION_JSON)
  public Object getStateHistory(@Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId, @PathParam("alarm_id") String alarmId,
      @QueryParam("offset") String offset,
      @QueryParam("limit") String limit)
      throws Exception {
    final int paging_limit = this.persistUtils.getLimit(limit);
    final List<AlarmStateHistory> resource = stateHistoryRepo.findById(tenantId,
                                                                       alarmId,
                                                                       offset,
                                                                       paging_limit
    );
    return Links.paginate(paging_limit, resource, uriInfo);
  }

  @GET
  @Timed
  @Path("/state-history")
  @Produces(MediaType.APPLICATION_JSON)
  public Object listStateHistory(
      @Context UriInfo uriInfo,
      @HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("dimensions") String dimensionsStr,
      @QueryParam("start_time") String startTimeStr,
      @QueryParam("end_time") String endTimeStr,
      @QueryParam("offset") String offset,
      @QueryParam("limit") String limit)
      throws Exception {

    // Validate query parameters
    DateTime startTime = Validation.parseAndValidateDate(startTimeStr, "start_time", false);

    DateTime endTime = Validation.parseAndValidateDate(endTimeStr, "end_time", false);

    Validation.parseAndValidateDate(offset, "offset", false);

    if (startTime != null) {
      Validation.validateTimes(startTime, endTime);
    }

    Map<String, String> dimensions =
        Strings.isNullOrEmpty(dimensionsStr) ? null : Validation
            .parseAndValidateDimensions(dimensionsStr);

    final int paging_limit = this.persistUtils.getLimit(limit);
    final List<AlarmStateHistory> resources = stateHistoryRepo.find(tenantId,
                                                                    dimensions,
                                                                    startTime,
                                                                    endTime,
                                                                    offset,
                                                                    paging_limit
    );
    return Links.paginate(paging_limit, resources, uriInfo);
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public Object list(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @QueryParam("alarm_definition_id") String alarmDefId,
      @QueryParam("metric_name") String metricName,
      @QueryParam("metric_dimensions") String metricDimensionsStr,
      @QueryParam("state") AlarmState state,
      @QueryParam("severity") String severity,
      @QueryParam("lifecycle_state") String lifecycleState,
      @QueryParam("link") String link,
      @QueryParam("state_updated_start_time") String stateUpdatedStartStr,
      @QueryParam("sort_by") String sortBy,
      @QueryParam("offset") String offset,
      @QueryParam("limit") String limit)
      throws Exception {

    Map<String, String> metricDimensions =
        Strings.isNullOrEmpty(metricDimensionsStr) ? null : Validation
            .parseAndValidateDimensions(metricDimensionsStr);
    MetricNameValidation.validate(metricName, false);
    DateTime stateUpdatedStart =
        Validation.parseAndValidateDate(stateUpdatedStartStr,
                                        "state_updated_start_time", false);

    List<String> sortByList = Validation.parseAndValidateSortBy(sortBy, ALLOWED_SORT_BY);
    if (!Strings.isNullOrEmpty(offset)) {
      Validation.parseAndValidateNumber(offset, "offset");
    }
    List<AlarmSeverity> severityList = Validation.parseAndValidateSeverity(severity);

    final int paging_limit = this.persistUtils.getLimit(limit);
    final List<Alarm> alarms = repo.find(tenantId, alarmDefId, metricName, metricDimensions, state,
                                         severityList, lifecycleState, link, stateUpdatedStart, sortByList,
                                         offset, paging_limit, true);
    for (final Alarm alarm : alarms) {
      Links.hydrate(
          alarm.getAlarmDefinition(),
          uriInfo,
          AlarmDefinitionResource.ALARM_DEFINITIONS_PATH
      );
    }
    return Links.paginateAlarming(paging_limit, Links.hydrate(alarms, uriInfo), uriInfo);
  }



  @PATCH
  @Timed
  @Path("/{alarm_id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Alarm patch(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId, @NotEmpty Map<String, String> fields)
      throws JsonMappingException {
    String stateStr = fields.get("state");
    String lifecycleState = fields.get("lifecycle_state");
    String link = fields.get("link");
    AlarmState state =
        stateStr == null ? null : Validation.parseAndValidate(AlarmState.class, stateStr);
    Validation.validateLifecycleState(lifecycleState);
    Validation.validateLink(link);

    return fixAlarmLinks(uriInfo, service.patch(tenantId, alarmId, state, lifecycleState, link));
  }

  @PUT
  @Timed
  @Path("/{alarm_id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Alarm update(@Context UriInfo uriInfo, @HeaderParam("X-Tenant-Id") String tenantId,
      @PathParam("alarm_id") String alarmId, @Valid UpdateAlarmCommand command) {

    Validation.validateLifecycleState(command.lifecycleState);
    Validation.validateLink(command.link);

    return fixAlarmLinks(uriInfo, service.update(tenantId, alarmId, command));
  }

  @GET
  @Timed
  @Path("/count")
  @Produces(MediaType.APPLICATION_JSON)
  public Object getCount(@Context UriInfo uriInfo,
                         @HeaderParam("X-Tenant-Id") String tenantId, @PathParam("alarm_id") String alarmId,
                         @QueryParam("alarm_definition_id") String alarmDefId,
                         @QueryParam("metric_name") String metricName,
                         @QueryParam("metric_dimensions") String metricDimensionsStr,
                         @QueryParam("state") AlarmState state,
                         @QueryParam("severity") String severity,
                         @QueryParam("lifecycle_state") String lifecycleState,
                         @QueryParam("link") String link,
                         @QueryParam("state_updated_start_time") String stateUpdatedStartStr,
                         @QueryParam("group_by") String groupByStr,
                         @QueryParam("offset") String offset,
                         @QueryParam("limit") String limit)
      throws Exception {
    Map<String, String> metricDimensions =
        Strings.isNullOrEmpty(metricDimensionsStr) ? null : Validation
            .parseAndValidateDimensions(metricDimensionsStr);
    MetricNameValidation.validate(metricName, false);
    DateTime stateUpdatedStart =
        Validation.parseAndValidateDate(stateUpdatedStartStr,
                                        "state_updated_start_time", false);
    List<AlarmSeverity> severityList = Validation.parseAndValidateSeverity(severity);
    List<String> groupBy = (Strings.isNullOrEmpty(groupByStr)) ? null : parseAndValidateGroupBy(
        groupByStr);

    if (offset != null) {
      Validation.parseAndValidateNumber(offset, "offset");
    }

    final int paging_limit = this.persistUtils.getLimit(limit);
    final AlarmCount resource = repo.getAlarmsCount(tenantId,
                                                    alarmDefId,
                                                    metricName,
                                                    metricDimensions,
                                                    state,
                                                    severityList,
                                                    lifecycleState,
                                                    link,
                                                    stateUpdatedStart,
                                                    groupBy,
                                                    offset,
                                                    paging_limit);
    Links.paginateAlarmCount(resource, paging_limit, uriInfo);
    return resource;
  }

  private List<String> parseAndValidateGroupBy(String groupByStr) {
    List<String> groupBy = null;
    if (!Strings.isNullOrEmpty(groupByStr)) {
      groupBy = Lists.newArrayList(Splitter.on(',').omitEmptyStrings().trimResults().split(groupByStr));
      if (!ALLOWED_GROUP_BY.containsAll(groupBy)) {
        throw Exceptions.unprocessableEntity("Unprocessable Entity", "Invalid group_by field");
      }
    }
    return groupBy;
  }
}
