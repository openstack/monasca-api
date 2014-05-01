/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

  public void create(List<Metric> metrics, String tenantId, @Nullable String crossTenantId) {
    Builder<String, Object> metaBuilder = new ImmutableMap.Builder<String, Object>().put(
        "tenantId", tenantId);
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
