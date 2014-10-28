/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package monasca.api.infrastructure.middleware;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CS Middleware configuration.
 */
public class MiddlewareConfiguration {
  @NotNull
  public Boolean enabled;
  @NotEmpty
  @JsonProperty
  public String serviceIds;
  @NotEmpty
  @JsonProperty
  public String endpointIds;
  @NotEmpty
  @JsonProperty
  public String serverVIP;
  @NotEmpty
  @JsonProperty
  public String serverPort;
  @NotEmpty
  @JsonProperty
  public String connTimeout;
  @NotEmpty
  @JsonProperty
  public String connSSLClientAuth;
  @NotEmpty
  @JsonProperty
  public String connPoolMaxActive;
  @NotEmpty
  @JsonProperty
  public String connPoolMaxIdle;
  @NotEmpty
  @JsonProperty
  public String connPoolEvictPeriod;
  @NotEmpty
  @JsonProperty
  public String connPoolMinIdleTime;
  @NotEmpty
  @JsonProperty
  public String connRetryTimes;
  @NotEmpty
  @JsonProperty
  public String connRetryInterval;
  @NotNull
  @JsonProperty
  public List<String> defaultAuthorizedRoles;
  @NotNull
  @JsonProperty
  public List<String> agentAuthorizedRoles;
  @JsonProperty
  public String timeToCacheToken;
  @JsonProperty
  public String adminAuthMethod;
  @JsonProperty
  public String adminUser;
  @JsonProperty
  public String adminToken;
  @JsonProperty
  public String adminPassword;
  @JsonProperty
  public String maxTokenCacheSize = "1048576";
  @JsonProperty
  public String truststore;
  @JsonProperty
  public String truststorePassword;
  @JsonProperty
  public String keystore;
  @JsonProperty
  public String keystorePassword;
}
