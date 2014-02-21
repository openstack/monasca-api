package com.hpcloud.mon.infrastructure.objectstore;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.concurrent.Executors;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.hpcloud.http.rest.ResourceNotFoundException;
import com.hpcloud.http.rest.RestClientException;
import com.hpcloud.mon.MonApiConfiguration.CloudServiceConfiguration;
import com.sun.jersey.api.client.Client;

/**
 * @author Jonathan Halterman
 */
@Test(groups = "integration")
public class ObjectStoreResourceVerificationServiceIntegrationTest {
  private Client client;
  private String authToken = System.getenv("AUTH_TOKEN");

  @BeforeClass
  protected void beforeClass() throws Exception {
    Client client = new Client();
    client.setExecutorService(Executors.newSingleThreadExecutor());

    this.client = new Client();
    this.client.setExecutorService(Executors.newSingleThreadExecutor());
  }

  private ObjectStoreResourceVerificationService serviceFor(String serviceVersion) {
    CloudServiceConfiguration config = new CloudServiceConfiguration();
    config.version = serviceVersion;
    if ("1.0".equals(serviceVersion))
      config.urlFormat = "https://region-a.geo-1.objects.hpcloudsvc.com/v1.0";
    else if ("1".equals(serviceVersion))
      config.urlFormat = "https://region-a.geo-1.objects.hpcloudsvc.com/v1";
    else
      fail("Invalid service version " + serviceVersion);
    return new ObjectStoreResourceVerificationService(config, client);
  }

  /**
   * Tests resource ownership verification in US West, which is at API version 1.1.
   */
  public void shouldVerifyOwnerOfResource() {
    ObjectStoreResourceVerificationService service = serviceFor("1.0");
    assertTrue(service.isVerifiedOwner("46995959297574", "misc", null, authToken));
  }

  @Test(expectedExceptions = ResourceNotFoundException.class)
  public void shouldRejectNonExistentResources() {
    ObjectStoreResourceVerificationService service = serviceFor("1.0");
    service.isVerifiedOwner("46995959297574", "foobarbaz", null, authToken);
  }

  // Wrong tenant
  @Test(expectedExceptions = RestClientException.class)
  public void shouldRejectNonOwner() {
    ObjectStoreResourceVerificationService service = serviceFor("1.0");
    assertFalse(service.isVerifiedOwner("78129159605336", "misc", null, authToken));
  }

  /**
   * Tests resource ownership verification in US East, which is at API version 1.
   */
  public void shouldVerifyOwnerOfResourceInBravo() {
    ObjectStoreResourceVerificationService service = serviceFor("1");
    assertTrue(service.isVerifiedOwner("46995959297574", "misc", null, authToken));
  }

  @Test(expectedExceptions = RestClientException.class)
  public void shouldRejectNonOwnerInBravo() {
    ObjectStoreResourceVerificationService service = serviceFor("1");
    assertFalse(service.isVerifiedOwner("78129159605336", "misc", null, authToken));
  }
}
