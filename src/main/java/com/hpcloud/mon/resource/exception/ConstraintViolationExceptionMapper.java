package com.hpcloud.mon.resource.exception;

import io.dropwizard.jersey.validation.ValidationErrorMessage;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.hpcloud.mon.resource.exception.Exceptions.FaultType;

@Provider
public class ConstraintViolationExceptionMapper implements
    ExceptionMapper<ConstraintViolationException> {
  private static final int UNPROCESSABLE_ENTITY = 422;

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    final ValidationErrorMessage message = new ValidationErrorMessage(
        exception.getConstraintViolations());
    String msg = message.getErrors().isEmpty() ? exception.getMessage() : message.getErrors()
        .toString();
    return Response.status(UNPROCESSABLE_ENTITY)
        .type(MediaType.APPLICATION_JSON)
        .entity(Exceptions.buildLoggedErrorMessage(FaultType.UNPROCESSABLE_ENTITY, msg))
        .build();
  }
}
