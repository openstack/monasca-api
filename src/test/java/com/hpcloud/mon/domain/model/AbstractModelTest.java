package com.hpcloud.mon.domain.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.hpcloud.mon.resource.serialization.SubAlarmExpressionSerializer;

/**
 * Base model test.
 */
public abstract class AbstractModelTest {
  public static final ObjectMapper MAPPER;

  static {
    MAPPER = new ObjectMapper();
    MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
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
