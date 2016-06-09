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
package monasca.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import monasca.common.hibernate.configuration.HibernateDbConfiguration;
import monasca.common.messaging.kafka.KafkaConfiguration;
import monasca.api.infrastructure.middleware.MiddlewareConfiguration;
import monasca.api.infrastructure.persistence.vertica.VerticaDataSourceFactory;
import monasca.common.configuration.DatabaseConfiguration;

import monasca.common.configuration.InfluxDbConfiguration;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;


public class ApiConfig extends Configuration {
  @NotEmpty
  public String region;
  @NotNull
  public Boolean accessedViaHttps;
  @NotEmpty
  public String metricsTopic = "metrics";
  @NotEmpty
  public String eventsTopic = "events";
  @NotNull
  public int maxQueryLimit;
  @NotEmpty
  public String alarmStateTransitionsTopic = "alarm-state-transitions";
  @NotEmpty
  public List<Integer> validNotificationPeriods;
  @Valid
  @NotNull
  public DataSourceFactory mysql;
  @Valid
  @NotNull
  public VerticaDataSourceFactory vertica;
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
  @Valid
  public HibernateDbConfiguration hibernate;
}
