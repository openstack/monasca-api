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
package com.hpcloud.mon.infrastructure;

import javax.inject.Singleton;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;
import com.hpcloud.mon.domain.model.metric.MetricDefinitionRepository;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;
import com.hpcloud.mon.domain.model.statistic.StatisticRepository;
import com.hpcloud.mon.infrastructure.persistence.influxdb.AlarmStateHistoryInfluxDbRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.influxdb.MeasurementInfluxDbRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.influxdb.MetricDefinitionInfluxDbRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.influxdb.StatisticInfluxDbRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.mysql.AlarmMySqlRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.mysql.NotificationMethodMySqlRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.vertica.AlarmStateHistoryVerticaRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.vertica.MeasurementVerticaRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.vertica.MetricDefinitionVerticaRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.vertica.StatisticVerticaRepositoryImpl;

/**
 * Infrastructure layer bindings.
 */
public class InfrastructureModule extends AbstractModule {
  private MonApiConfiguration config;

  public InfrastructureModule(MonApiConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    // Bind repositories
    bind(AlarmRepository.class).to(AlarmMySqlRepositoryImpl.class).in(Singleton.class);
    if (config.databaseConfiguration.getDatabaseType().trim().toLowerCase().equals("vertica")) {
      bind(AlarmStateHistoryRepository.class).to(AlarmStateHistoryVerticaRepositoryImpl.class).in(
          Singleton.class);
      bind(MetricDefinitionRepository.class).to(MetricDefinitionVerticaRepositoryImpl.class).in(
          Singleton.class);
      bind(MeasurementRepository.class).to(MeasurementVerticaRepositoryImpl.class).in(
          Singleton.class);
      bind(StatisticRepository.class).to(StatisticVerticaRepositoryImpl.class).in(Singleton.class);
    } else if (config.databaseConfiguration.getDatabaseType().trim().toLowerCase()
        .equals("influxdb")) {
      bind(AlarmStateHistoryRepository.class).to(AlarmStateHistoryInfluxDbRepositoryImpl.class).in(
          Singleton.class);
      bind(MetricDefinitionRepository.class).to(MetricDefinitionInfluxDbRepositoryImpl.class).in(
          Singleton.class);
      bind(MeasurementRepository.class).to(MeasurementInfluxDbRepositoryImpl.class).in(
          Singleton.class);
      bind(StatisticRepository.class).to(StatisticInfluxDbRepositoryImpl.class).in(Singleton.class);
    } else {
      throw new ProvisionException("Failed to detect supported database. Supported databases are "
          + "'vertica' and 'influxdb'. Check your config file.");
    }

    bind(NotificationMethodRepository.class).to(NotificationMethodMySqlRepositoryImpl.class).in(
        Singleton.class);
  }

  @Provides
  InfluxDB provideInfluxDB() {
    InfluxDB influxDB =
        InfluxDBFactory.connect(this.config.influxDB.getUrl(), this.config.influxDB.getUser(),
            this.config.influxDB.getPassword());
    return influxDB;
  }
}
