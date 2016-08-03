/*
 * (C) Copyright 2014,2016 Hewlett Packard Enterprise Development LP
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

import com.google.inject.AbstractModule;
import com.google.inject.ProvisionException;

import monasca.api.ApiConfig;
import monasca.api.domain.model.alarm.AlarmRepo;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepo;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistoryRepo;
import monasca.api.domain.model.dimension.DimensionRepo;
import monasca.api.domain.model.measurement.MeasurementRepo;
import monasca.api.domain.model.metric.MetricDefinitionRepo;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepo;
import monasca.api.domain.model.notificationmethod.NotificationMethodTypesRepo;
import monasca.api.domain.model.statistic.StatisticRepo;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.api.infrastructure.persistence.Utils;
import monasca.api.infrastructure.persistence.hibernate.AlarmDefinitionSqlRepoImpl;
import monasca.api.infrastructure.persistence.hibernate.AlarmHibernateUtils;
import monasca.api.infrastructure.persistence.hibernate.AlarmSqlRepoImpl;
import monasca.api.infrastructure.persistence.hibernate.NotificationMethodSqlRepoImpl;
import monasca.api.infrastructure.persistence.hibernate.NotificationMethodTypesSqlRepoImpl;
import monasca.api.infrastructure.persistence.influxdb.InfluxV9AlarmStateHistoryRepo;
import monasca.api.infrastructure.persistence.influxdb.InfluxV9DimensionRepo;
import monasca.api.infrastructure.persistence.influxdb.InfluxV9MeasurementRepo;
import monasca.api.infrastructure.persistence.influxdb.InfluxV9MetricDefinitionRepo;
import monasca.api.infrastructure.persistence.influxdb.InfluxV9RepoReader;
import monasca.api.infrastructure.persistence.influxdb.InfluxV9StatisticRepo;
import monasca.api.infrastructure.persistence.influxdb.InfluxV9Utils;
import monasca.api.infrastructure.persistence.mysql.AlarmDefinitionMySqlRepoImpl;
import monasca.api.infrastructure.persistence.mysql.AlarmMySqlRepoImpl;
import monasca.api.infrastructure.persistence.mysql.MySQLUtils;
import monasca.api.infrastructure.persistence.mysql.NotificationMethodMySqlRepoImpl;
import monasca.api.infrastructure.persistence.mysql.NotificationMethodTypesMySqlRepoImpl;
import monasca.api.infrastructure.persistence.vertica.AlarmStateHistoryVerticaRepoImpl;
import monasca.api.infrastructure.persistence.vertica.DimensionVerticaRepoImpl;
import monasca.api.infrastructure.persistence.vertica.MeasurementVerticaRepoImpl;
import monasca.api.infrastructure.persistence.vertica.MetricDefinitionVerticaRepoImpl;
import monasca.api.infrastructure.persistence.vertica.StatisticVerticaRepoImpl;

/**
 * Infrastructure layer bindings.
 */
public class InfrastructureModule extends AbstractModule {

  private ApiConfig config;

  private static final String VERTICA = "vertica";
  private static final String INFLUXDB = "influxdb";
  private static final String INFLUXDB_V9 = "v9";

  public InfrastructureModule(ApiConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    final boolean hibernateEnabled = this.isHibernateEnabled();

    this.bindUtils(hibernateEnabled);

    // Bind repositories

    if (hibernateEnabled) {
      this.bind(AlarmRepo.class).to(AlarmSqlRepoImpl.class).in(Singleton.class);
      this.bind(AlarmDefinitionRepo.class).to(AlarmDefinitionSqlRepoImpl.class).in(Singleton.class);
      this.bind(NotificationMethodRepo.class).to(NotificationMethodSqlRepoImpl.class).in(Singleton.class);
      this.bind(NotificationMethodTypesRepo.class).to(NotificationMethodTypesSqlRepoImpl.class).in(Singleton.class);
    } else {
      bind(AlarmRepo.class).to(AlarmMySqlRepoImpl.class).in(Singleton.class);
      bind(AlarmDefinitionRepo.class).to(AlarmDefinitionMySqlRepoImpl.class).in(Singleton.class);
      bind(NotificationMethodRepo.class).to(NotificationMethodMySqlRepoImpl.class).in(Singleton.class);
      bind(NotificationMethodTypesRepo.class).to(NotificationMethodTypesMySqlRepoImpl.class).in(Singleton.class);
      bind(PersistUtils.class).in(Singleton.class);
    }

    if (config.databaseConfiguration.getDatabaseType().trim().equalsIgnoreCase(VERTICA)) {

      bind(AlarmStateHistoryRepo.class).to(AlarmStateHistoryVerticaRepoImpl.class).in(Singleton.class);
      bind(DimensionRepo.class).to(DimensionVerticaRepoImpl.class).in(Singleton.class);
      bind(MetricDefinitionRepo.class).to(MetricDefinitionVerticaRepoImpl.class).in(Singleton.class);
      bind(MeasurementRepo.class).to(MeasurementVerticaRepoImpl.class).in(Singleton.class);
      bind(StatisticRepo.class).to(StatisticVerticaRepoImpl.class).in(Singleton.class);

    } else if (config.databaseConfiguration.getDatabaseType().trim().equalsIgnoreCase(INFLUXDB)) {

      if (config.influxDB.getVersion() != null && !config.influxDB.getVersion()
          .equalsIgnoreCase(INFLUXDB_V9)) {

        System.err.println("Found unsupported Influxdb version: " + config.influxDB.getVersion());
        System.err.println("Supported Influxdb versions are 'v9'");
        System.err.println("Check your config file");
        System.exit(1);

      }

      bind(InfluxV9Utils.class).in(Singleton.class);
      bind(InfluxV9RepoReader.class).in(Singleton.class);
      bind(AlarmStateHistoryRepo.class).to(InfluxV9AlarmStateHistoryRepo.class).in(Singleton.class);
      bind(DimensionRepo.class).to(InfluxV9DimensionRepo.class).in(Singleton.class);
      bind(MetricDefinitionRepo.class).to(InfluxV9MetricDefinitionRepo.class).in(Singleton.class);
      bind(MeasurementRepo.class).to(InfluxV9MeasurementRepo.class).in(Singleton.class);
      bind(StatisticRepo.class).to(InfluxV9StatisticRepo.class).in(Singleton.class);

    } else {

      throw new ProvisionException("Failed to detect supported database. Supported databases are "
          + "'vertica' and 'influxdb'. Check your config file.");
    }
  }

  private boolean isHibernateEnabled() {
    return this.config.hibernate != null && this.config.hibernate.getSupportEnabled();
  }

  private void bindUtils(final boolean hibernateEnabled) {
    final Class<? extends Utils> implementation = hibernateEnabled ? AlarmHibernateUtils.class : MySQLUtils.class;
    this.bind(Utils.class).to(implementation).in(Singleton.class);
  }

}
