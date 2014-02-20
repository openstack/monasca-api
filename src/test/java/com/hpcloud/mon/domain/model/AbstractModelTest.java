package com.hpcloud.mon.domain.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

/**
 * Base model test.
 * 
 * @author Jonathan Halterman
 */
public abstract class AbstractModelTest {
  private final static ObjectMapper MAPPER;

  static {
    MAPPER = new ObjectMapper();
    MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
  }

  protected String toJson(Object object) throws IOException {
    return MAPPER.writeValueAsString(object);
  }

  protected <T> T fromJson(String json, Class<T> type) throws Exception {
    return MAPPER.readValue(json, type);
  }
}
