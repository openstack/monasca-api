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

package monasca.api.resource;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import monasca.common.dropwizard.AbstractResourceTest;
import monasca.api.resource.exception.ConstraintViolationExceptionMapper;
import monasca.api.resource.exception.EntityExistsExceptionMapper;
import monasca.api.resource.exception.EntityNotFoundExceptionMapper;
import monasca.api.resource.exception.IllegalArgumentExceptionMapper;
import monasca.api.resource.exception.InvalidEntityExceptionMapper;
import monasca.api.resource.exception.JsonMappingExceptionManager;
import monasca.api.resource.exception.JsonProcessingExceptionMapper;
import monasca.api.resource.exception.ThrowableExceptionMapper;

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
