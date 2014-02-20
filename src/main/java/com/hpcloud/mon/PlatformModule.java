package com.hpcloud.mon;

import io.dropwizard.client.JerseyClientBuilder;
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
import com.google.inject.Scopes;
import com.sun.jersey.api.client.Client;

/**
 * Platform (non-application) specific bindings.
 * 
 * @author Jonathan Halterman
 */
public class PlatformModule extends AbstractModule {
  private final MonApiConfiguration config;
  private final Environment environment;

  public PlatformModule(Environment environment, MonApiConfiguration config) {
    this.environment = environment;
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(MetricRegistry.class).in(Scopes.SINGLETON);
    bind(DataSourceFactory.class).toInstance(config.database);
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
  public Client getClient() {
    return new JerseyClientBuilder(environment).using(config.jerseyClient)
        .using(environment)
        .build("default");
  }

  @Provides
  @Singleton
  public Producer<?, ?> getProducer() {
    Properties props = new Properties();
    props.put("metadata.broker.list", Joiner.on(',').join(config.kafka.hosts));
    props.put("serializer.class", "kafka.serializer.StringEncoder");
    props.put("partitioner.class", "example.producer.SimplePartitioner");
    props.put("request.required.acks", "1");
    ProducerConfig config = new ProducerConfig(props);
    return new Producer<String, String>(config);
  }
}
