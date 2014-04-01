package com.hpcloud.mon;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.hp.csbu.cc.middleware.TokenAuth;
import com.hpcloud.messaging.kafka.KafkaHealthCheck;
import com.hpcloud.mon.infrastructure.servlet.PostAuthenticationFilter;
import com.hpcloud.mon.infrastructure.servlet.PreAuthenticationFilter;
import com.hpcloud.mon.resource.*;
import com.hpcloud.mon.resource.exception.*;
import com.hpcloud.util.Injector;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.reader.ClassReaders;
import io.dropwizard.Application;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.servlet.FilterRegistration.Dynamic;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
        environment.jersey().register(new ResourceNotFoundExceptionMapper());
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

        /** Configure health checks */
        environment.healthChecks().register("kafka", new KafkaHealthCheck(config.kafka));

        /** Configure auth filters */
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

            environment.servlets()
                    .addFilter("pre-auth", new PreAuthenticationFilter())
                    .addMappingForUrlPatterns(null, true, "/*");
            Dynamic filter = environment.servlets().addFilter("token-auth", new TokenAuth());
            filter.addMappingForUrlPatterns(null, true, "/*");
            filter.setInitParameters(authInitParams);
            environment.servlets()
                    .addFilter("post-auth", new PostAuthenticationFilter(config.middleware.rolesToMatch))
                    .addMappingForUrlPatterns(null, true, "/*");
        }

        environment.jersey().register(new ApiListingResourceJSON());

        // Swagger providers
        environment.jersey().register(new ApiDeclarationProvider());
        environment.jersey().register(new ResourceListingProvider());

        // Swagger Scanner, which finds all the resources for @Api Annotations
        ScannerFactory.setScanner(new DefaultJaxrsScanner());

        // Add the reader, which scans the resources and extracts the resource information
        ClassReaders.setReader(new DefaultJaxrsApiReader());

        // Set the swagger config options
        SwaggerConfig swaggerConfig = ConfigFactory.config();
        swaggerConfig.setApiVersion("1.0.1");
        swaggerConfig.setBasePath("http://localhost:8000");
    }

    private void removeExceptionMappers(Set<Object> items) {
        for (Iterator<Object> i = items.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof ExceptionMapper)
                i.remove();
        }
    }
}
