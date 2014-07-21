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
package com.hpcloud.mon;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hpcloud.messaging.kafka.KafkaConfiguration;
import com.hpcloud.mon.infrastructure.middleware.MiddlewareConfiguration;
import com.hpcloud.configuration.DatabaseConfiguration;

import com.hpcloud.configuration.InfluxDbConfiguration;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;


public class MonApiConfiguration extends Configuration {
  @NotEmpty
  public String region;
  @NotNull
  public Boolean accessedViaHttps;
  @NotEmpty
  public String metricsTopic = "metrics";
  @NotEmpty
  public String eventsTopic = "events";
  @NotEmpty
  public String alarmStateTransitionsTopic = "alarm-state-transitions";

  @Valid
  @NotNull
  public DataSourceFactory mysql;
  @Valid
  @NotNull
  public DataSourceFactory vertica;
  @Valid
  @NotNull
  public KafkaConfiguration kafka;
  @Valid
  @NotNull
  public MiddlewareConfiguration middleware;
  @Valid
  public InfluxDbConfiguration influxDB;
  @Valid
  @JsonProperty
  public DatabaseConfiguration databaseConfiguration;

}
