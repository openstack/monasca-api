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
package monasca.api.infrastructure;

import javax.inject.Singleton;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import monasca.api.MonApiConfiguration;
import monasca.api.domain.model.alarm.AlarmRepository;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepository;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import monasca.api.domain.model.measurement.MeasurementRepository;
import monasca.api.domain.model.metric.MetricDefinitionRepository;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepository;
import monasca.api.domain.model.statistic.StatisticRepository;
import monasca.api.infrastructure.persistence.influxdb.AlarmStateHistoryInfluxDbRepositoryImpl;
import monasca.api.infrastructure.persistence.influxdb.MeasurementInfluxDbRepositoryImpl;
import monasca.api.infrastructure.persistence.influxdb.MetricDefinitionInfluxDbRepositoryImpl;
import monasca.api.infrastructure.persistence.influxdb.StatisticInfluxDbRepositoryImpl;
import monasca.api.infrastructure.persistence.mysql.AlarmDefinitionMySqlRepositoryImpl;
import monasca.api.infrastructure.persistence.mysql.AlarmMySqlRepositoryImpl;
import monasca.api.infrastructure.persistence.mysql.NotificationMethodMySqlRepositoryImpl;
import monasca.api.infrastructure.persistence.vertica.AlarmStateHistoryVerticaRepositoryImpl;
import monasca.api.infrastructure.persistence.vertica.MeasurementVerticaRepositoryImpl;
import monasca.api.infrastructure.persistence.vertica.MetricDefinitionVerticaRepositoryImpl;
import monasca.api.infrastructure.persistence.vertica.StatisticVerticaRepositoryImpl;

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
    bind(AlarmDefinitionRepository.class).to(AlarmDefinitionMySqlRepositoryImpl.class).in(
        Singleton.class);
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
