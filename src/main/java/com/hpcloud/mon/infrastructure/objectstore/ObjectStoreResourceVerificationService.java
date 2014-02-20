package com.hpcloud.mon.infrastructure.objectstore;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Preconditions;
import com.hpcloud.http.rest.AbstractRestClient;
import com.hpcloud.mon.MonApiConfiguration.CloudServiceConfiguration;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.domain.service.ResourceVerificationService;
import com.sun.jersey.api.client.Client;

/**
 * Performs Object store resource verification.
 * 
 * @author Jonathan Halterman
 */
public class ObjectStoreResourceVerificationService extends AbstractRestClient implements
    ResourceVerificationService {
  private final CloudServiceConfiguration config;

  @Inject
  public ObjectStoreResourceVerificationService(
      @Named(Namespaces.OBJECT_STORE_NAMESPACE) CloudServiceConfiguration config, Client client) {
    super(Namespaces.OBJECT_STORE_NAMESPACE, client);
    this.config = config;
  }

  @Override
  public boolean isVerifiedOwner(String tenantId, final String containerName, String unused,
      String az, String authToken) {
    Preconditions.checkNotNull(tenantId, "tenantId");
    Preconditions.checkNotNull(containerName, "containerName");

    String uri = String.format("%s/%s/%s", config.urlFormat, tenantId, containerName);
    return resourceExists(uri, Collections.singletonMap("X-Auth-Token", authToken));
  }
}
