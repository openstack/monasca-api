package com.hpcloud.mon.app;

import javax.annotation.Nullable;
import javax.inject.Inject;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.common.model.metric.Metric;
import com.hpcloud.mon.common.model.metric.MetricEnvelope;
import com.hpcloud.mon.common.model.metric.MetricEnvelopes;

/**
 * Metric service implementation.
 * 
 * @author Todd Walk
 */
public class MetricService {
  private final MonApiConfiguration config;
  private final Producer<String, String> producer;
  private final Meter metricMeter;

  @Inject
  public MetricService(MonApiConfiguration config, Producer<String, String> producer,
      MetricRegistry metricRegistry) {
    this.config = config;
    this.producer = producer;
    metricMeter = metricRegistry.meter(MetricRegistry.name(MetricService.class, "metrics.published"));
  }

  public void create(Metric metric, String tenantId, @Nullable String crossTenantId,
      String authToken) {
    Builder<String, Object> meta = new ImmutableMap.Builder<String, Object>().put("tenantId",
        tenantId).put("authToken", authToken);
    if (crossTenantId != null)
      meta.put("crossTenantId", crossTenantId);
    MetricEnvelope envelope = new MetricEnvelope(metric, meta.build());
    KeyedMessage<String, String> keyedMessage = new KeyedMessage<>(config.metricsTopic, tenantId,
        MetricEnvelopes.toJson(envelope));
    producer.send(keyedMessage);
    metricMeter.mark();
  }
}
