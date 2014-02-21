package com.hpcloud.mon.app;

import java.util.ArrayList;
import java.util.List;

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
 * @author Jonathan Halterman
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

  public void create(List<Metric> metrics, String tenantId, @Nullable String crossTenantId,
      String authToken) {
    Builder<String, Object> metaBuilder = new ImmutableMap.Builder<String, Object>().put(
        "tenantId", tenantId).put("authToken", authToken);
    if (crossTenantId != null)
      metaBuilder.put("crossTenantId", crossTenantId);
    ImmutableMap<String, Object> meta = metaBuilder.build();

    List<KeyedMessage<String, String>> keyedMessages = new ArrayList<>(metrics.size());
    for (Metric metric : metrics) {
      MetricEnvelope envelope = new MetricEnvelope(metric, meta);
      keyedMessages.add(new KeyedMessage<>(config.metricsTopic, tenantId,
          MetricEnvelopes.toJson(envelope)));
    }

    producer.send(keyedMessages);
    metricMeter.mark();
  }
}
