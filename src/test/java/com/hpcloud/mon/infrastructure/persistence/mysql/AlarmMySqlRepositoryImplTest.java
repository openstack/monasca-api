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

package com.hpcloud.mon.infrastructure.persistence.mysql;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;

@Test
public class AlarmMySqlRepositoryImplTest {
  private DBI db;
  private Handle handle;
  private AlarmRepository repo;
  private List<String> alarmActions;

  @BeforeClass
  protected void setupClass() throws Exception {
    db = new DBI("jdbc:h2:mem:test;MODE=MySQL");
    handle = db.open();
    handle
        .execute(Resources.toString(getClass().getResource("alarm.sql"), Charset.defaultCharset()));
    repo = new AlarmMySqlRepositoryImpl(db);

    alarmActions = new ArrayList<String>();
    alarmActions.add("29387234");
    alarmActions.add("77778687");
  }

  @AfterClass
  protected void afterClass() {
    handle.close();
  }

  @BeforeMethod
  protected void beforeMethod() {
    handle.execute("SET foreign_key_checks = 0;");
    handle.execute("truncate table alarm");
    handle.execute("truncate table alarm_action");
    handle.execute("truncate table alarm_definition");
    handle.execute("truncate table alarm_metric");
    handle.execute("truncate table metric_definition");
    handle.execute("truncate table metric_definition_dimensions");
    handle.execute("truncate table metric_dimension");

    handle
        .execute("insert into alarm_definition (id, tenant_id, name, severity, expression, match_by, actions_enabled, created_at, updated_at, deleted_at) "
            + "values ('1', 'bob', '90% CPU', 'LOW', 'avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu, device=1}) > 10', 'flavor_id,image_id', 1, NOW(), NOW(), NULL)");
    handle
        .execute("insert into alarm (id, alarm_definition_id, state, created_at, updated_at) values ('1', '1', 'OK', NOW(), NOW())");
    handle
        .execute("insert into alarm (id, alarm_definition_id, state, created_at, updated_at) values ('2', '1', 'UNDETERMINED', NOW(), NOW())");
    handle
        .execute("insert into alarm (id, alarm_definition_id, state, created_at, updated_at) values ('3', '1', 'ALARM', NOW(), NOW())");
    handle
        .execute("insert into alarm_metric (alarm_id, metric_definition_dimensions_id) values ('1', 11)");
    handle
        .execute("insert into alarm_metric (alarm_id, metric_definition_dimensions_id) values ('1', 22)");
    handle
        .execute("insert into alarm_metric (alarm_id, metric_definition_dimensions_id) values ('2', 11)");
    handle
        .execute("insert into alarm_metric (alarm_id, metric_definition_dimensions_id) values ('3', 22)");
    handle
        .execute("insert into metric_definition (id, name, tenant_id, region) values (1, 'cpu', 'bob', 'west')");
    handle
        .execute("insert into metric_definition (id, name, tenant_id, region) values (2, 'mem', 'bob', 'west')");
    handle
        .execute("insert into metric_definition_dimensions (id, metric_definition_id, metric_dimension_set_id) values (11, 1, 1)");
    handle
        .execute("insert into metric_definition_dimensions (id, metric_definition_id, metric_dimension_set_id) values (22, 2, 1)");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (1, 'instance_id', '123')");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (1, 'flavor_id', '222')");

    handle
        .execute("insert into alarm_definition (id, tenant_id, name, severity, expression, match_by, actions_enabled, created_at, updated_at, deleted_at) "
            + "values ('234', 'bob', '50% CPU', 'LOW', 'avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=mem}) > 20 and avg(hpcs.compute) < 100', 'flavor_id,image_id', 1, NOW(), NOW(), NULL)");
    handle
        .execute("insert into alarm (id, alarm_definition_id, state, created_at, updated_at) values ('234111', '234', 'UNDETERMINED', NOW(), NOW())");
    handle
        .execute("insert into alarm (id, alarm_definition_id, state, created_at, updated_at) values ('234222', '234', 'ALARM', NOW(), NOW())");
  }

  @Test(groups = "database")
  public void shouldDelete() {
    repo.deleteById("123111");

    try {
      assertNull(repo.findById("123111"));
      fail();
    } catch (EntityNotFoundException expected) {
    }
  }

  @Test(groups = "database")
  public void shouldFind() {
    List<Alarm> alarms =
        repo.find("bob", "1", "cpu",
            ImmutableMap.<String, String>builder().put("instance_id", "123").build(), null);
    assertEquals(alarms, Arrays.asList(new Alarm("123111", "123", "90% CPU", Arrays
        .asList(new MetricDefinition("hpcs.compute", ImmutableMap.<String, String>builder()
            .put("flavor_id", "777").put("image_id", "888").put("metric_name", "cpu").build())),
        AlarmState.ALARM)));
  }

  @Test(groups = "database")
  public void shouldFindById() {
    Alarm alarm = repo.findById("123111");

    assertEquals(alarm.getId(), "123111");
    assertEquals(alarm.getAlarmDefinitionId(), "123");
    assertEquals(alarm.getState(), "90% CPU");
    assertEquals(
        alarm.getMetrics(),
        Arrays.asList(new MetricDefinition("hpcs.compute", ImmutableMap.<String, String>builder()
            .put("flavor_id", "777").put("image_id", "888").put("metric_name", "cpu").build())));
  }
}
