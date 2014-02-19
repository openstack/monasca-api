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

import com.hpcloud.mon.infrastructure.identity.IdentityServiceConfiguration;
import com.hpcloud.mon.infrastructure.middleware.MiddlewareConfiguration;
import com.hpcloud.mon.infrastructure.zookeeper.ZookeeperConfiguration;

/**
 * @author Jonathan Halterman
 */
public class MonApiConfiguration extends Configuration {
  @Valid @NotEmpty public Map<String, CloudServiceConfiguration> cloudServices = new HashMap<String, CloudServiceConfiguration>();
  @Valid @NotNull public IdentityServiceConfiguration identityService;
  @NotNull public Boolean accessedViaHttps;
  @NotNull public Boolean useMiddleware;
  @NotEmpty public String controlExchange;
  @NotEmpty public String controlEventRoutingKey;
  @NotEmpty public String maasMetricsExchange;
  @NotEmpty public String[] adminUsers;
  @NotEmpty public String externalHost;
  public String apiCallCountPersistDelta;
  @Valid @NotNull public ZookeeperConfiguration zookeeper;
  @Valid @NotNull public DataSourceFactory database = new DataSourceFactory();
  // @Valid @NotNull public RabbitMQConfiguration internalRabbit = new RabbitMQConfiguration();
  // @Valid @NotNull public RabbitMQConfiguration externalRabbit = new RabbitMQConfiguration();
  @Valid @NotNull public MiddlewareConfiguration middleware;
  @Valid @NotNull public JerseyClientConfiguration jerseyClient = new JerseyClientConfiguration();
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
