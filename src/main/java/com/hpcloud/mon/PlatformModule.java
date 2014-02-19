package com.hpcloud.mon;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.skife.jdbi.v2.DBI;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.sun.jersey.api.client.Client;

/**
 * Platform (non-application) specific bindings.
 * 
 * @author Jonathan Halterman
 */
public class PlatformModule extends AbstractModule {
  private final MonApiConfiguration config;
  private final Environment environment;

  public PlatformModule(Environment environment, MonApiConfiguration config) {
    this.environment = environment;
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(DataSourceFactory.class).toInstance(config.database);
    bind(DBI.class).toProvider(new Provider<DBI>() {
      @Override
      public DBI get() {
        try {
          return new DBIFactory().build(environment, config.database, "mysql");
        } catch (ClassNotFoundException e) {
          throw new ProvisionException("Failed to provision DBI", e);
        }
      }
    }).in(Scopes.SINGLETON);

    // install(new RabbitMQModule());
    // bind(RabbitMQConfiguration.class).annotatedWith(Names.named("internal")).toInstance(
    // config.internalRabbit);
    // bind(RabbitMQConfiguration.class).annotatedWith(Names.named("external")).toInstance(
    // config.externalRabbit);

    bind(Client.class).toProvider(new Provider<Client>() {
      @Override
      @SuppressWarnings("deprecation")
      public Client get() {
        try {
          // Keystore
          KeyStore ks = KeyStore.getInstance("jks");
          FileInputStream is1 = new FileInputStream(new File(config.middleware.keystore));
          try {
            ks.load(is1, config.middleware.keystorePass.toCharArray());
          } finally {
            is1.close();
          }

          // Truststore
          KeyStore ts = KeyStore.getInstance("jks");
          FileInputStream is2 = new FileInputStream(new File(config.middleware.truststore));
          try {
            ts.load(is2, config.middleware.truststorePass.toCharArray());
          } finally {
            is2.close();
          }

          SSLSocketFactory ssf = new SSLSocketFactory(ks, config.middleware.keystorePass, ts);
          PlainSocketFactory psf = PlainSocketFactory.getSocketFactory();
          SchemeRegistry sr = new SchemeRegistry();
          sr.register(new Scheme("http", 80, psf));
          sr.register(new Scheme("http", 8080, psf));
          sr.register(new Scheme("https", 443, ssf));
          return new JerseyClientBuilder(environment).using(config.jerseyClient)
              .using(environment)
              .using(sr)
              .build("default");
        } catch (Exception e) {
          throw new ProvisionException("Failed to create jersey client", e);
        }
      }
    }).in(Scopes.SINGLETON);

    // // Bind external config by default for AdminService to use
    // bind(RabbitMQConfiguration.class).toInstance(config.externalRabbit);
    // bind(RabbitMQAdminService.class).in(Scopes.SINGLETON);
  }

  // @Provides
  // @Named("internal")
  // @Singleton
  // public RabbitMQService getInternalRabbit() {
  // return new RabbitMQService(config.internalRabbit);
  // }
  //
  // @Provides
  // @Named("external")
  // @Singleton
  // public RabbitMQService getExternalRabbit() {
  // return new RabbitMQService(config.externalRabbit);
  // }
}
