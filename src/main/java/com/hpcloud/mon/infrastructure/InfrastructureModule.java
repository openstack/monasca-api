/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpcloud.mon.infrastructure;

import com.google.inject.AbstractModule;
import com.google.inject.ProvisionException;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;
import com.hpcloud.mon.domain.model.metric.MetricDefinitionRepository;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;
import com.hpcloud.mon.domain.model.statistic.StatisticRepository;
import com.hpcloud.mon.infrastructure.persistence.*;

import javax.inject.Singleton;

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
        bind(AlarmRepository.class).to(AlarmRepositoryImpl.class).in(Singleton.class);
        if (config.databaseConfiguration.getDatabaseType().trim().toLowerCase().equals("vertica")) {
            bind(AlarmStateHistoryRepository.class).to(AlarmStateHistoryRepositoryImpl.class).in(Singleton.class);
            bind(MetricDefinitionRepository.class).to(MetricDefinitionRepositoryImpl.class).in(Singleton.class);
            bind(MeasurementRepository.class).to(MeasurementRepositoryImpl.class).in(Singleton.class);
            bind(StatisticRepository.class).to(StatisticRepositoryImpl.class).in(Singleton.class);
        } else if (config.databaseConfiguration.getDatabaseType().trim().toLowerCase().equals("influxdb")) {
            bind(AlarmStateHistoryRepository.class).to(AlarmStateHistoryInfluxDBRepositoryImpl.class).in(Singleton.class);
            bind(MetricDefinitionRepository.class).to(MetricDefinitionInfluxDBRepositoryImpl.class).in(Singleton.class);
            bind(MeasurementRepository.class).to(MeasurementInfluxDBRepositoryImpl.class).in(Singleton.class);
            bind(StatisticRepository.class).to(StatisticInfluxDBRepositoryImpl.class).in(Singleton.class);
        } else {
            throw new ProvisionException("Failed to detect supported database. Supported databases are 'vertica' and 'influxdb'. Check your config file.");
        }

        bind(NotificationMethodRepository.class).to(NotificationMethodRepositoryImpl.class).in(
                Singleton.class);
    }
}
