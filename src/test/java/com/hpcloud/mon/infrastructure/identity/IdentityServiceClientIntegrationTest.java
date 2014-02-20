package com.hpcloud.mon.infrastructure.identity;

import static org.testng.Assert.assertNotNull;

import java.util.concurrent.Executors;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.jersey.api.client.Client;

/**
 * @author Jonathan Halterman
 */
@Test(groups = "integration")
public class IdentityServiceClientIntegrationTest {
  private IdentityServiceClient client;

  @BeforeClass
  protected void beforeClass() throws Exception {
    IdentityServiceConfiguration config = new IdentityServiceConfiguration();
    config.url = System.getenv("URL");
    config.username = System.getenv("USERNAME");
    config.password = System.getenv("PASSWORD");
    config.tenantId = System.getenv("TENANT_ID");

    Client client = new Client();
    client.setExecutorService(Executors.newSingleThreadExecutor());
    this.client = new IdentityServiceClient(config, client);
  }

  public void shouldGetAuthToken() {
    assertNotNull(client.getAuthToken());
  }
}
