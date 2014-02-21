package com.hpcloud.mon.infrastructure.compute;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.concurrent.Executors;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.hpcloud.http.rest.ResourceNotFoundException;
import com.hpcloud.mon.MonApiConfiguration.CloudServiceConfiguration;
import com.hpcloud.mon.infrastructure.identity.IdentityServiceClient;
import com.hpcloud.mon.infrastructure.identity.IdentityServiceConfiguration;
import com.sun.jersey.api.client.Client;

/**
 * @author Jonathan Halterman
 */
@Test(groups = "integration")
public class ComputeResourceVerificationServiceIntegrationTest {
  private Client client;
  private IdentityServiceConfiguration identityConfig;
  private IdentityServiceClient identityServiceClient;

  @BeforeClass
  protected void beforeClass() throws Exception {
    identityConfig = new IdentityServiceConfiguration();
    identityConfig.url = System.getenv("IDENTITY_URL");
    identityConfig.username = System.getenv("USERNAME");
    identityConfig.password = System.getenv("PASSWORD");
    identityConfig.tenantId = System.getenv("TENANT_ID");
    Client client = new Client();
    client.setExecutorService(Executors.newSingleThreadExecutor());
    identityServiceClient = new IdentityServiceClient(identityConfig, client);

    this.client = new Client();
    this.client.setExecutorService(Executors.newSingleThreadExecutor());
  }

  private ComputeResourceVerificationService serviceFor(String serviceVersion) {
    CloudServiceConfiguration config = new CloudServiceConfiguration();
    config.version = serviceVersion;
    if ("1.1".equals(serviceVersion))
      config.urlFormat = "https://az-%s.region-a.geo-1.compute.hpcloudsvc.com/v1.1";
    else if ("2".equals(serviceVersion))
      config.urlFormat = "https://region-b.geo-1.compute.hpcloudsvc.com/v2";
    else
      fail("Invalid service version " + serviceVersion);
    return new ComputeResourceVerificationService(config, identityConfig, identityServiceClient,
        client);
  }

  /**
   * Tests resource ownership verification in US West, which is at API version 1.1.
   */
  public void shouldVerifyOwnerOfResource() {
    ComputeResourceVerificationService service = serviceFor("1.1");
    assertTrue(service.isVerifiedOwner("46995959297574", "d912fa79-77c1-40e0-8794-53829909d7b6",
        "1", null));
  }

  @Test(expectedExceptions = ResourceNotFoundException.class)
  public void shouldRejectNonExistentResources() {
    ComputeResourceVerificationService service = serviceFor("1.1");
    service.isVerifiedOwner("46995959297574", "d912fa79-77c1-40e0-8794-538299091234", "2", null);
  }

  // Wrong instance id
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void shouldRejectMismatchedServiceIds() {
    ComputeResourceVerificationService service = serviceFor("1.1");
    service.isVerifiedOwner("46995959297574", "d912fa79-77c1-40e0-8794-53829909d7b6", "1", null);
  }

  // Wrong tenant
  public void shouldRejectNonOwner() {
    ComputeResourceVerificationService service = serviceFor("1.1");
    assertFalse(service.isVerifiedOwner("78129159605336", "d912fa79-77c1-40e0-8794-53829909d7b6",
        "1", null));
  }

  /**
   * Tests resource ownership verification in US East, which is at API version 2.
   */
  public void shouldVerifyOwnerOfResourceInBravo() {
    ComputeResourceVerificationService service = serviceFor("2");
    assertTrue(service.isVerifiedOwner("46995959297574", "d770e4cc-10db-4717-a36d-29c10d4a0b5e",
        null, null));
  }

  public void shouldRejectNonOwnerInBravo() {
    ComputeResourceVerificationService service = serviceFor("2");
    assertFalse(service.isVerifiedOwner("78129159605336", "d770e4cc-10db-4717-a36d-29c10d4a0b5e",
        null, null));
  }
}
