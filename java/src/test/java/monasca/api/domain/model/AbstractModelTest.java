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

package monasca.api.domain.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import monasca.api.resource.serialization.SubAlarmExpressionSerializer;

/**
 * Base model test.
 */
public abstract class AbstractModelTest {
  public static final ObjectMapper MAPPER;

  static {
    MAPPER = new ObjectMapper();
    MAPPER
        .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    SimpleModule module = new SimpleModule("SerializationModule");
    module.addSerializer(new SubAlarmExpressionSerializer());
    MAPPER.registerModule(module);
    MAPPER.registerModule(new JodaModule());
  }

  protected String toJson(Object object) throws IOException {
    return MAPPER.writeValueAsString(object);
  }

  protected <T> T fromJson(String json, Class<T> type) throws Exception {
    return MAPPER.readValue(json, type);
  }
}
