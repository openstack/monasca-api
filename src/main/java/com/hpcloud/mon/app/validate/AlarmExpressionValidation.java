package com.hpcloud.mon.app.validate;

import javax.ws.rs.WebApplicationException;

import com.google.common.base.Strings;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * Utilities for working with AlarmExpressions.
 * 
 * @author Jonathan Halterman
 */
public final class AlarmExpressionValidation {
  private AlarmExpressionValidation() {
  }

  /**
   * Validates, normalizes and gets an AlarmExpression for the {@code expression}.
   * 
   * @throws WebApplicationException if validation fails
   */
  public static AlarmExpression validateNormalizeAndGet(String expression,
      MonApiConfiguration maasConfig) {
    AlarmExpression alarmExpression = null;

    try {
      alarmExpression = AlarmExpression.of(expression);
    } catch (IllegalArgumentException e) {
      throw Exceptions.unprocessableEntityDetails("The alarm expression is invalid",
          e.getMessage(), e);
    }

    for (AlarmSubExpression subExpression : alarmExpression.getSubExpressions()) {
      MetricDefinition metricDef = subExpression.getMetricDefinition();

      metricDef.namespace = NamespaceValidation.normalize(metricDef.namespace);
      NamespaceValidation.validateSimple(metricDef.namespace);

      metricDef.setDimensions(DimensionValidation.normalize(metricDef.dimensions));
      DimensionValidation.validate(metricDef.namespace, metricDef.dimensions,
          maasConfig.cloudServices.get(metricDef.namespace));

      if (Namespaces.isReserved(metricDef.namespace)) {
        String type = metricDef.dimensions.get("metric_name");
        if (!Strings.isNullOrEmpty(type)
            && !Namespaces.isValidMetricname(metricDef.namespace, type)) {
          throw Exceptions.unprocessableEntity("%s is not a valid metric name for namespace %s",
              type, metricDef.namespace);
        }
      }

      if (subExpression.getPeriod() == 0)
        throw Exceptions.unprocessableEntity("Period must not be 0");
      if (subExpression.getPeriod() % 60 != 0)
        throw Exceptions.unprocessableEntity("Period %s must be a multiple of 60",
            subExpression.getPeriod());
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
