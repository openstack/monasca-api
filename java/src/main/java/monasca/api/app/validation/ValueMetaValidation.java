/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P.
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

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import monasca.api.resource.exception.Exceptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

/**
 * Utilities for validating valueMeta.
 */
public final class ValueMetaValidation {
  private static final int VALUE_META_MAX_NUMBER = 16;
  private static final int VALUE_META_VALUE_MAX_LENGTH = 2048;
  private static final int VALUE_META_NAME_MAX_LENGTH = 255;
  private static final Map<String, String> EMPTY_VALUE_META = Collections
      .unmodifiableMap(new HashMap<String, String>());

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private ValueMetaValidation() {}

  /**
   * Normalizes valueMeta by stripping whitespace from name. validate() must
   * already have been called on the valueMeta
   */
  public static Map<String, String> normalize(Map<String, String> valueMeta) {
    if (valueMeta == null || valueMeta.isEmpty()) {
      return EMPTY_VALUE_META;
    }
    final Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, String> entry : valueMeta.entrySet()) {
      final String key = CharMatcher.WHITESPACE.trimFrom(entry.getKey());
      result.put(key, entry.getValue());
    }

    return result;
  }

  /**
   * Validates that the given {@code valueMetas} are valid.
   *
   * @throws WebApplicationException if validation fails
   */
  public static void validate(Map<String, String> valueMetas) {
    if (valueMetas.size() > VALUE_META_MAX_NUMBER) {
      throw Exceptions.unprocessableEntity("Maximum number of valueMeta key/value pairs is %d",
          VALUE_META_MAX_NUMBER);
    }

    // Validate valueMeta names and values
    for (Map.Entry<String, String> valueMeta : valueMetas.entrySet()) {
      // Have to check for null first because later check is for trimmed name
      if (valueMeta.getKey() == null) {
        throw Exceptions.unprocessableEntity("valueMeta name cannot be empty");
      }
      final String name = CharMatcher.WHITESPACE.trimFrom(valueMeta.getKey());
      String value = valueMeta.getValue();
      if (value == null) {
        // Store nulls as empty strings
        value = "";
      }

      // General validations
      if (Strings.isNullOrEmpty(name)) {
        throw Exceptions.unprocessableEntity("valueMeta name cannot be empty");
      }
      if (name.length() > VALUE_META_NAME_MAX_LENGTH) {
        throw Exceptions.unprocessableEntity("valueMeta name %s must be %d characters or less",
            name, VALUE_META_NAME_MAX_LENGTH);
      }
    }
    verifyValueMetaStringLength(valueMetas);
  }

  private static void verifyValueMetaStringLength(Map<String, String> valueMetas) {

    try {
      String valueMetaString = objectMapper.writeValueAsString(valueMetas);

      if (valueMetaString.length() > VALUE_META_VALUE_MAX_LENGTH) {
        throw Exceptions.unprocessableEntity("valueMeta name value combinations %s must be %d characters or less",
          valueMetaString, VALUE_META_VALUE_MAX_LENGTH);
       }
    } catch (JsonProcessingException e) {
      throw Exceptions.unprocessableEntity("Failed to serialize valueMeta combinations %s", valueMetas);
    }
  }
}

