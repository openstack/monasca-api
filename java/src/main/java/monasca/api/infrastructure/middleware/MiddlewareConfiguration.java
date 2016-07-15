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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CS Middleware configuration.
 */
public class MiddlewareConfiguration {
  public Boolean enabled = false;
  @JsonProperty
  public String serverVIP;
  @JsonProperty
  public String serverPort;
  @JsonProperty
  public Boolean useHttps = Boolean.FALSE;
  @JsonProperty
  public String connTimeout = "500";
  @JsonProperty
  public Boolean connSSLClientAuth = Boolean.FALSE;
  @JsonProperty
  public String connPoolMaxActive = "3";
  @JsonProperty
  public String connPoolMaxIdle = "3";
  @JsonProperty
  public String connPoolEvictPeriod = "600000";
  @JsonProperty
  public String connPoolMinIdleTime = "600000";
  @JsonProperty
  public String connRetryTimes = "2";
  @JsonProperty
  public String connRetryInterval = "50";
  @JsonProperty
  public List<String> defaultAuthorizedRoles;
  @JsonProperty
  public List<String> readOnlyAuthorizedRoles;
  @JsonProperty
  public List<String> agentAuthorizedRoles;
  @JsonProperty
  public String delegateAuthorizedRole;
  @JsonProperty
  public String adminRole;
  @JsonProperty
  public String timeToCacheToken = "600";
  @JsonProperty
  public String adminAuthMethod;
  @JsonProperty
  public String adminUser;
  @JsonProperty
  public String adminToken;
  @JsonProperty
  public String adminPassword;
  @JsonProperty
  public String adminProjectId = "";
  @JsonProperty
  public String adminProjectName = "";
  @JsonProperty
  public String adminUserDomainId = "";
  @JsonProperty
  public String adminUserDomainName = "";
  @JsonProperty
  public String adminProjectDomainId = "";
  @JsonProperty
  public String adminProjectDomainName = "";
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
