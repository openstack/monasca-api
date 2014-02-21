package com.hpcloud.mon.app.validate;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.domain.exception.EntityExistsException;
import com.hpcloud.mon.resource.exception.Exceptions;
import com.sun.jersey.spi.container.WebApplication;

/**
 * Utilities for validating namespaces.
 * 
 * @author Todd Walk
 */
public class NamespaceValidation {
  private NamespaceValidation() {
  }

  /**
   * Normalizes the {@code namespace} by removing whitespace.
   */
  public static String normalize(String namespace) {
    return namespace == null ? null : CharMatcher.WHITESPACE.trimFrom(namespace);
  }

  /**
   * Validates that the *custom metric created* {@code namespace} is valid for the character
   * constraints.
   * 
   * @throws WebApplication if validation fails
   */
  public static void validate(String namespace) {
    validateSimple(namespace);

    if (Namespaces.isReserved(namespace))
      throw new EntityExistsException("namespace %s is reserved", namespace);
    if (!namespace.matches("^[a-zA-Z0-9_\\.\\-]+$"))
      throw Exceptions.unprocessableEntity("namespace %s may only contain: a-z A-Z 0-9 _ - .",
          namespace);
  }

  public static void validateSimple(String namespace) {
    if (Strings.isNullOrEmpty(namespace))
      throw Exceptions.unprocessableEntity("namespace is required");
    if (namespace.length() > 64)
      throw Exceptions.unprocessableEntity("namespace %s must be 64 characters or less", namespace);
  }
}
