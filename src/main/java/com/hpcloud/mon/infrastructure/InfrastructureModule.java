package com.hpcloud.mon.infrastructure;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;
import com.hpcloud.mon.domain.service.ResourceVerificationService;
import com.hpcloud.mon.infrastructure.compute.ComputeResourceVerificationService;
import com.hpcloud.mon.infrastructure.identity.IdentityServiceClient;
import com.hpcloud.mon.infrastructure.objectstore.ObjectStoreResourceVerificationService;
import com.hpcloud.mon.infrastructure.persistence.AlarmRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.NotificationMethodRepositoryImpl;

/**
 * Infrastructure layer bindings.
 * 
 * @author Jonathan Halterman
 */
public class InfrastructureModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(NotificationMethodRepository.class).to(NotificationMethodRepositoryImpl.class).in(
        Singleton.class);
    bind(AlarmRepository.class).to(AlarmRepositoryImpl.class).in(Singleton.class);
    bind(IdentityServiceClient.class).in(Singleton.class);
    bind(ResourceVerificationService.class).annotatedWith(Names.named(Namespaces.COMPUTE_NAMESPACE))
        .to(ComputeResourceVerificationService.class)
        .in(Singleton.class);
    bind(ResourceVerificationService.class).annotatedWith(
        Names.named(Namespaces.OBJECT_STORE_NAMESPACE))
        .to(ObjectStoreResourceVerificationService.class)
        .in(Singleton.class);
  }
}
