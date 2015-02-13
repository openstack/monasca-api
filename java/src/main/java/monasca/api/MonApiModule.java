/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
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
package monasca.api;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;

import java.util.Properties;

import javax.inject.Named;
import javax.inject.Singleton;

import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;

import org.skife.jdbi.v2.DBI;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Joiner;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
import monasca.api.app.ApplicationModule;
import monasca.api.domain.DomainModule;
import monasca.api.infrastructure.InfrastructureModule;

/**
 * Monitoring API server bindings.
 */
public class MonApiModule extends AbstractModule {
  private final ApiConfig config;
  private final Environment environment;

  public MonApiModule(Environment environment, ApiConfig config) {
    this.environment = environment;
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(ApiConfig.class).toInstance(config);
    bind(MetricRegistry.class).toInstance(environment.metrics());
    bind(DataSourceFactory.class).annotatedWith(Names.named("mysql")).toInstance(config.mysql);
    bind(DataSourceFactory.class).annotatedWith(Names.named("vertica")).toInstance(config.vertica);

    install(new ApplicationModule());
    install(new DomainModule());
    install(new InfrastructureModule(this.config));
  }

  @Provides
  @Singleton
  @Named("mysql")
  public DBI getMySqlDBI() {
    try {
      return new DBIFactory().build(environment, config.mysql, "mysql");
    } catch (ClassNotFoundException e) {
      throw new ProvisionException("Failed to provision MySQL DBI", e);
    }
  }

  @Provides
  @Singleton
  @Named("vertica")
  public DBI getVerticaDBI() {
    try {
      return new DBIFactory().build(environment, config.vertica, "vertica");
    } catch (ClassNotFoundException e) {
      throw new ProvisionException("Failed to provision Vertica DBI", e);
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
