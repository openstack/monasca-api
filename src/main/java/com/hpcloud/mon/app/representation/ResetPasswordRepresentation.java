package com.hpcloud.mon.app.representation;

/**
 * A reset password.
 * 
 * @author Jonathan Halterman
 */
public class ResetPasswordRepresentation {
  public String password;

  public ResetPasswordRepresentation() {
  }

  public ResetPasswordRepresentation(String password) {
    this.password = password;
  }
}
