package com.hpcloud.mon.app.validate;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

/**
 * Encapsulates a validation result.
 * 
 * @author Jonathan Halterman
 */
public class ValidationResult {
  private List<String> errors;

  public void addError(String format, Object... args) {
    if (errors == null)
      errors = new ArrayList<String>();
    errors.add(String.format(format, args));
  }

  public String get() {
    return errors == null ? null : Joiner.on(", ").join(errors);
  }

  public boolean hasErrors() {
    return errors != null && !errors.isEmpty();
  }
}
