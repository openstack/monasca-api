package com.hpcloud.mon;

import io.dropwizard.Application;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterRegistration.Dynamic;
import javax.ws.rs.ext.ExceptionMapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.hp.csbu.cc.middleware.TokenAuth;
import com.hpcloud.mon.infrastructure.identity.IdentityServiceClient;
import com.hpcloud.mon.infrastructure.servlet.AddressValidationProxyServlet;
import com.hpcloud.mon.infrastructure.servlet.PostAuthenticationFilter;
import com.hpcloud.mon.infrastructure.servlet.PreAuthenticationFilter;
import com.hpcloud.mon.infrastructure.servlet.TenantCallCountingFilter;
import com.hpcloud.mon.infrastructure.servlet.TenantValidationFilter;
import com.hpcloud.mon.resource.VersionResource;
import com.hpcloud.mon.resource.exception.EntityExistsExceptionMapper;
import com.hpcloud.mon.resource.exception.EntityNotFoundExceptionMapper;
import com.hpcloud.mon.resource.exception.IllegalArgumentExceptionMapper;
import com.hpcloud.mon.resource.exception.InvalidEntityExceptionMapper;
import com.hpcloud.mon.resource.exception.JsonMappingExceptionManager;
import com.hpcloud.mon.resource.exception.JsonProcessingExceptionMapper;
import com.hpcloud.mon.resource.exception.ResourceNotFoundExceptionMapper;
import com.hpcloud.mon.resource.exception.ThrowableExceptionMapper;
import com.hpcloud.util.Injector;

/**
 * Monitoring API application.
 * 
 * @author Jonathan Halterman
 */
public class MonApiApplication extends Application<MonApiConfiguration> {
  public static void main(String[] args) throws Exception {
    new MonApiApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<MonApiConfiguration> bootstrap) {
    /** Configure bundles */
    bootstrap.addBundle(new DBIExceptionsBundle());
  }

  @Override
  public String getName() {
    return "monitoring";
  }

  @Override
  public void run(MonApiConfiguration config, Environment environment) throws Exception {
    /** Wire services */
    Injector.registerModules(new PlatformModule(environment, config), new ApplicationModule(config));

    /** Configure managed services */

    /** Configure resources */
    environment.jersey().register(Injector.getInstance(VersionResource.class));

    /** Configure providers */
    removeExceptionMappers(environment.jersey().getResourceConfig().getSingletons());
    environment.jersey().register(new EntityExistsExceptionMapper());
    environment.jersey().register(new EntityNotFoundExceptionMapper());
    environment.jersey().register(new ResourceNotFoundExceptionMapper());
    environment.jersey().register(new IllegalArgumentExceptionMapper());
    environment.jersey().register(new InvalidEntityExceptionMapper());
    environment.jersey().register(new JsonProcessingExceptionMapper());
    environment.jersey().register(new JsonMappingExceptionManager());
    environment.jersey().register(new ThrowableExceptionMapper<Throwable>() {
    });

    /** Configure Jackson */
    environment.getObjectMapper().setPropertyNamingStrategy(
        PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    environment.getObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    /** Configure health checks */
    // environment.addHealthCheck(new RabbitMQHealthCheck("internal", config.internalRabbit));
    // environment.addHealthCheck(new RabbitMQHealthCheck("external", config.externalRabbit));
    // environment.addHealthCheck(new RabbitMQAPIHealthCheck(
    // Injector.getInstance(RabbitMQAdminService.class)));

    /** Configure servlet filters */
    if (config.useMiddleware) {
      Map<String, String> authInitParams = new HashMap<String, String>();
      authInitParams.put("ServiceIds", config.middleware.serviceIds);
      authInitParams.put("EndpointIds", config.middleware.endpointIds);
      authInitParams.put("ServerVIP", config.middleware.serverVIP);
      authInitParams.put("ServerPort", config.middleware.serverPort);
      authInitParams.put("ConnTimeout", config.middleware.connTimeout);
      authInitParams.put("ConnSSLClientAuth", config.middleware.connSSLClientAuth);
      authInitParams.put("Keystore", config.middleware.keystore);
      authInitParams.put("KeystorePass", config.middleware.keystorePass);
      authInitParams.put("Truststore", config.middleware.truststore);
      authInitParams.put("TruststorePass", config.middleware.truststorePass);
      authInitParams.put("ConnPoolMaxActive", config.middleware.connPoolMaxActive);
      authInitParams.put("ConnPoolMaxIdle", config.middleware.connPoolMaxActive);
      authInitParams.put("ConnPoolEvictPeriod", config.middleware.connPoolEvictPeriod);
      authInitParams.put("ConnPoolMinIdleTime", config.middleware.connPoolMinIdleTime);
      authInitParams.put("ConnRetryTimes", config.middleware.connRetryTimes);
      authInitParams.put("ConnRetryInterval", config.middleware.connRetryInterval);

      /** Setup servlet filters */
      environment.servlets()
          .addFilter("pre-auth", new PreAuthenticationFilter())
          .addMappingForUrlPatterns(null, true, "/*");
      Dynamic filter = environment.servlets().addFilter("token-auth", new TokenAuth());
      filter.addMappingForUrlPatterns(null, true, "/*");
      filter.setInitParameters(authInitParams);
      environment.servlets()
          .addFilter("post-auth", new PostAuthenticationFilter(config.middleware.rolesToMatch))
          .addMappingForUrlPatterns(null, true, "/*");
      environment.servlets()
          .addFilter("tenant-validation", new TenantValidationFilter())
          .addMappingForUrlPatterns(null, true, "/*");
      environment.servlets()
          .addFilter("call-counting", new TenantCallCountingFilter())
          .addMappingForUrlPatterns(null, true, "/*");
    }

    /** Initialize the identity service */
    Injector.getInstance(IdentityServiceClient.class).getAuthToken();

    /** Configure proxy to address validation service */
    environment.servlets()
        .addServlet("address-validation",
            new AddressValidationProxyServlet(config.addressValidation.url))
        .addMapping("/v1.1/notification-addresses/*");
  }

  private void removeExceptionMappers(Set<Object> items) {
    for (Iterator<Object> i = items.iterator(); i.hasNext();) {
      Object o = i.next();
      if (o instanceof ExceptionMapper)
        i.remove();
    }
  }
}
