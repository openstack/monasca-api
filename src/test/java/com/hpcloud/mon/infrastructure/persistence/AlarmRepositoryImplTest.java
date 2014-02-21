package com.hpcloud.mon.infrastructure.persistence;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.google.common.io.Resources;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.alarm.AlarmDetail;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;

/**
 * @author Jonathan Halterman
 */
@Test
public class AlarmRepositoryImplTest {
  private DBI db;
  private Handle handle;
  private AlarmRepository repo;
  private List<String> alarmActions;

  @BeforeClass
  protected void setupClass() throws Exception {
    db = new DBI("jdbc:h2:mem:test;MODE=MySQL");
    handle = db.open();
    handle.execute(Resources.toString(getClass().getResource("alarm.sql"), Charset.defaultCharset()));
    repo = new AlarmRepositoryImpl(db);

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
    handle.execute("truncate table sub_alarm_dimension");
    handle.execute("truncate table alarm_action");
    handle.execute("truncate table sub_alarm");
    handle.execute("truncate table alarm");

    handle.execute("insert into alarm (id, tenant_id, name, expression, state, created_at, updated_at, deleted_at) "
        + "values ('123', 'bob', '90% CPU', 'avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu, device=1}) > 10', 'UNDETERMINED', NOW(), NOW(), NULL)");
    handle.execute("insert into sub_alarm (id, alarm_id, function, namespace, operator, threshold, period, periods, state, created_at, updated_at) "
        + "values ('111', '123', 'avg', 'hpcs.compute', 'GT', 10, 60, 1, 'UNDETERMINED', NOW(), NOW())");
    handle.execute("insert into sub_alarm_dimension values ('111', 'flavor_id', '777')");
    handle.execute("insert into sub_alarm_dimension values ('111', 'image_id', '888')");
    handle.execute("insert into sub_alarm_dimension values ('111', 'metric_name', 'cpu')");
    handle.execute("insert into sub_alarm_dimension values ('111', 'device', '1')");
    handle.execute("insert into alarm_action values ('123', '29387234')");
    handle.execute("insert into alarm_action values ('123', '77778687')");

    handle.execute("insert into alarm (id, tenant_id, name, expression, state, created_at, updated_at, deleted_at) "
        + "values ('234', 'bob', '50% CPU', 'avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=mem}) > 20', 'UNDETERMINED', NOW(), NOW(), NULL)");
    handle.execute("insert into sub_alarm (id, alarm_id, function, namespace, operator, threshold, period, periods, state, created_at, updated_at) "
        + "values ('222', '234', 'avg', 'hpcs.compute', 'GT', 20, 60, 1, 'UNDETERMINED', NOW(), NOW())");
    handle.execute("insert into sub_alarm_dimension values ('222', 'flavor_id', '777')");
    handle.execute("insert into sub_alarm_dimension values ('222', 'image_id', '888')");
    handle.execute("insert into sub_alarm_dimension values ('222', 'metric_name', 'mem')");
    handle.execute("insert into alarm_action values ('234', '29387234')");
    handle.execute("insert into alarm_action values ('234', '77778687')");
  }

  public void shouldCreate() {
    Map<String, AlarmSubExpression> subExpressions = ImmutableMap.<String, AlarmSubExpression>builder()
        .put(
            "4433",
            AlarmSubExpression.of("avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu}) > 10"))
        .build();

    AlarmDetail alarmA = repo.create("2345", "555", "90% CPU",
        "avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu}) > 10", subExpressions,
        alarmActions);
    AlarmDetail alarmB = repo.findById("555", alarmA.getId());

    assertEquals(alarmA, alarmB);

    // Assert that sub-alarm and sub-alarm-dimensions made it to the db
    assertEquals(
        handle.createQuery("select count(*) from sub_alarm where id = 4433")
            .map(StringMapper.FIRST)
            .first(), "1");
    assertEquals(
        handle.createQuery("select count(*) from sub_alarm_dimension where sub_alarm_id = 4433")
            .map(StringMapper.FIRST)
            .first(), "3");
  }

  public void shouldFindById() {
    AlarmDetail alarm = repo.findById("bob", "123");

    assertEquals(alarm.getId(), "123");
    assertEquals(alarm.getName(), "90% CPU");
    assertEquals(alarm.getExpression(),
        "avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu, device=1}) > 10");
    assertEquals(alarm.getState(), AlarmState.UNDETERMINED);
    assertEquals(alarm.getAlarmActions(), alarmActions);
  }

  @Test(groups = "database")
  public void shouldFindSubAlarmIds() {
    db = new DBI("jdbc:mysql://localhost/maas", "root", "password");
    handle = db.open();
    repo = new AlarmRepositoryImpl(db);
    beforeMethod();

    assertEquals(
        repo.findSubAlarmMetricDefinitions("123").get("111"),
        new MetricDefinition("hpcs.compute", ImmutableMap.<String, String>builder()
            .put("flavor_id", "777")
            .put("image_id", "888")
            .put("metric_name", "cpu")
            .put("device", "1")
            .build()));

    assertEquals(
        repo.findSubAlarmMetricDefinitions("234").get("222"),
        new MetricDefinition("hpcs.compute", ImmutableMap.<String, String>builder()
            .put("flavor_id", "777")
            .put("image_id", "888")
            .put("metric_name", "mem")
            .build()));

    assertTrue(repo.findSubAlarmMetricDefinitions("asdfasdf").isEmpty());
  }

  public void testExists() {
    assertTrue(repo.exists("bob", "90% CPU"));

    // Negative
    assertFalse(repo.exists("bob", "999% CPU"));
  }

  public void shouldFind() {
    List<Alarm> alarms = repo.find("bob");

    assertEquals(alarms, Arrays.asList(new Alarm("123", "90% CPU",
        "avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu, device=1}) > 10", AlarmState.UNDETERMINED),
        new Alarm("234", "50% CPU", "avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=mem}) > 20",
            AlarmState.UNDETERMINED)));
  }

  public void shouldDeleteById() {
    repo.deleteById("bob", "123");

    try {
      assertNull(repo.findById("bob", "123"));
      fail();
    } catch (EntityNotFoundException expected) {
    }
  }
}
