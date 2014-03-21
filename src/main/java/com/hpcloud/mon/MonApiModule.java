package com.hpcloud.mon;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;

import java.util.Properties;

import javax.inject.Singleton;

import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;

import org.skife.jdbi.v2.DBI;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Joiner;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.hpcloud.mon.app.ApplicationModule;
import com.hpcloud.mon.domain.DomainModule;
import com.hpcloud.mon.infrastructure.InfrastructureModule;

/**
 * Monitoring API server bindings.
 * 
 * @author Jonathan Halterman
 */
public class MonApiModule extends AbstractModule {
  private final MonApiConfiguration config;
  private final Environment environment;

  public MonApiModule(Environment environment, MonApiConfiguration config) {
    this.environment = environment;
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(MonApiConfiguration.class).toInstance(config);
    bind(MetricRegistry.class).in(Singleton.class);
    bind(DataSourceFactory.class).toInstance(config.database);

    install(new ApplicationModule());
    install(new DomainModule());
    install(new InfrastructureModule());
  }

  @Provides
  @Singleton
  public DBI getDBI() {
    try {
      return new DBIFactory().build(environment, config.database, "mysql");
    } catch (ClassNotFoundException e) {
      throw new ProvisionException("Failed to provision DBI", e);
    }
  }

  @Provides
  @Singleton
  public Producer<String, String> getProducer() {
    Properties props = new Properties();
    props.put("metadata.broker.list", Joiner.on(',').join(config.kafka.brokerUris));
    props.put("serializer.class", "kafka.serializer.StringEncoder");
    props.put("request.required.acks", "1");
    ProducerConfig config = new ProducerConfig(props);
    return new Producer<String, String>(config);
  }
}
