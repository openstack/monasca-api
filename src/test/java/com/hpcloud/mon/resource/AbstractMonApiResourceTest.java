package com.hpcloud.mon.resource;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.hpcloud.dropwizard.AbstractResourceTest;
import com.hpcloud.mon.resource.exception.ConstraintViolationExceptionMapper;
import com.hpcloud.mon.resource.exception.EntityExistsExceptionMapper;
import com.hpcloud.mon.resource.exception.EntityNotFoundExceptionMapper;
import com.hpcloud.mon.resource.exception.IllegalArgumentExceptionMapper;
import com.hpcloud.mon.resource.exception.InvalidEntityExceptionMapper;
import com.hpcloud.mon.resource.exception.JsonMappingExceptionManager;
import com.hpcloud.mon.resource.exception.JsonProcessingExceptionMapper;
import com.hpcloud.mon.resource.exception.ResourceNotFoundExceptionMapper;
import com.hpcloud.mon.resource.exception.ThrowableExceptionMapper;

/**
 * Support class for monitoring resource tests.
 * 
 * @author Jonathan Halterman
 */
public abstract class AbstractMonApiResourceTest extends AbstractResourceTest {
  @Override
  protected void setupResources() throws Exception {
    addSingletons(new EntityExistsExceptionMapper(), new EntityNotFoundExceptionMapper(),
        new ResourceNotFoundExceptionMapper(), new IllegalArgumentExceptionMapper(),
        new InvalidEntityExceptionMapper(), new JsonProcessingExceptionMapper(),
        new JsonMappingExceptionManager(), new ConstraintViolationExceptionMapper(),
        new ThrowableExceptionMapper<Throwable>() {
        });

    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
  }
}
