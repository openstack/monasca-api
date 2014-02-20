package com.hpcloud.mon.app;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

/**
 * Application layer bindings.
 * 
 * @author Jonathan Halterman
 */
public class ApplicationModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(MetricService.class).in(Singleton.class);
    bind(AlarmService.class).in(Singleton.class);
  }
}
