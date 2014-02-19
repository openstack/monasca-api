package com.hpcloud.mon.domain.exception;

/**
 * Indicates that a domain entity already exists.
 * 
 * @author Jonathan Halterman
 */
public class EntityExistsException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public EntityExistsException(Exception ex, String msg) {
    super(msg, ex);
  }

  public EntityExistsException(Exception ex, String msg, Object... args) {
    super(String.format(msg, args), ex);
  }

  public EntityExistsException(String msg) {
    super(msg);
  }

  public EntityExistsException(String msg, Object... args) {
    super(String.format(msg, args));
  }
}
