package com.hpcloud.mon.infrastructure.middleware;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CS Middleware configuration.
 * 
 * @author Jonathan Halterman
 */
public class MiddlewareConfiguration {
  @NotNull public Boolean enabled;
  @NotEmpty @JsonProperty public String serviceIds;
  @NotEmpty @JsonProperty public String endpointIds;
  @NotEmpty @JsonProperty public String serverVIP;
  @NotEmpty @JsonProperty public String serverPort;
  @NotEmpty @JsonProperty public String connTimeout;
  @NotEmpty @JsonProperty public String connSSLClientAuth;
  @NotEmpty @JsonProperty public String keystore;
  @NotEmpty @JsonProperty public String keystorePass;
  @NotEmpty @JsonProperty public String truststore;
  @NotEmpty @JsonProperty public String truststorePass;
  @NotEmpty @JsonProperty public String connPoolMaxActive;
  @NotEmpty @JsonProperty public String connPoolMaxIdle;
  @NotEmpty @JsonProperty public String connPoolEvictPeriod;
  @NotEmpty @JsonProperty public String connPoolMinIdleTime;
  @NotEmpty @JsonProperty public String connRetryTimes;
  @NotEmpty @JsonProperty public String connRetryInterval;
  @NotNull @JsonProperty public String[] rolesToMatch;
}
