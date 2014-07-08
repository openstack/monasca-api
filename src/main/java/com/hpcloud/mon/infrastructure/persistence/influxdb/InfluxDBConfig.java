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
package com.hpcloud.mon.infrastructure.persistence.influxdb;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InfluxDbConfig {

  @JsonProperty
  String name;

  public String getName() {
    return name;
  }

  @JsonProperty
  int replicationFactor;

  public int getReplicationFactor() {
    return replicationFactor;
  }

  @JsonProperty
  String url;

  public String getUrl() {
    return url;
  }

  @JsonProperty
  String user;

  public String getUser() {
    return user;
  }

  @JsonProperty
  String password;

  public String getPassword() {
    return password;
  }

}
