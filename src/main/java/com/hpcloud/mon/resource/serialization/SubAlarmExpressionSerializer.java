package com.hpcloud.mon.resource.serialization;

import java.io.IOException;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;

public class SubAlarmExpressionSerializer extends JsonSerializer<AlarmSubExpression> {
  @Override
  public void serialize(AlarmSubExpression value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException, JsonProcessingException {
    jgen.writeStartObject();
    jgen.writeStringField("function", value.getFunction().name());
    jgen.writeStringField("metric_name", value.getMetricDefinition().name);
    jgen.writeObjectField(
        "dimensions",
        value.getMetricDefinition().dimensions == null ? Collections.emptyMap()
            : value.getMetricDefinition().dimensions);
    jgen.writeStringField("operator", value.getOperator().name());
    jgen.writeNumberField("threshold", value.getThreshold());
    jgen.writeNumberField("period", value.getPeriod());
    jgen.writeNumberField("periods", value.getPeriods());
    jgen.writeEndObject();
  }

  @Override
  public Class<AlarmSubExpression> handledType() {
    return AlarmSubExpression.class;
  }
}