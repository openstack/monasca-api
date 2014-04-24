package com.hpcloud.mon.app.validation;

import java.util.List;

import javax.ws.rs.WebApplicationException;

import com.hpcloud.mon.common.model.Services;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * Utilities for validating AlarmExpressions.
 * 
 * @author Jonathan Halterman
 */
public final class AlarmValidation {
  private AlarmValidation() {
  }

  /**
   * @throws WebApplicationException if validation fails
   */
  public static void validate(String name, String description, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    if (name != null && name.length() > 255)
      throw Exceptions.unprocessableEntity("Name %s must be 255 characters or less", name);
    if (description != null && description.length() > 255)
      throw Exceptions.unprocessableEntity("Description %s must be 255 characters or less",
          description);
    if (alarmActions != null)
      for (String action : alarmActions)
        if (action.length() > 50)
          throw Exceptions.unprocessableEntity("Alarm action %s must be 50 characters or less",
              action);
    if (okActions != null)
      for (String action : okActions)
        if (action.length() > 50)
          throw Exceptions.unprocessableEntity("Ok action %s must be 50 characters or less", action);
    if (undeterminedActions != null)
      for (String action : undeterminedActions)
        if (action.length() > 50)
          throw Exceptions.unprocessableEntity(
              "Undetermined action %s must be 50 characters or less", action);
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
      String service = metricDef.dimensions == null ? null
          : metricDef.dimensions.get(Services.SERVICE_DIMENSION);

      // Normalize and validate namespace
      metricDef.name = MetricNameValidation.normalize(metricDef.name);
      MetricNameValidation.validate(metricDef.name, service);

      // Normalize and validate dimensions
      if (metricDef.dimensions != null) {
        metricDef.setDimensions(DimensionValidation.normalize(metricDef.dimensions));
        DimensionValidation.validate(metricDef.dimensions, service);
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
}
