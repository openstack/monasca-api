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

import java.util.Random;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.base.Splitter;

/**
 * Exception factory methods.
 */
public final class Exceptions {
  private static final Logger LOG = LoggerFactory.getLogger(Exceptions.class);
  private static final ObjectMapper OBJECT_MAPPER;
  private static final Splitter LINE_SPLITTER = Splitter.on("\n").trimResults();
  private static final Random RANDOM = new Random();

  static {
    OBJECT_MAPPER = new ObjectMapper();
    OBJECT_MAPPER
        .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
  }

  public enum FaultType {

    SERVER_ERROR(Status.INTERNAL_SERVER_ERROR, true),
    BAD_REQUEST(Status.BAD_REQUEST, true),
    UNAUTHORIZED(Status.UNAUTHORIZED, false),
    NOT_FOUND(Status.NOT_FOUND, true),
    CONFLICT(Status.CONFLICT, true),
    UNPROCESSABLE_ENTITY(422, true),
    FORBIDDEN(Status.FORBIDDEN, true);

    public final int statusCode;
    public final boolean loggable;

    FaultType(int statusCode, boolean loggable) {
      this.statusCode = statusCode;
      this.loggable = loggable;
    }

    FaultType(Status status, boolean loggable) {
      this.statusCode = status.getStatusCode();
      this.loggable = loggable;
    }

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  private static class WebAppException extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public WebAppException(FaultType faultType, String message) {
      super(Response.status(faultType.statusCode).entity(message).type(MediaType.APPLICATION_JSON)
          .build());
    }
  }

  private Exceptions() {}

  public static WebApplicationException badRequest(String msg, Object... args) {
    return new WebAppException(FaultType.BAD_REQUEST, buildLoggedErrorMessage(
        FaultType.BAD_REQUEST, msg, args));
  }

  /**
   * Builds and returns an error message containing an error code, and logs the message with the
   * corresponding error code.
   */
  public static String buildLoggedErrorMessage(FaultType faultType, String message, Object... args) {
    return buildLoggedErrorMessage(faultType,
        args == null || args.length == 0 ? message : String.format(message, args), null, null);
  }

  /**
   * Builds and returns an error message containing an error code, and logs the message with the
   * corresponding error code.
   */
  public static String buildLoggedErrorMessage(FaultType faultType, String message,
      @Nullable String details, @Nullable Throwable exception) {
    String errorCode = Long.toHexString(RANDOM.nextLong());

    if (faultType.loggable) {
      String withoutDetails = "{} {} - {}";
      String withDetails = "{} {} - {} {}";

      if (details == null) {
        if (exception == null)
          LOG.error(withoutDetails, faultType.name(), errorCode, message);
        else
          LOG.error(withoutDetails, faultType.name(), errorCode, message, exception);
      } else {
        if (exception == null)
          LOG.error(withDetails, faultType.name(), errorCode, message, details);
        else
          LOG.error(withDetails, faultType.name(), errorCode, message, details, exception);
      }
    }

    try {
      StringBuilder str = new StringBuilder("{\"");
      str.append(faultType.toString());
      str.append("\":");
      str.append(OBJECT_MAPPER.writeValueAsString(new ErrorMessage(faultType.statusCode, message,
          details, errorCode)));
      str.append("}");
      return str.toString();
    } catch (JsonProcessingException bestEffort) {
      return null;
    }
  }

  public static WebApplicationException forbidden(String msg, Object... args) {
    return new WebAppException(FaultType.FORBIDDEN, buildLoggedErrorMessage(FaultType.FORBIDDEN,
        msg, args));
  }

  /**
   * Returns the first line off of a stacktrace message.
   */
  public static String stripLocationFromStacktrace(String message) {
    for (String s : LINE_SPLITTER.split(message))
      return s;
    return message;
  }

  /**
   * Indicates that the content of a a POSTed request entity is invalid.
   */
  public static WebApplicationException unprocessableEntity(String msg, Object... args) {
    return new WebAppException(FaultType.UNPROCESSABLE_ENTITY, buildLoggedErrorMessage(
        FaultType.UNPROCESSABLE_ENTITY, msg, args));
  }

  /**
   * Indicates that the content of a a POSTed request entity is invalid.
   */
  public static WebApplicationException unprocessableEntityDetails(String msg, String details,
      Exception exception) {
    return new WebAppException(FaultType.UNPROCESSABLE_ENTITY, buildLoggedErrorMessage(
        FaultType.UNPROCESSABLE_ENTITY, msg, details, exception));
  }
}
