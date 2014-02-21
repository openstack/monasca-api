package com.hpcloud.mon.resource;

import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;

import com.hpcloud.dropwizard.AbstractResourceTest;
import com.hpcloud.mon.resource.exception.ConstraintViolationExceptionMapper;
import com.hpcloud.mon.resource.exception.EntityExistsExceptionMapper;
import com.hpcloud.mon.resource.exception.EntityNotFoundExceptionMapper;
import com.hpcloud.mon.resource.exception.IllegalArgumentExceptionMapper;
import com.hpcloud.mon.resource.exception.InvalidEntityExceptionMapper;
import com.hpcloud.mon.resource.exception.JsonMappingExceptionManager;
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
  }
}
