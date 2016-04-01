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

package monasca.api.resource.exception;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Error message utilities.
 */
public final class ErrorMessages {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public interface ErrorMessageMatcher {
    void matches(String faultType, int code, String messagePrefix);

    void matches(String faultType, int code, String messagePrefix, @Nullable String detailsPrefix);
  }

  public static ErrorMessageMatcher assertThat(final String errorMessage) {
    try {
      JsonNode node = MAPPER.readTree(errorMessage);
      final String rootKey = node.fieldNames().next();
      node = node.get(rootKey);
      final ErrorMessage message = MAPPER.reader(ErrorMessage.class).readValue(node);

      return new ErrorMessageMatcher() {
        @Override
        public void matches(String faultType, int code, String messagePrefix) {
          matches(faultType, code, messagePrefix, null);
        }

        @Override
        public void matches(String faultType, int code, String messagePrefix,
            @Nullable String detailsPrefix) {
          assertEquals(rootKey, faultType);
          assertEquals(message.code, code);
          assertTrue(message.message.startsWith(messagePrefix),
              String.format("String '%s' does not start with '%s'", message.message, messagePrefix));
          if (detailsPrefix != null)
            assertTrue(message.details.startsWith(detailsPrefix), message.details);
        }
      };

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
