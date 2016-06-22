/*
 * (C) Copyright 2014-2016 Hewlett Packard Enterprise Development Company LP
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
package monasca.api.app.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import monasca.common.model.alarm.AlarmExpression;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import monasca.api.resource.exception.Exceptions;

/**
 * Utilities for validating AlarmExpressions.
 */
public final class AlarmValidation {

  private static final List<String> VALID_ALARM_SERVERITY = Arrays.asList("low", "medium", "high",
      "critical");

  private AlarmValidation() {}

  /**
   * @throws WebApplicationException if validation fails
   */
  public static void validate(String name, String description, String severity,
      List<String> alarmActions, List<String> okActions, List<String> undeterminedActions) {
    if (name != null && name.length() > 255)
      throw Exceptions.unprocessableEntity("Name %s must be 255 characters or less", name);
    if (description != null && description.length() > 255)
      throw Exceptions.unprocessableEntity("Description %s must be 255 characters or less",
          description);
    if (alarmActions != null) {
      for (String action : alarmActions)
        if (action.length() > 50)
          throw Exceptions.unprocessableEntity("Alarm action %s must be 50 characters or less",
              action);
      if (checkForDuplicateNotificationMethodsInAlarmDef(alarmActions)) {
        throw Exceptions
            .unprocessableEntity("Alarm definition cannot have Duplicate alarm notification methods");
      }
    }
    if (okActions != null) {
      for (String action : okActions)
        if (action.length() > 50)
          throw Exceptions
              .unprocessableEntity("Ok action %s must be 50 characters or less", action);
      if (checkForDuplicateNotificationMethodsInAlarmDef(okActions)) {
        throw Exceptions
            .unprocessableEntity("Alarm definition cannot have Duplicate OK notification methods");
      }
    }
    if (undeterminedActions != null) {
      for (String action : undeterminedActions)
        if (action.length() > 50)
          throw Exceptions.unprocessableEntity(
              "Undetermined action %s must be 50 characters or less", action);
      if (checkForDuplicateNotificationMethodsInAlarmDef(undeterminedActions)) {
        throw Exceptions
            .unprocessableEntity("Alarm definition cannot have Duplicate Undetermined notification methods");
      }
    }
    if (severity != null && !VALID_ALARM_SERVERITY.contains(severity.toLowerCase())) {
      throw Exceptions.unprocessableEntity("%s is not a valid severity", severity);
    }
  }

  /**
   * Validates, normalizes and gets an AlarmExpression for the {@code expression}.
   * 
   * @throws WebApplicationException if validation fails
   */
  public static AlarmExpression validateNormalizeAndGet(String expression) {
    AlarmExpression alarmExpression = null;

    try {
      alarmExpression = AlarmExpression.of(expression);
    } catch (IllegalArgumentException e) {
      throw Exceptions.unprocessableEntityDetails("The alarm expression is invalid",
          e.getMessage(), e);
    }

    for (AlarmSubExpression subExpression : alarmExpression.getSubExpressions()) {
      MetricDefinition metricDef = subExpression.getMetricDefinition();

      // Normalize and validate namespace
      metricDef.name = MetricNameValidation.normalize(metricDef.name);
      MetricNameValidation.validate(metricDef.name, true);

      // Normalize and validate dimensions
      if (metricDef.dimensions != null) {
        metricDef.setDimensions(DimensionValidation.normalize(metricDef.dimensions));
        DimensionValidation.validate(metricDef.dimensions);
      }

      // Validate period
      if (subExpression.getPeriod() == 0)
        throw Exceptions.unprocessableEntity("Period must not be 0");
      if (subExpression.getPeriod() % 60 != 0)
        throw Exceptions.unprocessableEntity("Period %s must be a multiple of 60",
            subExpression.getPeriod());

      // Validate periods
      if (subExpression.getPeriods() < 1)
        throw Exceptions.unprocessableEntity("Periods %s must be greater than or equal to 1",
            subExpression.getPeriods());
      if (subExpression.getPeriod() * subExpression.getPeriods() > 1209600)
        throw Exceptions.unprocessableEntity(
            "Period %s times %s must total less than 2 weeks in seconds (1209600)",
            subExpression.getPeriod(), subExpression.getPeriods());
    }

    return alarmExpression;
  }

  /**
   * Method checks for duplicate alarm actions
   */
  @SuppressWarnings("unchecked")
  private static boolean checkForDuplicateNotificationMethodsInAlarmDef(List<String> alarmActions) {
    @SuppressWarnings("rawtypes")
    Set inputSet = new HashSet(alarmActions);
    if (inputSet.size() < alarmActions.size()) {
      return true;
    }
    return false;
  }
}
