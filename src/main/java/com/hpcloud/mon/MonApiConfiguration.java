package com.hpcloud.mon;

import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.hpcloud.messaging.kafka.KafkaConfiguration;
import com.hpcloud.mon.infrastructure.identity.IdentityServiceConfiguration;
import com.hpcloud.mon.infrastructure.middleware.MiddlewareConfiguration;

/**
 * @author Jonathan Halterman
 */
public class MonApiConfiguration extends Configuration {
  @Valid @NotEmpty public Map<String, CloudServiceConfiguration> cloudServices = new HashMap<String, CloudServiceConfiguration>();
  @Valid @NotNull public IdentityServiceConfiguration identityService;
  @NotNull public Boolean accessedViaHttps;
  @NotNull public Boolean useMiddleware;

  @NotEmpty public String metricsTopic;
  @NotEmpty public String eventsTopic;

  @NotEmpty public String[] adminUsers;
  @NotEmpty public String externalHost;
  public String apiCallCountPersistDelta;
  @Valid @NotNull public DataSourceFactory database;
  @Valid @NotNull public KafkaConfiguration kafka;
  @Valid @NotNull public MiddlewareConfiguration middleware;
  @Valid @NotNull public JerseyClientConfiguration jerseyClient;
  @Valid @NotNull public AddressValidationProxyConfiguration addressValidation;

  public static class CloudServiceConfiguration {
    @NotEmpty public String version;
    @NotEmpty public String urlFormat;
    @Min(1) @Max(65535) public int port = 80;
  }

  public static class AddressValidationProxyConfiguration {
    @NotEmpty public String url;
  }
}
