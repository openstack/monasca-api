package com.hpcloud.mon.app.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * Utilities for validating dimensions.
 * 
 * @author Jonathan Halterman
 * @author Todd Walk
 */
public final class DimensionValidation {
  private static final Map<String, DimensionValidator> VALIDATORS;
  private static final Pattern UUID_PATTERN = Pattern.compile("\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}");
  private static final Pattern VALID_DIMENSION_NAME = Pattern.compile("^[a-zA-Z0-9_\\.\\-]+$");

  private DimensionValidation() {
  }

  interface DimensionValidator {
    boolean isValidDimension(String name, String value);
  }

  static {
    VALIDATORS = new HashMap<String, DimensionValidator>();

    // Compute validator
    VALIDATORS.put(Namespaces.COMPUTE_NAMESPACE, new DimensionValidator() {
      @Override
      public boolean isValidDimension(String name, String value) {
        if ("instance_id".equals(name))
          return value.length() != 36 || UUID_PATTERN.matcher(value).matches();
        if ("az".equals(name))
          return Ints.tryParse(value) != null;
        return true;
      }
    });

    // Objectstore validator
    VALIDATORS.put(Namespaces.OBJECT_STORE_NAMESPACE, new DimensionValidator() {
      @Override
      public boolean isValidDimension(String name, String value) {
        if ("container".equals(name))
          return value.length() < 256 || !value.contains("/");
        return true;
      }
    });

    // Volume validator
    VALIDATORS.put(Namespaces.VOLUME_NAMESPACE, new DimensionValidator() {
      @Override
      public boolean isValidDimension(String name, String value) {
        if ("instance_id".equals(name))
          return value.length() != 36 || UUID_PATTERN.matcher(value).matches();
        if ("az".equals(name))
          return Ints.tryParse(value) != null;
        return true;
      }
    });
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
  public static void validate(String namespace, Map<String, String> dimensions) {
    NamespaceValidation.validate(namespace);
    List<String> requiredDimensions = Namespaces.getRequiredDimensions(namespace);
    if (dimensions == null) {
      if (requiredDimensions == null || requiredDimensions.isEmpty())
        return;
      throw Exceptions.unprocessableEntity("The required dimensions %s are not present",
          requiredDimensions);
    }

    // Assert the presence of required dimensions
    for (String requiredDimension : requiredDimensions)
      if (!dimensions.containsKey(requiredDimension))
        throw Exceptions.unprocessableEntity("The required dimensions %s are not present",
            requiredDimensions);

    // Validate metric_name dimension
    if (Namespaces.isReserved(namespace)) {
      String metricName = dimensions.get("metric_name");
      if (!Strings.isNullOrEmpty(metricName)
          && !Namespaces.isValidMetricName(namespace, metricName)) {
        throw Exceptions.unprocessableEntity("%s is not a valid metric_name for namespace %s",
            metricName, namespace);
      }
    }

    // Validate dimension names and values
    for (Map.Entry<String, String> dimension : dimensions.entrySet()) {
      String name = dimension.getKey();
      String value = dimension.getValue();

      // General validations
      if (Strings.isNullOrEmpty(name))
        throw Exceptions.unprocessableEntity("Dimension name cannot be empty");
      if (Strings.isNullOrEmpty(value))
        throw Exceptions.unprocessableEntity("Dimension %s cannot have an empty value", name);
      if (name.length() > 50)
        throw Exceptions.unprocessableEntity("Dimension name %s must be 50 characters or less",
            name);
      if (value.length() > 300)
        throw Exceptions.unprocessableEntity("Dimension value %s must be 300 characters or less",
            value);
      if (!VALID_DIMENSION_NAME.matcher(name).matches())
        throw Exceptions.unprocessableEntity(
            "Dimension name %s may only contain: a-z A-Z 0-9 _ - .", name);

      // Namespace specific validations
      if (!Namespaces.isValidDimensionName(namespace, name))
        throw Exceptions.unprocessableEntity("%s is not a valid dimension name for namespace %s",
            name, namespace);
      DimensionValidator validator = VALIDATORS.get(namespace);
      if (validator != null && !validator.isValidDimension(name, value))
        throw Exceptions.unprocessableEntity("%s is not a valid dimension value for namespace %s",
            value, namespace);
    }
  }
}
