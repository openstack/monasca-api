package com.hpcloud.mon.infrastructure.compute;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hpcloud.http.rest.AbstractRestClient;
import com.hpcloud.mon.MonApiConfiguration.CloudServiceConfiguration;
import com.hpcloud.mon.common.model.Namespaces;
import com.hpcloud.mon.domain.service.ResourceVerificationService;
import com.hpcloud.mon.infrastructure.identity.IdentityServiceClient;
import com.hpcloud.mon.infrastructure.identity.IdentityServiceClient.TokenCallable;
import com.hpcloud.mon.infrastructure.identity.IdentityServiceConfiguration;
import com.hpcloud.util.Exceptions;
import com.sun.jersey.api.client.Client;

/**
 * Performs Compute resource verification.
 * 
 * @author Jonathan Halterman
 */
public class ComputeResourceVerificationService extends AbstractRestClient implements
    ResourceVerificationService {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final CloudServiceConfiguration config;
  private final IdentityServiceConfiguration identityConfig;
  private final IdentityServiceClient identityServiceClient;

  @Inject
  public ComputeResourceVerificationService(
      @Named(Namespaces.COMPUTE_NAMESPACE) CloudServiceConfiguration config,
      IdentityServiceConfiguration identityConfig, IdentityServiceClient identityServiceClient,
      Client client) {
    super(Namespaces.COMPUTE_NAMESPACE, client);
    this.config = config;
    this.identityConfig = identityConfig;
    this.identityServiceClient = identityServiceClient;
  }

  @Override
  public boolean isVerifiedOwner(String tenantId, final String instanceId, String az, String unused) {
    Preconditions.checkNotNull(tenantId, "tenantId");
    Preconditions.checkNotNull(instanceId, "instanceId");

    String baseUri = String.format(config.urlFormat, az);
    final String uri = String.format("%s/%s/servers/%s", baseUri, identityConfig.tenantId,
        instanceId);
    String instance = identityServiceClient.withToken(new TokenCallable<String>() {
      @Override
      public String call(String authToken) {
        return getResource(uri, String.class, instanceId,
            Collections.singletonMap("X-Auth-Token", authToken));
      }
    });

    String serverId = null;
    String serverTenantId = null;

    try {
      JsonNode serverNode = OBJECT_MAPPER.readTree(instance).get("server");
      serverId = serverNode.get("id").asText();
      serverTenantId = serverNode.get("tenant_id").asText();
    } catch (Exception e) {
      throw Exceptions.uncheck(e, "Failed to parse compute resource verification response");
    }

    if (!serverId.endsWith(instanceId))
      throw new IllegalArgumentException(String.format(
          "The referenced resource id %s is invalid for the secondary id %s", instanceId,
          instanceId));
    return tenantId.equals(serverTenantId);
  }
}
