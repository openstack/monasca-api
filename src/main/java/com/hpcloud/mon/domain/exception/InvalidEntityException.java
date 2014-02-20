package com.hpcloud.mon.domain.exception;

/**
 * Indicates that an entity is invalid.
 * 
 * @author Jonathan Halterman
 */
public class InvalidEntityException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InvalidEntityException(Exception ex, String msg) {
    super(msg, ex);
  }

  public InvalidEntityException(Exception ex, String msg, Object... args) {
    super(String.format(msg, args), ex);
  }

  public InvalidEntityException(String msg) {
    super(msg);
  }

  public InvalidEntityException(String msg, Object... args) {
    super(String.format(msg, args));
  }
}
