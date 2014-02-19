package com.hpcloud.mon.resource.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.hpcloud.mon.domain.exception.EntityExistsException;
import com.hpcloud.mon.resource.exception.Exceptions.FaultType;

@Provider
public class EntityExistsExceptionMapper implements ExceptionMapper<EntityExistsException> {
  @Override
  public Response toResponse(EntityExistsException e) {
    return Response.status(Status.CONFLICT)
        .type(MediaType.APPLICATION_JSON)
        .entity(Exceptions.buildLoggedErrorMessage(FaultType.CONFLICT, e.getMessage()))
        .build();
  }
}