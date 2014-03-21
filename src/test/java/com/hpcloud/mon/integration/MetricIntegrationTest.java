package com.hpcloud.mon.integration;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import kafka.javaapi.producer.Producer;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.MonApiModule;
import com.hpcloud.mon.app.MetricService;
import com.hpcloud.mon.app.command.CreateMetricCommand;
import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;
import com.hpcloud.mon.domain.model.metric.MetricRepository;
import com.hpcloud.mon.resource.AbstractMonApiResourceTest;
import com.hpcloud.mon.resource.MetricResource;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @author Todd Walk
 */
@Test(groups = "integration", enabled = false)
public class MetricIntegrationTest extends AbstractMonApiResourceTest {
  private static final String TENANT_ID = "metric-test";
  private DBI db;
  private MetricService service;
  private Producer<String, String> producer;
  private MonApiConfiguration config;
  private MetricRepository metricRepo;
  private MeasurementRepository measurementRepo;
  private Map<String, String> dimensions;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();
    Handle handle = db.open();
    handle.execute("truncate table access");
    db.close(handle);
    metricRepo = mock(MetricRepository.class);
    measurementRepo = mock(MeasurementRepository.class);
    service = new MetricService(config, producer, metricRegistry);
    addResources(new MetricResource(service, metricRepo, measurementRepo));
  }

  @BeforeTest
  protected void beforeTest() throws Exception {
    config = getConfiguration("config-test.yml", MonApiConfiguration.class);
    Injector injector = Guice.createInjector(new MonApiModule(environment, config));
    producer = injector.getInstance(Key.get(new TypeLiteral<Producer<String, String>>() {
    }));
  }

  @AfterTest
  protected void afterTest() throws Exception {
    producer.close();
  }

  public void shouldCreate() throws Exception {
    dimensions = new HashMap<String, String>();
    dimensions.put("instance_id", "937");
    dimensions.put("az", "2");
    dimensions.put("instance_uuid", "abc123");
    long timestamp = System.currentTimeMillis() / 1000;
    ClientResponse response = client().resource("/v2.0/metrics")
        .header("X-Tenant-Id", TENANT_ID)
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class,
            new CreateMetricCommand("test_namespace", dimensions, timestamp, 22.0));

    assertEquals(response.getStatus(), 204);
  }

  public void shouldCreateList() throws Exception {
    dimensions = new HashMap<String, String>();
    dimensions.put("instance_id", "937");
    dimensions.put("az", "2");
    dimensions.put("instance_uuid", "abc123");
    long timestamp = System.currentTimeMillis() / 1000;
    double timestampD = (double) timestamp;
    double[][] timeValues = { { timestampD, 22.0 }, { timestampD + 1, 23.0 } };
    ClientResponse response = client().resource("/v2.0/metrics")
        .header("X-Tenant-Id", TENANT_ID)
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class,
            new CreateMetricCommand("test_namespace", dimensions, timestamp, timeValues));

    assertEquals(response.getStatus(), 204);
  }
}
