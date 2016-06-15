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
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmRepo;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * These tests won't work without the real mysql database so use mini-mon.
 * Warning, this will truncate your mini-mon database
 * @author craigbr
 *
 */
@Test
public class AlarmMySqlRepositoryImplTest {
  private static final String TENANT_ID = "bob";
  private static final String ALARM_ID = "234111";
  private static final DateTimeFormatter ISO_8601_FORMATTER = ISODateTimeFormat.dateOptionalTimeParser().withZoneUTC();
  private DBI db;
  private Handle handle;
  private AlarmRepo repo;
  private List<String> alarmActions;
  private Alarm compoundAlarm;
  private Alarm alarm1;
  private Alarm alarm2;
  private Alarm alarm3;

  @BeforeClass
  protected void setupClass() throws Exception {
    // This test won't work without the real mysql database so use mini-mon.
    // Warning, this will truncate your mini-mon database
    db = new DBI("jdbc:mysql://192.168.10.4:3306/mon?connectTimeout=5000&autoReconnect=true&useLegacyDatetimeCode=false", "monapi", "password");

    handle = db.open();
    /*
    handle
        .execute(Resources.toString(getClass().getResource("alarm.sql"), Charset.defaultCharset()));
        */
    repo = new AlarmMySqlRepoImpl(db, new PersistUtils());

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
    handle.execute("truncate table sub_alarm");
    handle.execute("truncate table alarm_action");
    handle.execute("truncate table alarm_definition");
    handle.execute("truncate table alarm_metric");
    handle.execute("truncate table metric_definition");
    handle.execute("truncate table metric_definition_dimensions");
    handle.execute("truncate table metric_dimension");

    DateTime timestamp1 = ISO_8601_FORMATTER.parseDateTime("2015-03-14T09:26:53").withZoneRetainFields(DateTimeZone.forID("UTC"));
    DateTime timestamp2 = ISO_8601_FORMATTER.parseDateTime("2015-03-14T09:26:54").withZoneRetainFields(DateTimeZone.forID("UTC"));
    DateTime timestamp3 = ISO_8601_FORMATTER.parseDateTime("2015-03-14T09:26:55").withZoneRetainFields(DateTimeZone.forID("UTC"));

    handle
        .execute(
            "insert into alarm_definition (id, tenant_id, name, severity, expression, match_by, actions_enabled, created_at, updated_at, deleted_at) "
            + "values ('1', 'bob', '90% CPU', 'LOW', 'avg(cpu.idle_perc{flavor_id=777, image_id=888, device=1}) > 10', 'flavor_id,image_id', 1, NOW(), NOW(), NULL)");
    handle
        .execute("insert into alarm (id, alarm_definition_id, state, lifecycle_state, link, created_at, updated_at, state_updated_at) values ('1', '1', 'OK', 'OPEN', 'http://somesite.com/this-alarm-info', '"+timestamp1.toString().replace('Z', ' ')+"', '"+timestamp1.toString().replace('Z', ' ')+"', '"+timestamp1.toString().replace('Z', ' ')+"')");
    handle
        .execute("insert into alarm (id, alarm_definition_id, state, lifecycle_state, created_at, updated_at, state_updated_at) values ('2', '1', 'UNDETERMINED', 'OPEN', '"+timestamp2.toString().replace('Z', ' ')+"', '"+timestamp2.toString().replace('Z', ' ')+"', '"+timestamp2.toString().replace('Z', ' ')+"')");
    handle
        .execute("insert into alarm (id, alarm_definition_id, state, link, created_at, updated_at, state_updated_at) values ('3', '1', 'ALARM', 'http://somesite.com/this-alarm-info', '"+timestamp3.toString().replace('Z', ' ')+"', '"+timestamp3.toString().replace('Z', ' ')+"', '"+timestamp3.toString().replace('Z', ' ')+"')");
    long subAlarmId = 42;
    for (int alarmId = 1; alarmId <= 3; alarmId++) {
      handle
          .execute("insert into sub_alarm (id, alarm_id, expression, created_at, updated_at) values ('"
              + String.valueOf(subAlarmId++)
              + "', '" + alarmId
              + "', 'avg(cpu.idle_perc{flavor_id=777, image_id=888, device=1}) > 10', NOW(), NOW())");
    }

    handle
        .execute("insert into alarm_metric (alarm_id, metric_definition_dimensions_id) values ('1', 11)");
    handle
        .execute("insert into alarm_metric (alarm_id, metric_definition_dimensions_id) values ('1', 22)");
    handle
        .execute("insert into alarm_metric (alarm_id, metric_definition_dimensions_id) values ('2', 11)");
    handle
        .execute("insert into alarm_metric (alarm_id, metric_definition_dimensions_id) values ('3', 22)");
    handle
        .execute("insert into metric_definition (id, name, tenant_id, region) values (1, 'cpu.idle_perc', 'bob', 'west')");
    handle
        .execute("insert into metric_definition_dimensions (id, metric_definition_id, metric_dimension_set_id) values (11, 1, 1)");
    handle
        .execute("insert into metric_definition_dimensions (id, metric_definition_id, metric_dimension_set_id) values (22, 1, 2)");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (1, 'instance_id', '123')");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (1, 'service', 'monitoring')");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (2, 'flavor_id', '222')");

    alarm1 =
        new Alarm("1", "1", "90% CPU", "LOW", buildAlarmMetrics(
            buildMetricDefinition("cpu.idle_perc", "instance_id", "123", "service", "monitoring"),
            buildMetricDefinition("cpu.idle_perc", "flavor_id", "222")),
                  AlarmState.OK, "OPEN", "http://somesite.com/this-alarm-info", timestamp1, timestamp1, timestamp1);

    alarm2 =
        new Alarm("2", "1", "90% CPU", "LOW", buildAlarmMetrics(
            buildMetricDefinition("cpu.idle_perc", "instance_id", "123", "service", "monitoring")),
                  AlarmState.UNDETERMINED, "OPEN", null, timestamp2, timestamp2, timestamp2);

    alarm3 =
        new Alarm("3", "1", "90% CPU", "LOW", buildAlarmMetrics(
            buildMetricDefinition("cpu.idle_perc", "flavor_id", "222")),
                  AlarmState.ALARM, null, "http://somesite.com/this-alarm-info", timestamp3, timestamp3, timestamp3);

    DateTime timestamp4 = ISO_8601_FORMATTER.parseDateTime("2015-03-15T09:26:53Z");

    handle
        .execute(
        "insert into alarm_definition (id, tenant_id, name, severity, expression, match_by, actions_enabled, created_at, updated_at, deleted_at) "
        + "values ('234', 'bob', '50% CPU', 'HIGH', 'avg(cpu.sys_mem{service=monitoring}) > 20 and avg(cpu.idle_perc{service=monitoring}) < 10', 'hostname,region', 1, NOW(), NOW(), NULL)");
    handle
        .execute("insert into alarm (id, alarm_definition_id, state, created_at, updated_at, state_updated_at) values ('234111', '234', 'UNDETERMINED', '"+timestamp4.toString().replace('Z', ' ')+"', '"+timestamp4.toString().replace('Z', ' ')+"', '"+timestamp4.toString().replace('Z', ' ')+"')");
    handle
        .execute("insert into sub_alarm (id, alarm_id, expression, created_at, updated_at) values ('4343', '234111', 'avg(cpu.sys_mem{service=monitoring}) > 20', NOW(), NOW())");
    handle
        .execute("insert into sub_alarm (id, alarm_id, expression, created_at, updated_at) values ('4242', '234111', 'avg(cpu.idle_perc{service=monitoring}) < 10', NOW(), NOW())");

    handle
        .execute("insert into alarm_metric (alarm_id, metric_definition_dimensions_id) values ('234111', 31)");
    handle
        .execute("insert into alarm_metric (alarm_id, metric_definition_dimensions_id) values ('234111', 32)");
    handle
        .execute("insert into metric_definition (id, name, tenant_id, region) values (111, 'cpu.sys_mem', 'bob', 'west')");
    handle
        .execute("insert into metric_definition (id, name, tenant_id, region) values (112, 'cpu.idle_perc', 'bob', 'west')");
    handle
        .execute("insert into metric_definition_dimensions (id, metric_definition_id, metric_dimension_set_id) values (31, 111, 21)");
    handle
        .execute("insert into metric_definition_dimensions (id, metric_definition_id, metric_dimension_set_id) values (32, 112, 22)");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (21, 'service', 'monitoring')");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (22, 'service', 'monitoring')");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (21, 'hostname', 'roland')");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (22, 'hostname', 'roland')");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (21, 'region', 'colorado')");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (22, 'region', 'colorado')");
    handle
        .execute("insert into metric_dimension (dimension_set_id, name, value) values (22, 'extra', 'vivi')");

    compoundAlarm =
        new Alarm("234111", "234", "50% CPU", "HIGH", buildAlarmMetrics(
            buildMetricDefinition("cpu.sys_mem", "service", "monitoring", "hostname", "roland",
                "region", "colorado"),
            buildMetricDefinition("cpu.idle_perc", "service", "monitoring", "hostname", "roland",
                "region", "colorado", "extra", "vivi")), AlarmState.UNDETERMINED, null, null,
                  timestamp4, timestamp4, timestamp4);
  }

  private List<MetricDefinition> buildAlarmMetrics(final MetricDefinition ... metricDefinitions) {
    return Arrays.asList(metricDefinitions);
  }

  private MetricDefinition buildMetricDefinition(final String metricName,
                                                 final String ... dimensions) {
    final Builder<String, String> builder = ImmutableMap.<String, String>builder();
    for (int i = 0; i < dimensions.length;) {
      builder.put(dimensions[i], dimensions[i+1]);
      i += 2;
    }
    return new MetricDefinition(metricName, builder.build());
  }

  @Test(groups = "database")
  public void shouldDelete() {
    repo.deleteById(TENANT_ID, ALARM_ID);

    List<Map<String, Object>> rows = handle.createQuery("select * from alarm_definition where id='234'").list();
    assertEquals(rows.size(), 1, "Alarm Definition was deleted as well");
  }

  @Test(groups = "database", expectedExceptions=EntityNotFoundException.class)
  public void shouldThowExceptionOnDelete() {
    repo.deleteById(TENANT_ID, "Not an alarm ID");
  }

  @Test(groups = "database")
  public void shouldFindAlarmSubExpressions() {
    final Map<String, AlarmSubExpression> subExpressionMap = repo.findAlarmSubExpressions(ALARM_ID);
    assertEquals(subExpressionMap.size(), 2);
    assertEquals(subExpressionMap.get("4343"),
        AlarmSubExpression.of("avg(cpu.sys_mem{service=monitoring}) > 20"));
    assertEquals(subExpressionMap.get("4242"),
        AlarmSubExpression.of("avg(cpu.idle_perc{service=monitoring}) < 10"));
  }

  @Test(groups = "database")
  public void shouldAlarmSubExpressionsForAlarmDefinition() {
    final Map<String, Map<String, AlarmSubExpression>> alarmSubExpressionMap =
        repo.findAlarmSubExpressionsForAlarmDefinition(alarm1.getAlarmDefinition().getId());
    assertEquals(alarmSubExpressionMap.size(), 3);
    long subAlarmId = 42;
    for (int alarmId = 1; alarmId <= 3; alarmId++) {
      final Map<String, AlarmSubExpression> subExpressionMap =
          alarmSubExpressionMap.get(String.valueOf(alarmId));
      assertEquals(subExpressionMap.get(String.valueOf(subAlarmId)),
          AlarmSubExpression.of("avg(cpu.idle_perc{flavor_id=777, image_id=888, device=1}) > 10"));
      subAlarmId++;
    }
  }
  
  private void checkList(List<Alarm> found, Alarm ... expected) {
    assertEquals(found.size(), expected.length);
    for (Alarm alarm : expected) {
      assertTrue(found.contains(alarm));
    }
  }

  @Test(groups = "database")
  public void shouldFind() {
    checkList(repo.find("Not a tenant id", null, null, null, null, null, null, null, null, null, null, 1, false));

    checkList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, null, null, 1, false), alarm1, alarm2, alarm3, compoundAlarm);

    checkList(repo.find(TENANT_ID, compoundAlarm.getAlarmDefinition().getId(), null, null, null, null, null, null, null, null, null, 1, false), compoundAlarm);

    checkList(repo.find(TENANT_ID, null, "cpu.sys_mem", null, null, null, null, null, null, null, null, 1, false), compoundAlarm);

    checkList(repo.find(TENANT_ID, null, "cpu.idle_perc", null, null, null, null, null, null, null, null, 1, false), alarm1, alarm2, alarm3, compoundAlarm);

    checkList(
        repo.find(TENANT_ID, null, "cpu.idle_perc",
            ImmutableMap.<String, String>builder().put("flavor_id", "222").build(), null, null, null, null, null, null, null, 1, false), alarm1,
        alarm3);

    checkList(
        repo.find(TENANT_ID, null, "cpu.idle_perc",
            ImmutableMap.<String, String>builder().put("service", "monitoring")
                .put("hostname", "roland").build(), null, null, null, null, null, null, null, 1, false), compoundAlarm);

    checkList(repo.find(TENANT_ID, null, null, null, AlarmState.UNDETERMINED, null, null, null, null, null, null, 1, false),
              alarm2,
              compoundAlarm);

    checkList(
        repo.find(TENANT_ID, alarm1.getAlarmDefinition().getId(), "cpu.idle_perc", ImmutableMap
            .<String, String>builder().put("service", "monitoring").build(), null, null, null, null, null, null, null, 1, false), alarm1, alarm2);

    checkList(
        repo.find(TENANT_ID, alarm1.getAlarmDefinition().getId(), "cpu.idle_perc", null, null, null, null, null, null, null, null, 1, false),
        alarm1, alarm2, alarm3);

    checkList(repo.find(TENANT_ID, compoundAlarm.getAlarmDefinition().getId(), null, null,
        AlarmState.UNDETERMINED, null, null, null, null, null, null, 1, false), compoundAlarm);

    checkList(repo.find(TENANT_ID, null, "cpu.sys_mem", null, AlarmState.UNDETERMINED, null, null, null, null, null, null, 1, false),
        compoundAlarm);

    checkList(repo.find(TENANT_ID, null, "cpu.idle_perc", ImmutableMap.<String, String>builder()
        .put("service", "monitoring").build(), AlarmState.UNDETERMINED, null, null, null, null, null, null, 1,false), alarm2, compoundAlarm);

    checkList(repo.find(TENANT_ID, alarm1.getAlarmDefinition().getId(), "cpu.idle_perc",
        ImmutableMap.<String, String>builder().put("service", "monitoring").build(),
        AlarmState.UNDETERMINED, null, null, null, null, null, null, 1, false), alarm2);

    checkList(repo.find(TENANT_ID, null, null, null, null, null, null, null, DateTime.now(DateTimeZone.forID("UTC")), null, null, 0, false));

    checkList(repo.find(TENANT_ID, null, null, null, null, null, null, null, ISO_8601_FORMATTER.parseDateTime("2015-03-15T00:00:00Z"), null, null, 0, false), compoundAlarm);

    checkList(
        repo.find(TENANT_ID, null, null, null, null, null, null, null, ISO_8601_FORMATTER.parseDateTime("2015-03-14T00:00:00Z"), null, null,
                  1, false), alarm1, alarm2, alarm3, compoundAlarm);

    checkList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, Arrays.asList("state","severity"), null, 1, false),
              alarm1, alarm2, compoundAlarm, alarm3);

    checkList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, Arrays.asList("state desc","severity"), null, 1, false),
              compoundAlarm, alarm3, alarm2, alarm1);

    checkList(repo.find(TENANT_ID, null, null, null, null, Arrays.asList(AlarmSeverity.HIGH), null, null, null, null, null, 1, false),
              compoundAlarm);
  }

  private DateTime getAlarmStateUpdatedDate(final String alarmId) {
    final List<Map<String, Object>> rows =
        handle.createQuery("select state_updated_at from alarm where id = :alarmId")
            .bind("alarmId", alarmId).list();
    final Object state_updated_at = rows.get(0).get("state_updated_at");
    return (new DateTime(((Timestamp)state_updated_at).getTime(), DateTimeZone.forID("UTC")));
  }

  private DateTime getAlarmUpdatedDate(final String alarmId) {
    final List<Map<String, Object>> rows =
        handle.createQuery("select updated_at from alarm where id = :alarmId")
            .bind("alarmId", alarmId).list();
    final Object state_updated_at = rows.get(0).get("updated_at");
    return (new DateTime(((Timestamp)state_updated_at).getTime(), DateTimeZone.forID("UTC")));
  }

  @Test(groups = "database")
  public void shouldUpdate() throws InterruptedException {
    final Alarm originalAlarm = repo.findById(TENANT_ID, ALARM_ID);
    final DateTime originalStateUpdatedAt = getAlarmStateUpdatedDate(ALARM_ID);
    final DateTime originalUpdatedAt = getAlarmUpdatedDate(ALARM_ID);
    assertEquals(originalAlarm.getState(), AlarmState.UNDETERMINED);

    Thread.sleep(1000);
    final Alarm newAlarm = repo.update(TENANT_ID, ALARM_ID, AlarmState.OK, null, null);
    final DateTime newStateUpdatedAt = getAlarmStateUpdatedDate(ALARM_ID);
    final DateTime newUpdatedAt = getAlarmUpdatedDate(ALARM_ID);
    assertNotEquals(newStateUpdatedAt.getMillis(), originalStateUpdatedAt.getMillis(),
                    "state_updated_at did not change");
    assertNotEquals(newUpdatedAt.getMillis(), originalUpdatedAt.getMillis(),
                    "updated_at did not change");

    assertEquals(newAlarm, originalAlarm);

    newAlarm.setState(AlarmState.OK);
    newAlarm.setStateUpdatedTimestamp(newStateUpdatedAt);
    newAlarm.setUpdatedTimestamp(newUpdatedAt);

    // Make sure it was updated in the DB
    assertEquals(repo.findById(TENANT_ID, ALARM_ID), newAlarm);

    Thread.sleep(1000);
    final Alarm unchangedAlarm = repo.update(TENANT_ID, ALARM_ID, AlarmState.OK, "OPEN", null);
    assertTrue(getAlarmStateUpdatedDate(ALARM_ID).equals(newStateUpdatedAt), "state_updated_at did change");
    assertNotEquals(getAlarmUpdatedDate(ALARM_ID).getMillis(), newStateUpdatedAt, "updated_at did not change");
    assertEquals(unchangedAlarm, newAlarm);
  }

  @Test(groups = "database", expectedExceptions=EntityNotFoundException.class)
  public void shouldUpdateThrowException() {

    repo.update(TENANT_ID, "Not a valid alarm id", AlarmState.UNDETERMINED, null, null);
  }

  @Test(groups = "database")
  public void shouldFindById() {

    final Alarm alarm = repo.findById(TENANT_ID, compoundAlarm.getId());

    assertEquals(alarm, compoundAlarm);
  }

  @Test(groups = "database", expectedExceptions=EntityNotFoundException.class)
  public void shouldFindByIdThrowException() {

    repo.findById(TENANT_ID, "Not a valid alarm id");
  }
}
