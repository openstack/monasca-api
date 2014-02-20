package com.hpcloud.mon.infrastructure;

import com.google.inject.AbstractModule;
import com.hpcloud.mon.MonApiConfiguration;

/**
 * Infrastructure layer bindings.
 * 
 * @author Jonathan Halterman
 */
public class InfrastructureModule extends AbstractModule {
  private final MonApiConfiguration config;

  public InfrastructureModule(MonApiConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    // bind(AccessRepository.class).to(AccessRepositoryImpl.class).in(Scopes.SINGLETON);
    // bind(DatapointRepository.class).to(SomDatapointRepositoryImpl.class).in(Scopes.SINGLETON);
    // bind(EndpointRepository.class).to(EndpointRepositoryImpl.class).in(Scopes.SINGLETON);
    // bind(SubscriptionRepository.class).to(SubscriptionRepositoryImpl.class).in(Scopes.SINGLETON);
    // bind(NotificationMethodRepository.class).to(NotificationMethodRepositoryImpl.class).in(
    // Scopes.SINGLETON);
    // bind(AlarmRepository.class).to(AlarmRepositoryImpl.class).in(Scopes.SINGLETON);
    // bind(IdentityServiceClient.class).in(Scopes.SINGLETON);
    // bind(ResourceVerificationService.class).annotatedWith(Names.named(Namespaces.COMPUTE_NAMESPACE))
    // .to(ComputeResourceVerificationService.class)
    // .in(Scopes.SINGLETON);
    // bind(ResourceVerificationService.class).annotatedWith(
    // Names.named(Namespaces.OBJECT_STORE_NAMESPACE))
    // .to(ObjectStoreResourceVerificationService.class)
    // .in(Scopes.SINGLETON);
  }
}
