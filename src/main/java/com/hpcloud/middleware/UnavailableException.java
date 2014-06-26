package com.hpcloud.middleware;

/**
 * Created by johnderr on 6/25/14.
 */
public class UnavailableException extends RuntimeException {

  public UnavailableException(String msg) {
    super(msg);
  }
  public UnavailableException(String msg, Exception e) {
    super(msg, e);
  }
}
