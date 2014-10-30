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
package monasca.api.bundle;

import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.setup.Environment;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.reader.ClassReaders;

public class SwaggerBundle extends AssetsBundle {
  private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerBundle.class);
  public static final String PATH = "/swagger-ui";

  public SwaggerBundle() {
    super(PATH, PATH, "index.html");
  }

  /**
   * Call this method from
   * {@link io.dropwizard.Application#run(io.dropwizard.Configuration, io.dropwizard.setup.Environment)}
   * passing the {@link io.dropwizard.Configuration} so that Swagger can be properly configured to
   * run on Amazon.
   * <p/>
   * It is recommended that this method is called even when running locally -which has no side
   * effect- so that this application can be deployed to an EC2 instance without any changes.
   */
  public static void configure(Configuration configuration) throws IOException {
    SwaggerConfig config = ConfigFactory.config();
    String swaggerBasePath = getSwaggerBasePath(configuration);
    config.setBasePath(swaggerBasePath);
    config.setApiPath(swaggerBasePath);
  }

  @Override
  public void run(Environment environment) {
    environment.jersey().register(new ApiListingResourceJSON());
    environment.jersey().register(new ApiDeclarationProvider());
    environment.jersey().register(new ResourceListingProvider());
    ScannerFactory.setScanner(new DefaultJaxrsScanner());
    ClassReaders.setReader(new DefaultJaxrsApiReader());
    super.run(environment);
  }

  private static String getSwaggerBasePath(Configuration configuration) throws IOException {
    String host;

    host = "localhost";
    LOGGER.info("Assume we are running locally");

    ServerFactory serverFactory = configuration.getServerFactory();
    HttpConnectorFactory httpConnectorFactory = null;

    if (serverFactory instanceof SimpleServerFactory) {
      ConnectorFactory cf = ((SimpleServerFactory) serverFactory).getConnector();
      if (cf instanceof HttpConnectorFactory) {
        httpConnectorFactory = (HttpConnectorFactory) cf;
      }
    } else if (serverFactory instanceof DefaultServerFactory) {
      List<ConnectorFactory> applicationConnectors =
          ((DefaultServerFactory) serverFactory).getApplicationConnectors();
      for (ConnectorFactory connectorFactory : applicationConnectors) {
        if (connectorFactory instanceof HttpConnectorFactory) {
          httpConnectorFactory = (HttpConnectorFactory) connectorFactory;
        }
      }
    }

    if (httpConnectorFactory == null) {
      throw new IllegalStateException("Could not get HttpConnectorFactory");
    }

    return String.format("%s:%s", host, httpConnectorFactory.getPort());
  }
}
