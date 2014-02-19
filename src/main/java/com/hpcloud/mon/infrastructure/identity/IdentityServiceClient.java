package com.hpcloud.mon.infrastructure.identity;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpcloud.http.rest.AbstractRestClient;
import com.hpcloud.http.rest.UnauthorizedException;
import com.hpcloud.util.Exceptions;
import com.hpcloud.util.retry.RetryLoop;
import com.hpcloud.util.retry.RetryLoops;
import com.sun.jersey.api.client.Client;

/**
 * An identity service client.
 * 
 * @author Jonathan Halterman
 */
public class IdentityServiceClient extends AbstractRestClient {
  private static final Logger LOG = LoggerFactory.getLogger(IdentityServiceClient.class);
  private static final String AUTH_STRING_FORMAT = "{\"auth\": {\"passwordCredentials\": {\"username\": \"%s\",\"password\": \"%s\"},\"tenantId\":\"%s\"}}";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final IdentityServiceConfiguration config;
  private volatile String guardedAuthToken;

  public interface TokenCallable<T> {
    T call(String authToken);
  }

  @Inject
  public IdentityServiceClient(IdentityServiceConfiguration config, Client client) {
    super("identity", client);
    this.config = config;
  }

  /**
   * Gets an auth token for the identity service's configuration.
   */
  public String getAuthToken() {
    if (guardedAuthToken == null)
      guardedAuthToken = requestAuthToken();
    return guardedAuthToken;
  }

  /**
   * Invalidates the auth token. Should be called when an indication that the auth token has expired
   * is recognized, such as an UnauthorizedException.
   */
  public void invalidateAuthToken() {
    guardedAuthToken = null;
  }

  /**
   * Calls the {@code callable} providing an authToken, retrying the {@code callable} one time if an
   * UnauthorizedException is thrown to handle token expiration.
   */
  public <T> T withToken(TokenCallable<T> callable) {
    RetryLoop loop = RetryLoops.nRetries(1);
    while (loop.shouldContinue()) {
      try {
        return callable.call(getAuthToken());
      } catch (UnauthorizedException e) {
        LOG.debug("Invalid token detected. Invalidating.");
        invalidateAuthToken();
        loop.recordFailure(e);
      }
    }

    return null;
  }

  String requestAuthToken() {
    String authRequestBody = String.format(AUTH_STRING_FORMAT, config.username, config.password,
        config.tenantId);
    LOG.debug("Obtaining auth token for {}", config.username);
    String serviceCatalog = createResource(config.url, String.class, authRequestBody, null);

    try {
      return OBJECT_MAPPER.readTree(serviceCatalog).get("access").get("token").get("id").asText();
    } catch (Exception e) {
      throw Exceptions.uncheck(e, "Failed to parse auth token from service catalog");
    }
  }
}
