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
package monasca.api.resource.serialization;

import java.io.IOException;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import monasca.common.model.alarm.AlarmSubExpression;

public class SubAlarmExpressionSerializer extends JsonSerializer<AlarmSubExpression> {
  @Override
  public void serialize(AlarmSubExpression value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException, JsonProcessingException {
    jgen.writeStartObject();
    jgen.writeStringField("function", value.getFunction().name());
    jgen.writeStringField("metric_name", value.getMetricDefinition().name);
    jgen.writeObjectField(
        "dimensions",
        value.getMetricDefinition().dimensions == null ? Collections.emptyMap() : value
            .getMetricDefinition().dimensions);
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
