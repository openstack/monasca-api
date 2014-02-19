package com.hpcloud.mon.app;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.hash.HashCode;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.common.model.metric.Metric;
import com.hpcloud.mon.common.model.metric.Metrics;

/**
 * Metric service implementation.
 * 
 * @author Todd Walk
 */
public class MetricService {
  private Map<HashCode, String> metricTypeCache = new ConcurrentHashMap<HashCode, String>();
  private AtomicInteger cacheMonth = new AtomicInteger(-1); // Must empty out the metricTypeCache
                                                            // each month
  private final RabbitMQService rabbitService;
  private final MetricRepository repo;
  private final String exchange;
  private final Meter metricMeter;

  @Inject
  public MetricService(MonApiConfiguration config,
      @Named("internal") RabbitMQService rabbitService, MetricRepository repo,
      MetricRegistry metricRegistry) {
    exchange = config.maasMetricsExchange;
    this.rabbitService = rabbitService;
    this.repo = repo;
    metricMeter = metricRegistry.meter("maas metrics published");
    cacheMonth = new AtomicInteger(Calendar.getInstance().get(Calendar.MONTH));
  }

  public void create(Metric metric, String tenantId, @Nullable String crossTenantId,
      String authToken) {
    // Send metric
    String event = Metrics.toJson(metric);
    Builder<String, Object> builder = new ImmutableMap.Builder<String, Object>().put("tenantId",
        tenantId).put("authToken", authToken);
    if (crossTenantId != null)
      builder.put("crossTenantId", crossTenantId);
    rabbitService.sendUTF8(exchange, tenantId, event, builder.build());

    // Add metric stats
    int month = Calendar.getInstance().get(Calendar.MONTH);
    int prevMonth = month > 0 ? month - 1 : 11;
    if (cacheMonth.compareAndSet(prevMonth, month))
      metricTypeCache.clear();
    HashCode hash = metric.definition().toHashCode();
    if (!metricTypeCache.containsKey(hash)) {
      metricTypeCache.put(hash, tenantId);
      repo.persist(tenantId, hash);
    }

    metricMeter.mark();
  }
}
