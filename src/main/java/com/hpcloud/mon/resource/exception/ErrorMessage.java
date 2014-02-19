package com.hpcloud.mon.resource.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

public class ErrorMessage {
  public int code;
  public String message;
  public String details;
  @JsonProperty("internal_code") public String internalCode;

  ErrorMessage() {
  }

  public ErrorMessage(int code, String message, String details, String internalCode) {
    Preconditions.checkNotNull(internalCode, "internalCode");

    this.code = code;
    this.message = message == null ? "" : message;
    this.details = details == null ? "" : details;
    this.internalCode = internalCode;
  }
}
