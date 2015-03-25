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
package monasca.api.app.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import monasca.common.model.Services;
import monasca.api.resource.exception.Exceptions;

/**
 * Validation related utilities.
 */
public final class Validation {
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();
  private static final Splitter COLON_SPLITTER = Splitter.on(':').omitEmptyStrings().trimResults().limit(2);
  private static final DateTimeFormatter ISO_8601_FORMATTER = ISODateTimeFormat
      .dateOptionalTimeParser().withZoneUTC();
  private static final List<String> VALID_STATISTICS = Arrays.asList("avg", "min", "max", "sum",
      "count");
  private static final List<String> VALID_ALARM_STATE = Arrays
      .asList("undetermined", "ok", "alarm");

  private Validation() {}

  /**
   * @throws JsonMappingException if the {@code value} is not valid for the {@code type}
   */
  public static <T extends Enum<T>> T parseAndValidate(Class<T> type, String value)
      throws JsonMappingException {
    for (T constant : type.getEnumConstants())
      if (constant.name().equalsIgnoreCase(value))
        return constant;
    List<String> acceptedValues = new ArrayList<>();
    for (T constant : type.getEnumConstants())
      acceptedValues.add(constant.name());
    throw new JsonMappingException(String.format("%s was not one of %s", value, acceptedValues));
  }

  /**
   * @throws WebApplicationException if the {@code date} invalid or is required and null.
   */
  public static DateTime parseAndValidateDate(String date, String parameterName, boolean required) {
    if (Strings.isNullOrEmpty(date)) {
      if (required)
        throw Exceptions.unprocessableEntity("%s is required", parameterName);
      else
        return null;
    }

    try {
      return ISO_8601_FORMATTER.parseDateTime(date);
    } catch (Exception e) {
      throw Exceptions.unprocessableEntity("%s must be an ISO 8601 formatted time", parameterName);
    }
  }

  /**
   * @throws WebApplicationException if the {@code value} is null or empty.
   */
  public static Map<String, String> parseAndValidateNameAndDimensions(String name,
                                                                      String dimensionsStr,
                                                                      boolean nameRequiredFlag) {
    Map<String, String> dimensions = parseAndValidateDimensions(dimensionsStr);

    String service = dimensions.get(Services.SERVICE_DIMENSION);
    MetricNameValidation.validate(name, service, nameRequiredFlag);
    return dimensions;
  }

  /**
   * @throws WebApplicationException if the {@code value} is null or empty.
   */
  public static Map<String, String> parseAndValidateDimensions(String dimensionsStr) {
    Validation.validateNotNullOrEmpty(dimensionsStr, "dimensions");

    Map<String, String> dimensions = new HashMap<String, String>();
    for (String dimensionStr : COMMA_SPLITTER.split(dimensionsStr)) {
      String[] dimensionArr = Iterables.toArray(COLON_SPLITTER.split(dimensionStr), String.class);
      if (dimensionArr.length == 2)
        dimensions.put(dimensionArr[0], dimensionArr[1]);
    }

    String service = dimensions.get(Services.SERVICE_DIMENSION);
    DimensionValidation.validate(dimensions, service);
    return dimensions;
  }

  /**
   * @throws WebApplicationException if the {@code number} is invalid.
   */
  public static int parseAndValidateNumber(String number, String parameterName) {
    try {
      return Integer.parseInt(number);
    } catch (NumberFormatException e) {
      throw Exceptions.unprocessableEntity("%s must be valid number", parameterName);
    }
  }

  /**
   * @throws WebApplicationException if the {@code statistics} empty or invalid.
   */
  public static List<String> parseValidateAndNormalizeStatistics(Iterable<String> statistics) {
    List<String> validStats = new ArrayList<String>(5);
    for (String statistic : statistics) {
      String statisticLower = statistic.toLowerCase();
      if (!VALID_STATISTICS.contains(statisticLower))
        throw Exceptions.unprocessableEntity("%s is not a valid statistic", statistic);
      validStats.add(statisticLower);
    }

    if (validStats.isEmpty())
      throw Exceptions.unprocessableEntity("Statistics are required");

    return validStats;
  }

  /**
   * @throws WebApplicationException if the {@code statistics} empty or invalid.
   */
  public static void validateAlarmState(String state) {
    String stateLower = state.toLowerCase();
    if (!VALID_ALARM_STATE.contains(stateLower)) {
      throw Exceptions.unprocessableEntity("%s is not a valid state", state);
    }
  }

  /**
   * @throws WebApplicationException if the {@code value} is null or empty.
   */
  public static void validateNotNullOrEmpty(String value, String parameterName) {
    if (Strings.isNullOrEmpty(value))
      throw Exceptions.unprocessableEntity("%s is required", parameterName);
  }

  /**
   * @throws WebApplicationException if the {@code startTime} or {@code endTime} are invalid
   */
  public static void validateTimes(DateTime startTime, DateTime endTime) {
    if (!startTime.isBefore(endTime))
        throw Exceptions.badRequest("start_time must be before end_time");
  }
}
