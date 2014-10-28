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
package monasca.api;

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

import org.eclipse.jetty.servlets.CrossOriginFilter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import monasca.common.messaging.kafka.KafkaHealthCheck;
import monasca.common.middleware.AuthConstants;
import monasca.common.middleware.TokenAuth;
import monasca.api.bundle.SwaggerBundle;
import monasca.api.infrastructure.servlet.MockAuthenticationFilter;
import monasca.api.infrastructure.servlet.PostAuthenticationFilter;
import monasca.api.infrastructure.servlet.PreAuthenticationFilter;
import monasca.api.infrastructure.servlet.RoleAuthorizationFilter;
import monasca.api.resource.AlarmDefinitionResource;
import monasca.api.resource.AlarmResource;
import monasca.api.resource.MeasurementResource;
import monasca.api.resource.MetricResource;
import monasca.api.resource.NotificationMethodResource;
import monasca.api.resource.StatisticResource;
import monasca.api.resource.VersionResource;
import monasca.api.resource.exception.ConstraintViolationExceptionMapper;
import monasca.api.resource.exception.EntityExistsExceptionMapper;
import monasca.api.resource.exception.EntityNotFoundExceptionMapper;
import monasca.api.resource.exception.IllegalArgumentExceptionMapper;
import monasca.api.resource.exception.InvalidEntityExceptionMapper;
import monasca.api.resource.exception.JsonMappingExceptionManager;
import monasca.api.resource.exception.JsonProcessingExceptionMapper;
import monasca.api.resource.exception.ThrowableExceptionMapper;
import monasca.api.resource.serialization.SubAlarmExpressionSerializer;
import monasca.common.util.Injector;

/**
 * Monitoring API application.
 */
public class MonApiApplication extends Application<MonApiConfiguration> {
  public static void main(String[] args) throws Exception {
    new MonApiApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<MonApiConfiguration> bootstrap) {
    /** Configure bundles */
    bootstrap.addBundle(new DBIExceptionsBundle());
    bootstrap.addBundle(new SwaggerBundle());
  }

  @Override
  public String getName() {
    return "HP Cloud Monitoring";
  }

  @Override
  @SuppressWarnings("unchecked")
  public void run(MonApiConfiguration config, Environment environment) throws Exception {
    /** Wire services */
    Injector.registerModules(new MonApiModule(environment, config));

    /** Configure resources */
    environment.jersey().register(Injector.getInstance(VersionResource.class));
    environment.jersey().register(Injector.getInstance(AlarmDefinitionResource.class));
    environment.jersey().register(Injector.getInstance(AlarmResource.class));
    environment.jersey().register(Injector.getInstance(MetricResource.class));
    environment.jersey().register(Injector.getInstance(MeasurementResource.class));
    environment.jersey().register(Injector.getInstance(StatisticResource.class));
    environment.jersey().register(Injector.getInstance(NotificationMethodResource.class));

    /** Configure providers */
    removeExceptionMappers(environment.jersey().getResourceConfig().getSingletons());
    environment.jersey().register(new EntityExistsExceptionMapper());
    environment.jersey().register(new EntityNotFoundExceptionMapper());
    environment.jersey().register(new IllegalArgumentExceptionMapper());
    environment.jersey().register(new InvalidEntityExceptionMapper());
    environment.jersey().register(new JsonProcessingExceptionMapper());
    environment.jersey().register(new JsonMappingExceptionManager());
    environment.jersey().register(new ConstraintViolationExceptionMapper());
    environment.jersey().register(new ThrowableExceptionMapper<Throwable>() {});

    /** Configure Jackson */
    environment.getObjectMapper().setPropertyNamingStrategy(
        PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    environment.getObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    environment.getObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    SimpleModule module = new SimpleModule("SerializationModule");
    module.addSerializer(new SubAlarmExpressionSerializer());
    environment.getObjectMapper().registerModule(module);

    /** Configure health checks */
    environment.healthChecks().register("kafka", new KafkaHealthCheck(config.kafka));

    /** Configure CORS filter */
    Dynamic corsFilter = environment.servlets().addFilter("cors", CrossOriginFilter.class);
    corsFilter.addMappingForUrlPatterns(null, true, "/*");
    corsFilter.setInitParameter("allowedOrigins", "*");
    corsFilter.setInitParameter("allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,X-Auth-Token");
    corsFilter.setInitParameter("allowedMethods", "OPTIONS,GET,HEAD");

    if (config.middleware.enabled) {
      Map<String, String> authInitParams = new HashMap<String, String>();
      authInitParams.put("ServiceIds", config.middleware.serviceIds);
      authInitParams.put("EndpointIds", config.middleware.endpointIds);
      authInitParams.put("ServerVIP", config.middleware.serverVIP);
      authInitParams.put("ServerPort", config.middleware.serverPort);
      authInitParams.put("ConnTimeout", config.middleware.connTimeout);
      authInitParams.put("ConnSSLClientAuth", config.middleware.connSSLClientAuth);
      authInitParams.put("ConnPoolMaxActive", config.middleware.connPoolMaxActive);
      authInitParams.put("ConnPoolMaxIdle", config.middleware.connPoolMaxActive);
      authInitParams.put("ConnPoolEvictPeriod", config.middleware.connPoolEvictPeriod);
      authInitParams.put("ConnPoolMinIdleTime", config.middleware.connPoolMinIdleTime);
      authInitParams.put("ConnRetryTimes", config.middleware.connRetryTimes);
      authInitParams.put("ConnRetryInterval", config.middleware.connRetryInterval);
      authInitParams.put("AdminToken", config.middleware.adminToken);
      authInitParams.put("TimeToCacheToken", config.middleware.timeToCacheToken);
      authInitParams.put("AdminAuthMethod", config.middleware.adminAuthMethod);
      authInitParams.put("AdminUser", config.middleware.adminUser);
      authInitParams.put("AdminPassword", config.middleware.adminPassword);
      authInitParams.put("MaxTokenCacheSize", config.middleware.maxTokenCacheSize);
      setIfNotNull(authInitParams, AuthConstants.TRUSTSTORE, config.middleware.truststore);
      setIfNotNull(authInitParams, AuthConstants.TRUSTSTORE_PASS, config.middleware.truststorePassword);
      setIfNotNull(authInitParams, AuthConstants.KEYSTORE, config.middleware.keystore);
      setIfNotNull(authInitParams, AuthConstants.KEYSTORE_PASS, config.middleware.keystorePassword);

      /** Configure auth filters */
      Dynamic preAuthenticationFilter =
          environment.servlets().addFilter("pre-auth", new PreAuthenticationFilter());
      preAuthenticationFilter.addMappingForUrlPatterns(null, true, "/");
      preAuthenticationFilter.addMappingForUrlPatterns(null, true, "/v2.0/*");

      Dynamic tokenAuthFilter = environment.servlets().addFilter("token-auth", new TokenAuth());
      tokenAuthFilter.addMappingForUrlPatterns(null, true, "/");
      tokenAuthFilter.addMappingForUrlPatterns(null, true, "/v2.0/*");
      tokenAuthFilter.setInitParameters(authInitParams);

      Dynamic postAuthenticationFilter =
          environment.servlets().addFilter(
              "post-auth",
              new PostAuthenticationFilter(config.middleware.defaultAuthorizedRoles,
                  config.middleware.agentAuthorizedRoles));
      postAuthenticationFilter.addMappingForUrlPatterns(null, true, "/");
      postAuthenticationFilter.addMappingForUrlPatterns(null, true, "/v2.0/*");

      environment.jersey().getResourceConfig().getContainerRequestFilters()
          .add(new RoleAuthorizationFilter());
    } else {
      Dynamic mockAuthenticationFilter =
          environment.servlets().addFilter("mock-auth", new MockAuthenticationFilter());
      mockAuthenticationFilter.addMappingForUrlPatterns(null, true, "/");
      mockAuthenticationFilter.addMappingForUrlPatterns(null, true, "/v2.0/*");
    }

    /** Configure swagger */
    SwaggerBundle.configure(config);
  }

  private void setIfNotNull(Map<String, String> authInitParams, String name, String value) {
    if (value != null) {
      authInitParams.put(name, value);
    }
  }

  private void removeExceptionMappers(Set<Object> items) {
    for (Iterator<Object> i = items.iterator(); i.hasNext();) {
      Object o = i.next();
      if (o instanceof ExceptionMapper)
        i.remove();
    }
  }
}
