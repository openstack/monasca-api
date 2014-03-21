package com.hpcloud.mon.infrastructure;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.hpcloud.mon.common.model.Services;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;
import com.hpcloud.mon.domain.model.metric.MetricRepository;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;
import com.hpcloud.mon.domain.service.ResourceVerificationService;
import com.hpcloud.mon.infrastructure.compute.ComputeResourceVerificationService;
import com.hpcloud.mon.infrastructure.identity.IdentityServiceClient;
import com.hpcloud.mon.infrastructure.objectstore.ObjectStoreResourceVerificationService;
import com.hpcloud.mon.infrastructure.persistence.AlarmRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.MeasurementRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.MetricRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.NotificationMethodRepositoryImpl;

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
    bind(MetricRepository.class).to(MetricRepositoryImpl.class).in(Singleton.class);
    bind(MeasurementRepository.class).to(MeasurementRepositoryImpl.class).in(Singleton.class);
    bind(NotificationMethodRepository.class).to(NotificationMethodRepositoryImpl.class).in(
        Singleton.class);

    // Bind domain services
    bind(IdentityServiceClient.class).in(Singleton.class);
    bind(ResourceVerificationService.class).annotatedWith(Names.named(Services.COMPUTE_SERVICE))
        .to(ComputeResourceVerificationService.class)
        .in(Singleton.class);
    bind(ResourceVerificationService.class).annotatedWith(
        Names.named(Services.OBJECT_STORE_SERVICE))
        .to(ObjectStoreResourceVerificationService.class)
        .in(Singleton.class);
  }
}
