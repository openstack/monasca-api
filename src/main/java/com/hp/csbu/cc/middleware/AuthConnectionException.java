package com.hp.csbu.cc.middleware;

/**
 * An exception to indicate any connection issue.
 * 
 * @author liemmn
 *
 */
public class AuthConnectionException extends RuntimeException {
  private static final long serialVersionUID = 4318025130590973448L;

  public AuthConnectionException(String msg) {
    super(msg);
  }
  public AuthConnectionException(String msg, Exception e) {
    super(msg, e);
  }
}
