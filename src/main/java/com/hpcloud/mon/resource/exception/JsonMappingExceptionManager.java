package com.hpcloud.mon.resource.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.hpcloud.mon.resource.exception.Exceptions.FaultType;

/**
 * Adapted from Dropwizard's JsonMappingExceptionManager.
 * 
 * @author Todd Walk
 */
@Provider
public class JsonMappingExceptionManager implements ExceptionMapper<JsonMappingException> {
  @Override
  public Response toResponse(JsonMappingException exception) {
    return Response.status(FaultType.BAD_REQUEST.statusCode)
        .type(MediaType.APPLICATION_JSON)
        .entity(
            Exceptions.buildLoggedErrorMessage(FaultType.BAD_REQUEST,
                "Unable to process the provided JSON",
                Exceptions.stripLocationFromStacktrace(exception.getMessage()), null))
        .build();
  }
}
