package com.hpcloud.mon.resource.exception;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Error message utilities.
 * 
 * @author Jonathan Halterman
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
          assertTrue(message.message.startsWith(messagePrefix), message.message);
          if (detailsPrefix != null)
            assertTrue(message.details.startsWith(detailsPrefix), message.details);
        }
      };

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
