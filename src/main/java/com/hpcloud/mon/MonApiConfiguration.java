package com.hpcloud.mon;

import io.dropwizard.Configuration;
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
  @NotEmpty public String metricsTopic = "metrics";
  @NotEmpty public String eventsTopic = "events";
  @NotEmpty public String alarmStateTransitionsTopic = "alarm-state-transitions";

  @Valid @NotNull public DataSourceFactory mysql;
  @Valid @NotNull public DataSourceFactory vertica;
  @Valid @NotNull public KafkaConfiguration kafka;
  @Valid @NotNull public MiddlewareConfiguration middleware;
}
