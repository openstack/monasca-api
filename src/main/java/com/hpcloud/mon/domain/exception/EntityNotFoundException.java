package com.hpcloud.mon.domain.exception;

/**
 * Indicates that a domain entity is unknown.
 * 
 * @author Jonathan Halterman
 */
public class EntityNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public EntityNotFoundException(Exception ex, String msg) {
    super(msg, ex);
  }

  public EntityNotFoundException(Exception ex, String msg, Object... args) {
    super(String.format(msg, args), ex);
  }

  public EntityNotFoundException(String msg) {
    super(msg);
  }

  public EntityNotFoundException(String msg, Object... args) {
    super(String.format(msg, args));
  }
}
