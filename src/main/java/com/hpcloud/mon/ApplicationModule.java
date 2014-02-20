package com.hpcloud.mon;

import java.util.Map.Entry;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.hpcloud.mon.MonApiConfiguration.CloudServiceConfiguration;
import com.hpcloud.mon.domain.DomainModule;
import com.hpcloud.mon.infrastructure.InfrastructureModule;
import com.hpcloud.mon.infrastructure.identity.IdentityServiceConfiguration;

/**
 * Application specific bindings.
 * 
 * @author Jonathan Halterman
 */
public class ApplicationModule extends AbstractModule {
  private final MonApiConfiguration config;

  public ApplicationModule(MonApiConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(MonApiConfiguration.class).toInstance(config);
    for (Entry<String, CloudServiceConfiguration> cloudServiceConf : config.cloudServices.entrySet())
      bind(CloudServiceConfiguration.class).annotatedWith(Names.named(cloudServiceConf.getKey()))
          .toInstance(cloudServiceConf.getValue());
    bind(IdentityServiceConfiguration.class).toInstance(config.identityService);

    install(new DomainModule());
    install(new InfrastructureModule());
  }
}
