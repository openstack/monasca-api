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

package monasca.api.integration;

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
import monasca.api.ApiConfig;
import monasca.api.MonApiModule;
import monasca.api.app.MetricService;
import monasca.api.app.command.CreateMetricCommand;
import monasca.api.domain.model.metric.MetricDefinitionRepo;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.api.resource.AbstractMonApiResourceTest;
import monasca.api.resource.MetricResource;
import com.sun.jersey.api.client.ClientResponse;

@Test(groups = "integration", enabled = false)
public class MetricIntegrationTest extends AbstractMonApiResourceTest {
  private static final String TENANT_ID = "metric-test";
  private DBI db;
  private MetricService service;
  private Producer<String, String> producer;
  private ApiConfig config;
  private MetricDefinitionRepo metricRepo;
  private Map<String, String> dimensions;
  private Map<String, String> valueMeta;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();
    Handle handle = db.open();
    handle.execute("truncate table access");
    db.close(handle);
    metricRepo = mock(MetricDefinitionRepo.class);
    service = new MetricService(config, producer, metricRegistry);
    addResources(new MetricResource(config, service, metricRepo, new PersistUtils()));
  }

  @BeforeTest
  protected void beforeTest() throws Exception {
    config = getConfiguration("config-test.yml", ApiConfig.class);
    Injector injector = Guice.createInjector(new MonApiModule(environment, config));
    producer = injector.getInstance(Key.get(new TypeLiteral<Producer<String, String>>() {}));
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
    valueMeta = new HashMap<String, String>();
    valueMeta.put("rc", "404");
    valueMeta.put("errMsg", "Not Found");
    long timestamp = System.currentTimeMillis();
    ClientResponse response =
        client()
            .resource("/v2.0/metrics")
            .header("X-Tenant-Id", TENANT_ID)
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateMetricCommand("test_namespace", dimensions, timestamp, 22.0, valueMeta));

    assertEquals(response.getStatus(), 204);
  }
}
