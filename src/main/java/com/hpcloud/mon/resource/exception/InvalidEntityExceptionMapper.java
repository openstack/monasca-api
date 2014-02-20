package com.hpcloud.mon.resource.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.hpcloud.mon.domain.exception.InvalidEntityException;
import com.hpcloud.mon.resource.exception.Exceptions.FaultType;

@Provider
public class InvalidEntityExceptionMapper implements ExceptionMapper<InvalidEntityException> {
  @Override
  public Response toResponse(InvalidEntityException e) {
    return Response.status(FaultType.BAD_REQUEST.statusCode)
        .type(MediaType.APPLICATION_JSON)
        .entity(Exceptions.buildLoggedErrorMessage(FaultType.BAD_REQUEST, e.getMessage()))
        .build();
  }
}