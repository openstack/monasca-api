package com.hpcloud.mon.app.validation;

import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.resource.exception.Exceptions;
import com.sun.jersey.spi.container.WebApplication;

/**
 * Utilities for validating namespaces.
 * 
 * @author Todd Walk
 */
public class NamespaceValidation {
  private static final Pattern VALID_NAMESPACE = Pattern.compile("^[a-zA-Z0-9_\\.\\-]+$");

  private NamespaceValidation() {
  }

  /**
   * Normalizes the {@code namespace} by removing whitespace.
   */
  public static String normalize(String namespace) {
    return namespace == null ? null : CharMatcher.WHITESPACE.trimFrom(namespace);
  }

  /**
   * Validates the {@code namespace} for the character constraints.
   * 
   * @throws WebApplication if validation fails
   */
  public static void validate(String namespace) {
    if (Strings.isNullOrEmpty(namespace))
      throw Exceptions.unprocessableEntity("namespace is required");
    if (namespace.length() > 64)
      throw Exceptions.unprocessableEntity("namespace %s must be 64 characters or less", namespace);
    if (!Namespaces.isReserved(namespace) && !VALID_NAMESPACE.matcher(namespace).matches())
      throw Exceptions.unprocessableEntity("namespace %s may only contain: a-z A-Z 0-9 _ - .",
          namespace);
  }
}
