package com.hpcloud.mon.resource.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.hpcloud.http.rest.ResourceNotFoundException;
import com.hpcloud.mon.resource.exception.Exceptions.FaultType;

@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {
  @Override
  public Response toResponse(ResourceNotFoundException e) {
    return Response.status(Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(
            Exceptions.buildLoggedErrorMessage(FaultType.BAD_REQUEST,
                "The referenced %s resource id %s could not be found", e.service, e.resourceId))
        .build();
  }
}