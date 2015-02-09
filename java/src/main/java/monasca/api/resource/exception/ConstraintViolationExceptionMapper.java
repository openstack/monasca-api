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
package monasca.api.resource.exception;

import io.dropwizard.jersey.validation.ValidationErrorMessage;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import monasca.api.resource.exception.Exceptions.FaultType;

@Provider
public class ConstraintViolationExceptionMapper implements
    ExceptionMapper<ConstraintViolationException> {
  private static final int UNPROCESSABLE_ENTITY = 422;

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    final ValidationErrorMessage message =
        new ValidationErrorMessage(exception.getConstraintViolations());
    String msg =
        message.getErrors().isEmpty() ? exception.getMessage() : message.getErrors().toString();
    return Response.status(UNPROCESSABLE_ENTITY).type(MediaType.APPLICATION_JSON)
        .entity(Exceptions.buildLoggedErrorMessage(FaultType.UNPROCESSABLE_ENTITY, msg)).build();
  }
}
