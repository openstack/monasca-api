/*
 * Copyright (c) 2014,2016 Hewlett Packard Enterprise Development Company, L.P.
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

package monasca.api.infrastructure.persistence.mysql;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.util.StringMapper;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.common.model.alarm.AggregateFunction;
import monasca.common.model.alarm.AlarmOperator;
import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepo;

@Test(groups = "database")
public class AlarmDefinitionMySqlRepositoryImplTest {
  private DBI db;
  private Handle handle;
  private AlarmDefinitionRepo repo;
  private List<String> alarmActions;
  private AlarmDefinition alarmDef_123;
  private AlarmDefinition alarmDef_234;
  private AlarmDefinition alarmDef_345;

  @BeforeClass
  protected void setupClass() throws Exception {
    db = new DBI("jdbc:h2:mem:test;MODE=MySQL");
    handle = db.open();
    handle
        .execute(Resources.toString(getClass().getResource("alarm.sql"), Charset.defaultCharset()));
    repo = new AlarmDefinitionMySqlRepoImpl(db, new PersistUtils());

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
    handle.execute("truncate table sub_alarm");
    handle.execute("truncate table sub_alarm_definition");
    handle.execute("truncate table alarm_action");
    handle.execute("truncate table sub_alarm_definition_dimension");
    handle.execute("truncate table alarm_definition");

    handle
        .execute("insert into alarm_definition (id, tenant_id, name, severity, expression, match_by, actions_enabled, created_at, updated_at, deleted_at) "
            + "values ('123', 'bob', '90% CPU', 'LOW', 'avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu, device=1}) > 10', 'flavor_id,image_id', 1, NOW(), NOW(), NULL)");
    handle
        .execute("insert into sub_alarm_definition (id, alarm_definition_id, function, metric_name, operator, threshold, period, periods, created_at, updated_at) "
            + "values ('111', '123', 'avg', 'hpcs.compute', 'GT', 10, 60, 1, NOW(), NOW())");
    handle.execute("insert into sub_alarm_definition_dimension values ('111', 'flavor_id', '777')");
    handle.execute("insert into sub_alarm_definition_dimension values ('111', 'image_id', '888')");
    handle.execute("insert into sub_alarm_definition_dimension values ('111', 'metric_name', 'cpu')");
    handle.execute("insert into sub_alarm_definition_dimension values ('111', 'device', '1')");
    handle.execute("insert into alarm_action values ('123', 'ALARM', '29387234')");
    handle.execute("insert into alarm_action values ('123', 'ALARM', '77778687')");

    handle
        .execute("insert into alarm_definition (id, tenant_id, name, severity, expression, match_by, actions_enabled, created_at, updated_at, deleted_at) "
            + "values ('234', 'bob', '50% CPU', 'HIGH', 'avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=mem}) > 20 and avg(hpcs.compute) < 100', 'flavor_id,image_id', 1, NOW(), NOW(), NULL)");
    handle
        .execute("insert into sub_alarm_definition (id, alarm_definition_id, function, metric_name, operator, threshold, period, periods, created_at, updated_at) "
            + "values ('222', '234', 'avg', 'hpcs.compute', 'GT', 20, 60, 1, NOW(), NOW())");
    handle
        .execute("insert into sub_alarm_definition (id, alarm_definition_id, function, metric_name, operator, threshold, period, periods, created_at, updated_at) "
            + "values ('223', '234', 'avg', 'hpcs.compute', 'LT', 100, 60, 1, NOW(), NOW())");
    handle.execute("insert into sub_alarm_definition_dimension values ('222', 'flavor_id', '777')");
    handle.execute("insert into sub_alarm_definition_dimension values ('222', 'image_id', '888')");
    handle.execute("insert into sub_alarm_definition_dimension values ('222', 'metric_name', 'mem')");
    handle.execute("insert into alarm_action values ('234', 'ALARM', '29387234')");
    handle.execute("insert into alarm_action values ('234', 'ALARM', '77778687')");

    handle
        .execute("insert into alarm_definition (id, tenant_id, name, severity, expression, match_by, actions_enabled, created_at, updated_at, deleted_at) "
                 + "values ('345', 'bob', 'Testing Critical', 'CRITICAL', 'avg(test_metric{flavor_id=777, image_id=888, metric_name=mem}) > 20 and avg(test_metric) < 100', 'flavor_id,image_id', 1, NOW(), NOW(), NULL)");
    handle
        .execute("insert into sub_alarm_definition (id, alarm_definition_id, function, metric_name, operator, threshold, period, periods, created_at, updated_at) "
                 + "values ('333', '345', 'avg', 'test_metric', 'GT', 20, 60, 1, NOW(), NOW())");
    handle
        .execute("insert into sub_alarm_definition (id, alarm_definition_id, function, metric_name, operator, threshold, period, periods, created_at, updated_at) "
                 + "values ('334', '345', 'avg', 'test_metric', 'LT', 100, 60, 1, NOW(), NOW())");
    handle.execute("insert into sub_alarm_definition_dimension values ('333', 'flavor_id', '777')");
    handle.execute("insert into sub_alarm_definition_dimension values ('333', 'image_id', '888')");
    handle.execute("insert into sub_alarm_definition_dimension values ('333', 'metric_name', 'mem')");
    handle.execute("insert into alarm_action values ('345', 'ALARM', '29387234')");
    handle.execute("insert into alarm_action values ('345', 'ALARM', '77778687')");

    alarmDef_123 = new AlarmDefinition("123", "90% CPU", null, "LOW",
        "avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu, device=1}) > 10",
        Arrays.asList("flavor_id", "image_id"), true, Arrays.asList("29387234", "77778687"),
        Collections.<String>emptyList(), Collections.<String>emptyList());
    alarmDef_234 = new AlarmDefinition("234","50% CPU", null, "HIGH",
        "avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=mem}) > 20 and avg(hpcs.compute) < 100",
        Arrays.asList("flavor_id", "image_id"), true, Arrays.asList("29387234", "77778687"),
        Collections.<String>emptyList(), Collections.<String>emptyList());
    alarmDef_345 = new AlarmDefinition("345","Testing Critical", null, "CRITICAL",
        "avg(test_metric{flavor_id=777, image_id=888, metric_name=mem}) > 20 and avg(test_metric) < 100",
        Arrays.asList("flavor_id", "image_id"), true, Arrays.asList("29387234", "77778687"),
        Collections.<String>emptyList(), Collections.<String>emptyList());
  }

  public void shouldCreate() {
    Map<String, AlarmSubExpression> subExpressions =
        ImmutableMap
            .<String, AlarmSubExpression>builder()
            .put(
                "4433",
                AlarmSubExpression
                    .of("avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu}) > 10"))
            .build();

    AlarmDefinition alarmA =
        repo.create("555", "2345", "90% CPU", null, "LOW",
            "avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu}) > 10", subExpressions,
            Arrays.asList("flavor_id", "image_id"), alarmActions, null, null);
    AlarmDefinition alarmB = repo.findById("555", alarmA.getId());

    assertEquals(alarmA, alarmB);

    // Assert that sub-alarm and sub-alarm-dimensions made it to the db
    assertEquals(
        handle.createQuery("select count(*) from sub_alarm_definition where id = 4433")
            .map(StringMapper.FIRST).first(), "1");
    assertEquals(
        handle.createQuery("select count(*) from sub_alarm_definition_dimension where sub_alarm_definition_id = 4433")
            .map(StringMapper.FIRST).first(), "3");
  }

  @Test(groups = "database")
  public void shouldUpdate() {
    // This test won't work without the real mysql database so use mini-mon.
    // Warning, this will truncate your mini-mon database
    db = new DBI("jdbc:mysql://192.168.10.4/mon", "monapi", "password");
    handle = db.open();
    repo = new AlarmDefinitionMySqlRepoImpl(db, new PersistUtils());
    beforeMethod();

    List<String> oldSubAlarmIds = Arrays.asList("222");
    AlarmSubExpression changedSubExpression = AlarmSubExpression.of("avg(hpcs.compute) <= 200");
    Map<String, AlarmSubExpression> changedSubExpressions =
        ImmutableMap.<String, AlarmSubExpression>builder().put("223", changedSubExpression).build();
    AlarmSubExpression newSubExpression = AlarmSubExpression.of("avg(foo{flavor_id=777}) > 333");
    Map<String, AlarmSubExpression> newSubExpressions =
        ImmutableMap.<String, AlarmSubExpression>builder().put("555", newSubExpression).build();

    repo.update("bob", "234", false, "90% CPU", null,
        "avg(foo{flavor_id=777}) > 333 and avg(hpcs.compute) <= 200",
        Arrays.asList("flavor_id", "image_id"), "LOW", false, oldSubAlarmIds,
        changedSubExpressions, newSubExpressions, alarmActions, null, null);

    AlarmDefinition alarm = repo.findById("bob", "234");
    AlarmDefinition expected =
        new AlarmDefinition("234", "90% CPU", null, "LOW",
            "avg(foo{flavor_id=777}) > 333 and avg(hpcs.compute) <= 200", Arrays.asList(
                "flavor_id", "image_id"), false, alarmActions, Collections.<String>emptyList(),
            Collections.<String>emptyList());
    assertEquals(expected, alarm);

    Map<String, AlarmSubExpression> subExpressions = repo.findSubExpressions("234");
    assertEquals(subExpressions.get("223"), changedSubExpression);
    assertEquals(subExpressions.get("555"), newSubExpression);
  }

  public void shouldFindById() {
    assertEquals(alarmDef_123, repo.findById("bob", "123"));

    // Make sure it still finds AlarmDefinitions with no notifications
    handle.execute("delete from alarm_action");
    alarmDef_123.setAlarmActions(new ArrayList<String>(0));
    assertEquals(alarmDef_123, repo.findById("bob", "123"));    
  }

  @Test(groups = "database")
  public void shouldFindSubAlarmMetricDefinitions() {
    // This test won't work without the real mysql database so use mini-mon.
    // Warning, this will truncate your mini-mon database
    db = new DBI("jdbc:mysql://192.168.10.4/mon", "monapi", "password");
    handle = db.open();
    repo = new AlarmDefinitionMySqlRepoImpl(db, new PersistUtils());
    beforeMethod();

    assertEquals(
        repo.findSubAlarmMetricDefinitions("123").get("111"),
        new MetricDefinition("hpcs.compute", ImmutableMap.<String, String>builder()
            .put("flavor_id", "777").put("image_id", "888").put("metric_name", "cpu")
            .put("device", "1").build()));

    assertEquals(
        repo.findSubAlarmMetricDefinitions("234").get("222"),
        new MetricDefinition("hpcs.compute", ImmutableMap.<String, String>builder()
            .put("flavor_id", "777").put("image_id", "888").put("metric_name", "mem").build()));

    assertTrue(repo.findSubAlarmMetricDefinitions("asdfasdf").isEmpty());
  }

  @Test(groups = "database")
  public void shouldFindSubExpressions() {
    // This test won't work without the real mysql database so use mini-mon.
    // Warning, this will truncate your mini-mon database
    db = new DBI("jdbc:mysql://192.168.10.4/mon", "monapi", "password");
    handle = db.open();
    repo = new AlarmDefinitionMySqlRepoImpl(db, new PersistUtils());
    beforeMethod();

    assertEquals(
        repo.findSubExpressions("123").get("111"),
        new AlarmSubExpression(AggregateFunction.AVG, new MetricDefinition("hpcs.compute",
            ImmutableMap.<String, String>builder().put("flavor_id", "777").put("image_id", "888")
                .put("metric_name", "cpu").put("device", "1").build()), AlarmOperator.GT, 10, 60, 1));

    assertEquals(repo.findSubExpressions("234").get("223"), new AlarmSubExpression(
        AggregateFunction.AVG, new MetricDefinition("hpcs.compute", new HashMap<String, String>()), AlarmOperator.LT, 100,
        60, 1));

    assertTrue(repo.findSubAlarmMetricDefinitions("asdfasdf").isEmpty());
  }

  public void testExists() {
    assertEquals(repo.exists("bob", "90% CPU"),"123");

    // Negative
    assertNull(repo.exists("bob", "999% CPU"));
  }

  public void shouldFind() {
    assertEquals(Arrays.asList(alarmDef_123, alarmDef_234), repo.find("bob", null, null, null, null, null, 1));

    // Make sure it still finds AlarmDefinitions with no notifications
    handle.execute("delete from alarm_action");
    alarmDef_123.setAlarmActions(new ArrayList<String>(0));
    alarmDef_234.setAlarmActions(new ArrayList<String>(0));
    assertEquals(Arrays.asList(alarmDef_123, alarmDef_234), repo.find("bob", null, null, null, null, null, 1));

    assertEquals(0, repo.find("bill", null, null, null, null, null, 1).size());

    assertEquals(Arrays.asList(alarmDef_234, alarmDef_123),
                 repo.find("bob", null, null, null, Arrays.asList("name"), null, 1));

    assertEquals(Arrays.asList(alarmDef_234, alarmDef_123),
                 repo.find("bob", null, null, null, Arrays.asList("id desc"), null, 1));
  }

  public void shouldFindByDimension() {
    final Map<String, String> dimensions = new HashMap<>();
    dimensions.put("image_id", "888");
    assertEquals(Arrays.asList(alarmDef_123, alarmDef_234),
                 repo.find("bob", null, dimensions, null, null, null, 1));

    dimensions.clear();
    dimensions.put("device", "1");
    assertEquals(Arrays.asList(alarmDef_123), repo.find("bob", null, dimensions, null, null, null, 1));

    dimensions.clear();
    dimensions.put("Not real", "AA");
    assertEquals(0, repo.find("bob", null, dimensions, null, null, null, 1).size());
  }

  public void shouldFindByName() {
    assertEquals(Arrays.asList(alarmDef_123), repo.find("bob", "90% CPU", null, null, null, null, 1));

    assertEquals(0, repo.find("bob", "Does not exist", null, null, null, null, 1).size());
  }

  public void shouldFindBySeverity() {
    assertEquals(Arrays.asList(alarmDef_234), repo.find("bob", null, null, Lists.newArrayList(AlarmSeverity.HIGH), null, null, 1));

    assertEquals(0, repo.find("bob", null, null, Lists.newArrayList(AlarmSeverity.CRITICAL), null, null, 1).size());

    assertEquals(Arrays.asList(alarmDef_234, alarmDef_345),
                 repo.find("bob", null, null, Lists.newArrayList(AlarmSeverity.HIGH, AlarmSeverity.CRITICAL),
                           null, null, 2));
  }

  public void shouldDeleteById() {
    repo.deleteById("bob", "123");

    try {
      assertNull(repo.findById("bob", "123"));
      fail();
    } catch (EntityNotFoundException expected) {
    }
    assertEquals(Arrays.asList(alarmDef_234), repo.find("bob", null, null, null, null, null, 1));
  }
}
