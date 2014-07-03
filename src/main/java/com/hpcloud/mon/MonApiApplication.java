/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpcloud.mon;

import io.dropwizard.Application;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Collections;
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
import com.hpcloud.middleware.TokenAuth;
import com.hpcloud.messaging.kafka.KafkaHealthCheck;
import com.hpcloud.mon.bundle.SwaggerBundle;
import com.hpcloud.mon.infrastructure.servlet.MockAuthenticationFilter;
import com.hpcloud.mon.infrastructure.servlet.PostAuthenticationFilter;
import com.hpcloud.mon.infrastructure.servlet.PreAuthenticationFilter;
import com.hpcloud.mon.resource.AlarmResource;
import com.hpcloud.mon.resource.MeasurementResource;
import com.hpcloud.mon.resource.MetricResource;
import com.hpcloud.mon.resource.NotificationMethodResource;
import com.hpcloud.mon.resource.StatisticResource;
import com.hpcloud.mon.resource.VersionResource;
import com.hpcloud.mon.resource.exception.ConstraintViolationExceptionMapper;
import com.hpcloud.mon.resource.exception.EntityExistsExceptionMapper;
import com.hpcloud.mon.resource.exception.EntityNotFoundExceptionMapper;
import com.hpcloud.mon.resource.exception.IllegalArgumentExceptionMapper;
import com.hpcloud.mon.resource.exception.InvalidEntityExceptionMapper;
import com.hpcloud.mon.resource.exception.JsonMappingExceptionManager;
import com.hpcloud.mon.resource.exception.JsonProcessingExceptionMapper;
import com.hpcloud.mon.resource.exception.ThrowableExceptionMapper;
import com.hpcloud.mon.resource.serialization.SubAlarmExpressionSerializer;
import com.hpcloud.util.Injector;

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
  public void run(MonApiConfiguration config, Environment environment) throws Exception {
    /** Wire services */
    Injector.registerModules(new MonApiModule(environment, config));

    /** Configure resources */
    environment.jersey().register(Injector.getInstance(VersionResource.class));
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
    environment.jersey().register(new ThrowableExceptionMapper<Throwable>() {
    });

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
    corsFilter.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin,X-Auth-Token");
    corsFilter.setInitParameter("allowedMethods", "OPTIONS,GET,HEAD");

    /** Configure auth filters */
    Dynamic preAuthenticationFilter = environment.servlets().addFilter("pre-auth",
        new PreAuthenticationFilter());
    preAuthenticationFilter.addMappingForUrlPatterns(null, true, "/");
    preAuthenticationFilter.addMappingForUrlPatterns(null, true, "/v2.0/*");

    if (config.middleware.enabled) {
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
      authInitParams.put("AdminToken", config.middleware.adminToken);
      authInitParams.put("TimeToCacheToken", config.middleware.timeToCacheToken);
      authInitParams.put("AdminAuthMethod", config.middleware.adminAuthMethod);
      authInitParams.put("AdminUser", config.middleware.adminUser);
      authInitParams.put("AdminPassword", config.middleware.adminPassword);
      authInitParams.put("MaxTokenCacheSize",config.middleware.maxTokenCacheSize);

      Dynamic tokenAuthFilter = environment.servlets().addFilter("token-auth", new TokenAuth());
      tokenAuthFilter.addMappingForUrlPatterns(null, true, "/");
      tokenAuthFilter.addMappingForUrlPatterns(null, true, "/v2.0/*");
      tokenAuthFilter.setInitParameters(authInitParams);
    } else {
      Dynamic mockAuthenticationFilter = environment.servlets().addFilter("mock-auth",
          new MockAuthenticationFilter());
      mockAuthenticationFilter.addMappingForUrlPatterns(null, true, "/");
      mockAuthenticationFilter.addMappingForUrlPatterns(null, true, "/v2.0/*");
    }

    Dynamic postAuthenticationFilter = environment.servlets()
        .addFilter("post-auth", new PostAuthenticationFilter(config.middleware.rolesToMatch));
    postAuthenticationFilter.addMappingForUrlPatterns(null, true, "/");
    postAuthenticationFilter.addMappingForUrlPatterns(null, true, "/v2.0/*");

    /** Configure swagger */
    SwaggerBundle.configure(config);
  }

  private void removeExceptionMappers(Set<Object> items) {
    for (Iterator<Object> i = items.iterator(); i.hasNext();) {
      Object o = i.next();
      if (o instanceof ExceptionMapper)
        i.remove();
    }
  }
}
