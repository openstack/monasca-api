package com.hpcloud.mon.resource.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.resource.exception.Exceptions.FaultType;

@Provider
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {
  @Override
  public Response toResponse(EntityNotFoundException e) {
    return Response.status(Status.NOT_FOUND)
        .type(MediaType.APPLICATION_JSON)
        .entity(Exceptions.buildLoggedErrorMessage(FaultType.NOT_FOUND, e.getMessage()))
        .build();
  }
}