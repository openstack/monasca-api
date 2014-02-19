package com.hpcloud.mon.app.validate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.hpcloud.mon.MonApiConfiguration.CloudServiceConfiguration;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.domain.service.ResourceVerificationService;
import com.hpcloud.mon.resource.exception.Exceptions;
import com.hpcloud.util.Injector;

/**
 * Validation related utilities.
 * 
 * @author Jonathan Halterman
 */
public final class Validation {
  private static final DateTimeFormatter ISO_8601_FORMATTER = ISODateTimeFormat.dateTimeParser();
  private static final List<String> VALID_STATISTICS = Arrays.asList("avg", "min", "max", "sum",
      "count");

  private Validation() {
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
      if (!VALID_STATISTICS.contains(statistic))
        throw Exceptions.unprocessableEntity("%s is not a valid statistic", statistic);
      validStats.add(statisticLower);
    }

    if (validStats.isEmpty())
      throw Exceptions.unprocessableEntity("Statistics are required");

    return validStats;
  }

  /**
   * @throws WebApplicationException if the {@code value} is null or empty.
   */
  public static void validateNotNullOrEmpty(String value, String parameterName) {
    if (Strings.isNullOrEmpty(value))
      throw Exceptions.unprocessableEntity("%s is required", parameterName);
  }

  /**
   * Verifies that the {@code tenantId} owns any resources referenced in the {@code dimensions} for
   * the {@code namespace}.
   */
  public static void verifyOwnership(String tenantId, String namespace,
      Map<String, String> dimensions, CloudServiceConfiguration serviceConfig, String authToken) {
    String serviceVersion = serviceConfig == null ? null : serviceConfig.version;
    String resourceIdDim = Namespaces.getResourceIdDimension(namespace, serviceVersion);
    String secondaryResourceIdDim = Namespaces.getSecondaryResourceIdDimension(namespace,
        serviceVersion);

    if (resourceIdDim != null && Injector.isBound(ResourceVerificationService.class, namespace)) {
      ResourceVerificationService verificationService = Injector.getInstance(
          ResourceVerificationService.class, namespace);
      String resourceId = dimensions.get(resourceIdDim);
      if (resourceId == null)
        throw Exceptions.badRequest("Missing required dimension %s", resourceIdDim);
      String secondaryResourceId = secondaryResourceIdDim == null ? resourceId
          : dimensions.get(secondaryResourceIdDim);

      if (verificationService != null
          && !verificationService.isVerifiedOwner(tenantId, resourceId, secondaryResourceId,
              dimensions.get("az"), authToken))
        throw Exceptions.badRequest("The %s resource %s is not owned by tenant %s", namespace,
            resourceId, tenantId);
    }
  }
}
