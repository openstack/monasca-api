package com.hpcloud.mon.resource.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.hpcloud.mon.resource.exception.Exceptions.FaultType;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
  @Override
  public Response toResponse(IllegalArgumentException e) {
    return Response.status(Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(Exceptions.buildLoggedErrorMessage(FaultType.BAD_REQUEST, e.getMessage()))
        .build();
  }
}