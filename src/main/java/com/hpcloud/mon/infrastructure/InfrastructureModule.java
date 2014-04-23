package com.hpcloud.mon.infrastructure;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;
import com.hpcloud.mon.domain.model.metric.MetricDefinitionRepository;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;
import com.hpcloud.mon.domain.model.statistic.StatisticRepository;
import com.hpcloud.mon.infrastructure.persistence.AlarmStateHistoryRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.AlarmRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.MeasurementRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.MetricDefinitionRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.NotificationMethodRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.StatisticRepositoryImpl;

/**
 * Infrastructure layer bindings.
 * 
 * @author Jonathan Halterman
 */
public class InfrastructureModule extends AbstractModule {
  @Override
  protected void configure() {
    // Bind repositories
    bind(AlarmRepository.class).to(AlarmRepositoryImpl.class).in(Singleton.class);
    bind(AlarmStateHistoryRepository.class).to(AlarmStateHistoryRepositoryImpl.class).in(Singleton.class);
    bind(MetricDefinitionRepository.class).to(MetricDefinitionRepositoryImpl.class).in(Singleton.class);
    bind(MeasurementRepository.class).to(MeasurementRepositoryImpl.class).in(Singleton.class);
    bind(StatisticRepository.class).to(StatisticRepositoryImpl.class).in(Singleton.class);
    bind(NotificationMethodRepository.class).to(NotificationMethodRepositoryImpl.class).in(
        Singleton.class);
  }
}
