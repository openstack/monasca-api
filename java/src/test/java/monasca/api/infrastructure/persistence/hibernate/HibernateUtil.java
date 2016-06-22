/*
 * Copyright 2015 FUJITSU LIMITED
 * (C) Copyright 2014,2016 Hewlett Packard Enterprise Development LP
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
package monasca.api.infrastructure.persistence.hibernate;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import monasca.common.hibernate.db.AlarmActionDb;
import monasca.common.hibernate.db.AlarmDb;
import monasca.common.hibernate.db.AlarmDefinitionDb;
import monasca.common.hibernate.db.AlarmMetricDb;
import monasca.common.hibernate.db.MetricDefinitionDb;
import monasca.common.hibernate.db.MetricDefinitionDimensionsDb;
import monasca.common.hibernate.db.MetricDimensionDb;
import monasca.common.hibernate.db.NotificationMethodDb;
import monasca.common.hibernate.db.NotificationMethodTypesDb;
import monasca.common.hibernate.db.SubAlarmDb;
import monasca.common.hibernate.db.SubAlarmDefinitionDb;
import monasca.common.hibernate.db.SubAlarmDefinitionDimensionDb;

class HibernateUtil {

  private static Configuration CONFIGURATION = null;

  static {
    try {
      Configuration configuration = new Configuration();

      configuration.addAnnotatedClass(AlarmDb.class);
      configuration.addAnnotatedClass(AlarmDefinitionDb.class);
      configuration.addAnnotatedClass(AlarmMetricDb.class);
      configuration.addAnnotatedClass(MetricDefinitionDb.class);
      configuration.addAnnotatedClass(MetricDefinitionDimensionsDb.class);
      configuration.addAnnotatedClass(MetricDimensionDb.class);
      configuration.addAnnotatedClass(SubAlarmDefinitionDb.class);
      configuration.addAnnotatedClass(SubAlarmDefinitionDimensionDb.class);
      configuration.addAnnotatedClass(SubAlarmDb.class);
      configuration.addAnnotatedClass(AlarmActionDb.class);
      configuration.addAnnotatedClass(NotificationMethodDb.class);
      configuration.addAnnotatedClass(NotificationMethodTypesDb.class);

      configuration.setProperties(getHikariH2Properties());

      HibernateUtil.CONFIGURATION = configuration;
    } catch (Throwable ex) {
      // Make sure you log the exception, as it might be swallowed
      System.err.println("Initial SessionFactory creation failed." + ex);
      throw new ExceptionInInitializerError(ex);
    }
  }

  private static Properties getHikariPostgresProperties() {
    Properties properties = new Properties();
    properties.put("hibernate.connection.provider_class", "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
    properties.put("hibernate.hbm2ddl.auto", "validate");
    properties.put("show_sql", true);
    properties.put("hibernate.hikari.dataSourceClassName", "org.postgresql.ds.PGPoolingDataSource");
    properties.put("hibernate.hikari.dataSource.serverName", "localhost");
    properties.put("hibernate.hikari.dataSource.portNumber", "5432");
    properties.put("hibernate.hikari.dataSource.databaseName", "mon");
    properties.put("hibernate.hikari.dataSource.user", "mon");
    properties.put("hibernate.hikari.dataSource.password", "mon");
    properties.put("hibernate.hikari.dataSource.initialConnections", "25");
    properties.put("hibernate.hikari.dataSource.maxConnections", "100");
    properties.put("hibernate.hikari.connectionTestQuery", "SELECT 1");
    return properties;
  }

  private static Properties getHikariMySqlProperties() {
    Properties properties = new Properties();
    properties.put("hibernate.connection.provider_class", "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
    properties.put("hibernate.hbm2ddl.auto", "validate");
    properties.put("show_sql", true);
    properties.put("hibernate.hikari.dataSourceClassName", "com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
    properties.put("hibernate.hikari.dataSource.url",
        "jdbc:mysql://192.168.10.4:3306/mon?useLegacyDatetimeCode=false&serverTimezone=UTC");
    properties.put("hibernate.hikari.dataSource.user", "monapi");
    properties.put("hibernate.hikari.dataSource.password", "password");
    return properties;
  }

  private static Properties getHikariH2Properties() {
    Properties properties = new Properties();
    properties.put("hibernate.connection.provider_class", "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
    properties.put("hibernate.hbm2ddl.auto", "create-drop");
    properties.put("show_sql", false);
    properties.put("hibernate.hikari.dataSourceClassName", "org.h2.jdbcx.JdbcDataSource");
    properties.put("hibernate.hikari.dataSource.url", "jdbc:h2:mem:mon;MODE=PostgreSQL");
    properties.put("hibernate.hikari.dataSource.user", "sa");
    properties.put("hibernate.hikari.dataSource.password", "");
    return properties;
  }

  public static SessionFactory getSessionFactory() throws HibernateException {
    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(CONFIGURATION.getProperties()).build();
    return CONFIGURATION.buildSessionFactory(serviceRegistry);
  }
}
