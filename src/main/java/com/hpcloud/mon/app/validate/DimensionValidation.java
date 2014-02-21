package com.hpcloud.mon.app.validate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.hpcloud.mon.MonApiConfiguration.CloudServiceConfiguration;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * Utilities for validating dimensions.
 * 
 * @author Jonathan Halterman
 * @author Todd Walk
 */
public final class DimensionValidation {
  private DimensionValidation() {
  }

  /**
   * Normalizes dimensions by stripping whitespace.
   */
  public static Map<String, String> normalize(Map<String, String> dimensions) {
    if (dimensions == null)
      return null;
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, String> dimension : dimensions.entrySet()) {
      String dimensionKey = null;
      if (dimension.getKey() != null) {
        dimensionKey = CharMatcher.WHITESPACE.trimFrom(dimension.getKey());
        if (dimensionKey.isEmpty())
          dimensionKey = null;
      }
      String dimensionValue = null;
      if (dimension.getValue() != null) {
        dimensionValue = CharMatcher.WHITESPACE.trimFrom(dimension.getValue());
        if (dimensionValue.isEmpty())
          dimensionValue = null;
      }
      result.put(dimensionKey, dimensionValue);
    }

    return result;
  }

  /**
   * Validates that the given {@code dimensions} are valid for the {@code namespace}.
   * 
   * @throws WebApplicationException if validation fails
   */
  public static void validate(String namespace, Map<String, String> dimensions,
      @Nullable CloudServiceConfiguration config) {
    NamespaceValidation.validateSimple(namespace);
    List<String> requiredDimensions = Namespaces.getRequiredDimensions(namespace,
        config == null ? null : config.version);
    if (dimensions == null) {
      if (requiredDimensions == null || requiredDimensions.isEmpty())
        return;
      throw Exceptions.unprocessableEntity("The required dimensions %s are not present",
          requiredDimensions);
    }

    // Ensure all required dimensions are present
    for (String requiredDimension : requiredDimensions)
      if (!dimensions.containsKey(requiredDimension))
        throw Exceptions.unprocessableEntity("The required dimensions %s are not present",
            requiredDimensions);

    for (Map.Entry<String, String> dimension : dimensions.entrySet()) {
      // Generic checks
      if (Strings.isNullOrEmpty(dimension.getKey()))
        throw Exceptions.unprocessableEntity("Dimension name cannot be empty");
      if (Strings.isNullOrEmpty(dimension.getValue()))
        throw Exceptions.unprocessableEntity("Dimension name %s cannot have an emtpy value",
            dimension.getKey());
      if (dimension.getKey().length() > 50)
        throw Exceptions.unprocessableEntity("Dimension name %s must be 50 characters or less",
            dimension.getKey());
      if (dimension.getValue().length() > 300)
        throw Exceptions.unprocessableEntity("Dimension value %s must be 300 characters or less",
            dimension.getValue());
      if (!dimension.getKey().matches("^[a-zA-Z0-9_\\.\\-]+$"))
        throw Exceptions.unprocessableEntity(
            "Dimension name %s may only contain: a-z A-Z 0-9 _ - .", dimension.getKey());
      // TODO: Redo the below code for a character exclusion list, which is to be determined
      if (Strings.isNullOrEmpty(dimension.getValue()))
        throw Exceptions.unprocessableEntity("Dimension values may not be empty");

      // Namespace specific checks
      if (!Namespaces.isValidDimensionName(namespace, dimension.getKey()))
        throw Exceptions.unprocessableEntity("%s is not a valid dimension for namespace %s",
            dimension.getKey(), namespace);
      if (!Namespaces.isValidDimensionValue(namespace, dimension.getKey(), dimension.getValue()))
        throw Exceptions.unprocessableEntity("The value of dimension %s is invalid",
            dimension.getKey());
    }
  }
}
