package com.hpcloud.mon.resource.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hpcloud.mon.resource.exception.Exceptions.FaultType;

/**
 * Adapted from Dropwizard's JsonProcessingExceptionMapper.
 * 
 * @author Jonathan Halterman
 */
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {
  @Override
  public Response toResponse(JsonProcessingException exception) {
    /*
     * If the error is in the JSON generation, it's a server error.
     */
    if (exception instanceof JsonGenerationException)
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .type(MediaType.APPLICATION_JSON)
          .entity(
              Exceptions.buildLoggedErrorMessage(FaultType.SERVER_ERROR, "Error generating JSON",
                  null, exception))
          .build();

    final String message = exception.getMessage();

    /*
     * If we can't deserialize the JSON because someone forgot a no-arg constructor, it's a server
     * error and we should inform the developer.
     */
    if (message.startsWith("No suitable constructor found"))
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .type(MediaType.APPLICATION_JSON)
          .entity(
              Exceptions.buildLoggedErrorMessage(FaultType.SERVER_ERROR,
                  "Unable to deserialize the provided JSON", null, exception))
          .build();

    /*
     * Otherwise, it's those pesky users.
     */
    return Response.status(Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(
            Exceptions.buildLoggedErrorMessage(FaultType.BAD_REQUEST,
                "Unable to process the provided JSON",
                Exceptions.stripLocationFromStacktrace(message), exception))
        .build();
  }
}
