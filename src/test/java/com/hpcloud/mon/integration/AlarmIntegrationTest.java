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

package com.hpcloud.mon.integration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import kafka.javaapi.producer.Producer;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.MonApiModule;
import com.hpcloud.mon.app.AlarmService;
import com.hpcloud.mon.app.command.CreateAlarmCommand;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import com.hpcloud.mon.infrastructure.persistence.mysql.AlarmMySqlRepositoryImpl;
import com.hpcloud.mon.infrastructure.persistence.mysql.NotificationMethodMySqlRepositoryImpl;
import com.hpcloud.mon.resource.AbstractMonApiResourceTest;
import com.hpcloud.mon.resource.AlarmResource;
import com.sun.jersey.api.client.ClientResponse;

@Test(groups = "integration", enabled = false)
public class AlarmIntegrationTest extends AbstractMonApiResourceTest {
  private static final String TENANT_ID = "alarm-test";
  private DBI mysqlDb;
  private Alarm alarm;
  private AlarmService service;
  private MonApiConfiguration config;
  private Producer<String, String> producer;
  private AlarmRepository repo;
  AlarmStateHistoryRepository stateHistoryRepo;
  private Map<String, String> dimensions;
  private List<String> alarmActions;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();

    Handle handle = mysqlDb.open();
    handle.execute("truncate table alarm");
    handle.execute("truncate table notification_method");
    handle
        .execute("insert into notification_method (id, tenant_id, name, type, address, created_at, updated_at) values ('29387234', 'alarm-test', 'MySMS', 'SMS', '8675309', NOW(), NOW())");
    handle
        .execute("insert into notification_method (id, tenant_id, name, type, address, created_at, updated_at) values ('77778687', 'alarm-test', 'MySMS', 'SMS', '8675309', NOW(), NOW())");
    mysqlDb.close(handle);

    repo = new AlarmMySqlRepositoryImpl(mysqlDb);
    service =
        new AlarmService(config, producer, repo, new NotificationMethodMySqlRepositoryImpl(mysqlDb));
    addResources(new AlarmResource(service, repo, null));
  }

  @BeforeTest
  protected void beforeTest() throws Exception {
    config = getConfiguration("config-test.yml", MonApiConfiguration.class);
    Injector injector = Guice.createInjector(new MonApiModule(environment, config));
    producer = injector.getInstance(Key.get(new TypeLiteral<Producer<String, String>>() {}));
    mysqlDb = injector.getInstance(Key.get(DBI.class, Names.named("mysql")));
    Handle handle = mysqlDb.open();
    handle.execute(Resources.toString(
        NotificationMethodMySqlRepositoryImpl.class.getResource("alarm.sql"),
        Charset.defaultCharset()));
    handle.execute(Resources.toString(
        NotificationMethodMySqlRepositoryImpl.class.getResource("notification_method.sql"),
        Charset.defaultCharset()));
    handle.close();

    // Fixtures
    dimensions = new HashMap<String, String>();
    dimensions.put("instance_id", "937");
    alarmActions = new ArrayList<String>();
    alarmActions.add("29387234");
    alarmActions.add("77778687");
    alarm =
        new Alarm("123", "90% CPU", null, null, "avg(hpcs.compute:cpu:{instance_id=123} > 10",
            AlarmState.OK, true, alarmActions, null, null);
  }

  @AfterTest
  protected void afterTest() throws Exception {
    producer.close();
  }

  public void shouldCreate() throws Exception {
    ClientResponse response =
        client()
            .resource("/v2.0/alarms")
            .header("X-Tenant-Id", TENANT_ID)
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(
                ClientResponse.class,
                new CreateAlarmCommand("90% CPU", null, null,
                    "avg(hpcs.compute:cpu:{instance_id=123} > 10", alarmActions, null, null));

    Alarm newAlarm = response.getEntity(Alarm.class);
    String location = response.getHeaders().get("Location").get(0);
    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/alarms/" + newAlarm.getId());
    assertEquals(alarm.getExpression(), newAlarm.getExpression());
    assertEquals(alarm.getAlarmActions(), newAlarm.getAlarmActions());
  }

  public void shouldCreateCaseInsensitiveAndKeywords() throws Exception {
    Alarm alarm_local;
    alarm_local =
        new Alarm("123", "90% CPU", null, null, "AvG(avg:cpu:{instance_id=123} gT 10",
            AlarmState.OK, true, alarmActions, null, null);
    ClientResponse response =
        client()
            .resource("/v2.0/alarms")
            .header("X-Tenant-Id", TENANT_ID)
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(
                ClientResponse.class,
                new CreateAlarmCommand("90% CPU", null, null,
                    "AvG(avg:cpu:{instance_id=123} gT 10", alarmActions, null, null));

    Alarm newAlarm = response.getEntity(Alarm.class);
    String location = response.getHeaders().get("Location").get(0);
    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/alarms/" + newAlarm.getId());
    assertEquals(alarm_local.getExpression(), newAlarm.getExpression());
    assertEquals(alarm_local.getAlarmActions(), newAlarm.getAlarmActions());
  }

  public void shouldDelete() {
    Alarm newAlarm =
        repo.create(TENANT_ID, "123", alarm.getName(), null, alarm.getName(),
            alarm.getExpression(), null, alarm.getAlarmActions(), alarm.getOkActions(),
            alarm.getUndeterminedActions());
    assertNotNull(repo.findById(TENANT_ID, newAlarm.getId()));

    ClientResponse response =
        client().resource("/v2.0/alarms/" + newAlarm.getId()).header("X-Tenant-Id", TENANT_ID)
            .delete(ClientResponse.class);
    assertEquals(response.getStatus(), 204);

    try {
      assertNull(repo.findById(TENANT_ID, newAlarm.getId()));
      fail();
    } catch (EntityNotFoundException expected) {
    }
  }
}
