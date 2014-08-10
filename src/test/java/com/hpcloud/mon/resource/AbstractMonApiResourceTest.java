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
import com.hpcloud.mon.resource.exception.ThrowableExceptionMapper;

/**
 * Support class for monitoring resource tests.
 */
public abstract class AbstractMonApiResourceTest extends AbstractResourceTest {
  @Override
  protected void setupResources() throws Exception {
    addSingletons(new EntityExistsExceptionMapper(), new EntityNotFoundExceptionMapper(),
        new IllegalArgumentExceptionMapper(), new InvalidEntityExceptionMapper(),
        new JsonProcessingExceptionMapper(), new JsonMappingExceptionManager(),
        new ConstraintViolationExceptionMapper(), new ThrowableExceptionMapper<Throwable>() {});

    objectMapper
        .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
  }
}
