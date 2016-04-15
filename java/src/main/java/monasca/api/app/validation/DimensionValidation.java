/*
 * Copyright (c) 2014,2016 Hewlett Packard Enterprise Development Company, L.P.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import monasca.common.model.Services;
import monasca.api.resource.exception.Exceptions;

/**
 * Utilities for validating dimensions.
 */
public final class DimensionValidation {
  private static final Map<String, DimensionValidator> VALIDATORS;
  private static final Pattern UUID_PATTERN = Pattern
      .compile("\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}");
  private static final Pattern VALID_DIMENSION_NAME = Pattern.compile("[^><={}(),\"\\\\;&\\|]+$");
  private static final String INVALID_CHAR_STRING = "> < = { } ( ) \" \\ , ; & |";

  private DimensionValidation() {}

  interface DimensionValidator {
    boolean isValidDimension(String name, String value);
  }

  static {
    VALIDATORS = new HashMap<String, DimensionValidator>();

    // Compute validator
    VALIDATORS.put(Services.COMPUTE_SERVICE, new DimensionValidator() {
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
    VALIDATORS.put(Services.OBJECT_STORE_SERVICE, new DimensionValidator() {
      @Override
      public boolean isValidDimension(String name, String value) {
        if ("container".equals(name))
          return value.length() < 256 || !value.contains("/");
        return true;
      }
    });

    // Volume validator
    VALIDATORS.put(Services.VOLUME_SERVICE, new DimensionValidator() {
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
   * Validates that the given {@code dimensions} are valid.
   *
   * @throws WebApplicationException if validation fails
   */
  public static void validate(Map<String, String> dimensions) {
    // Validate dimension names and values
    for (Map.Entry<String, String> dimension : dimensions.entrySet()) {
      String name = dimension.getKey();
      String value = dimension.getValue();

      // General validations
      validateDimensionName(name);
      validateDimensionValue(value, name, false);
    }
  }

  /**
   * Validates a list of dimension names
   * @param names
   */
  public static void validateNames(List<String> names) {
    if (names != null) {
      for (String name : names) {
        validateDimensionName(name);
      }
    }
  }

  /**
   * Validates a dimension name
   * @param name Dimension name
   */
  public static void validateName(String name) {
    validateDimensionName(name);
  }

  /**
   * Validates a dimension value
   * @param value Dimension value
   * @param name Dimension name of the value
   */
  public static void validateValue(String value, String name) {
    validateDimensionValue(value, name, true);
  }

  /**
   * Validates a dimension name
   * @param name Dimension name
   */
  public static void validateDimensionName(String name) {
    if (Strings.isNullOrEmpty(name)) {
      throw Exceptions.unprocessableEntity("Dimension name cannot be empty");
    }
    if (name.length() > 255) {
      throw Exceptions.unprocessableEntity("Dimension name '%s' must be 255 characters or less",
                                           name);
    }
    // Dimension name that start with underscores are reserved for internal use only.
    if (name.startsWith("_")) {
      throw Exceptions.unprocessableEntity("Dimension name '%s' cannot start with underscore (_)",
                                           name);
    }

    if (!VALID_DIMENSION_NAME.matcher(name).matches()) {
      throw Exceptions.unprocessableEntity(
          "Dimension name '%s' may not contain: %s", name, INVALID_CHAR_STRING);
    }
  }

  /**
   * Validates a dimension value
   * @param value Dimension value
   * @param name Dimension name of the value
   * @param nullValueOk whether or not a null value is valid
   */
  public static void validateDimensionValue(String value, String name, boolean nullValueOk) {
    if (value == null && nullValueOk) {
      return;
    }
    if (Strings.isNullOrEmpty(value)) {
      throw Exceptions.unprocessableEntity("Dimension '%s' cannot have an empty value", name);
    }
    if (value.length() > 255) {
      throw Exceptions.unprocessableEntity("Dimension '%s' value '%s' must be 255 characters or less",
                                           name, value);
    }

    if (!VALID_DIMENSION_NAME.matcher(value).matches()) {
      throw Exceptions.unprocessableEntity(
          "Dimension '%s' value '%s' may not contain: %s", name, value,
          INVALID_CHAR_STRING);
    }
  }
}
