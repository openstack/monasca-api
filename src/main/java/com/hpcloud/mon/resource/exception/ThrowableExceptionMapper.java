package com.hpcloud.mon.resource.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.hpcloud.mon.resource.exception.Exceptions.FaultType;

/**
 * Adapted from Dropwizard's LoggingExceptionMapper.
 * 
 * @author Jonathan Halterman
 * @param <E> Exception type
 */
@Provider
public class ThrowableExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {
  @Override
  public Response toResponse(E exception) {
    if (exception instanceof WebApplicationException)
      return ((WebApplicationException) exception).getResponse();

    return Response.status(Status.INTERNAL_SERVER_ERROR)
        .type(MediaType.APPLICATION_JSON)
        .entity(
            Exceptions.buildLoggedErrorMessage(FaultType.SERVER_ERROR,
                "An internal server error occurred", null, exception))
        .build();
  }
}
