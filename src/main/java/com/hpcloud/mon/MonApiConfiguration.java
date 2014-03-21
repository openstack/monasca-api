package com.hpcloud.mon;

import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.hpcloud.messaging.kafka.KafkaConfiguration;
import com.hpcloud.mon.infrastructure.middleware.MiddlewareConfiguration;

/**
 * @author Jonathan Halterman
 */
public class MonApiConfiguration extends Configuration {
  @NotNull public Boolean accessedViaHttps;

  @NotEmpty public String metricsTopic;
  @NotEmpty public String eventsTopic;

  @Valid @NotNull public DataSourceFactory database;
  @Valid @NotNull public KafkaConfiguration kafka;
  @Valid @NotNull public MiddlewareConfiguration middleware;
  @Valid @NotNull public JerseyClientConfiguration jerseyClient;
}
