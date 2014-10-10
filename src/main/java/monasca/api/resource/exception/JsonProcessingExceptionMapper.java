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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import monasca.api.resource.exception.Exceptions.FaultType;

/**
 * Adapted from Dropwizard's JsonProcessingExceptionMapper.
 */
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {
  @Override
  public Response toResponse(JsonProcessingException exception) {
    /*
     * If the error is in the JSON generation, it's a server error.
     */
    if (exception instanceof JsonGenerationException)
      return Response
          .status(Status.INTERNAL_SERVER_ERROR)
          .type(MediaType.APPLICATION_JSON)
          .entity(
              Exceptions.buildLoggedErrorMessage(FaultType.SERVER_ERROR, "Error generating JSON",
                  null, exception)).build();

    final String message = exception.getMessage();

    /*
     * If we can't deserialize the JSON because someone forgot a no-arg constructor, it's a server
     * error and we should inform the developer.
     */
    if (message.startsWith("No suitable constructor found"))
      return Response
          .status(Status.INTERNAL_SERVER_ERROR)
          .type(MediaType.APPLICATION_JSON)
          .entity(
              Exceptions.buildLoggedErrorMessage(FaultType.SERVER_ERROR,
                  "Unable to deserialize the provided JSON", null, exception)).build();

    /*
     * Otherwise, it's those pesky users.
     */
    return Response
        .status(Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(
            Exceptions.buildLoggedErrorMessage(FaultType.BAD_REQUEST,
                "Unable to process the provided JSON",
                Exceptions.stripLocationFromStacktrace(message), exception)).build();
  }
}
