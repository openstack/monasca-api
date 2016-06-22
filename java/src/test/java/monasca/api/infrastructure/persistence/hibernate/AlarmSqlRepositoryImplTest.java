/*
 * Copyright 2015 FUJITSU LIMITED
 * Copyright 2016 Hewlett Packard Enterprise Development Company, L.P.
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.alarm.AlarmRepo;
import monasca.common.hibernate.db.AlarmDb;
import monasca.common.hibernate.db.AlarmDefinitionDb;
import monasca.common.hibernate.db.AlarmMetricDb;
import monasca.common.hibernate.db.MetricDefinitionDb;
import monasca.common.hibernate.db.MetricDefinitionDimensionsDb;
import monasca.common.hibernate.db.MetricDimensionDb;
import monasca.common.hibernate.db.SubAlarmDb;
import monasca.common.hibernate.db.SubAlarmDefinitionDb;
import monasca.common.model.alarm.AlarmOperator;
import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;

@Test(groups = "orm")
public class AlarmSqlRepositoryImplTest {
  private static final String TENANT_ID = "bob";
  private static final String ALARM_ID = "234111";
  private static final DateTimeFormatter ISO_8601_FORMATTER = ISODateTimeFormat.dateOptionalTimeParser().withZoneUTC();
  private static final DateTimeZone UTC_TIMEZONE = DateTimeZone.forID("UTC");
  private SessionFactory sessionFactory;
  private AlarmRepo repo;
  private Alarm compoundAlarm;
  private Alarm alarm1;
  private Alarm alarm2;
  private Alarm alarm3;
  private Transaction tx;

  @BeforeMethod
  protected void setupClass() throws Exception {
    this.sessionFactory = HibernateUtil.getSessionFactory();
    this.repo = new AlarmSqlRepoImpl(this.sessionFactory);
    this.prepareData(this.sessionFactory);

    this.tx = this.sessionFactory.openSession().beginTransaction();
  }

  @AfterMethod
  protected void afterMethod() throws Exception {
    this.tx.rollback();

    this.sessionFactory.close();
    this.sessionFactory = null;
  }

  private void prepareData(final SessionFactory sessionFactory) {
    final DateTime now = new DateTime();
    Session session = null;
    try {
      session = sessionFactory.openSession();
      session.beginTransaction();

      DateTime timestamp1 = ISO_8601_FORMATTER.parseDateTime("2015-03-14T09:26:53").withZoneRetainFields(UTC_TIMEZONE);
      DateTime timestamp2 = ISO_8601_FORMATTER.parseDateTime("2015-03-14T09:26:54").withZoneRetainFields(UTC_TIMEZONE);
      DateTime timestamp3 = ISO_8601_FORMATTER.parseDateTime("2015-03-14T09:26:55").withZoneRetainFields(UTC_TIMEZONE);
      DateTime timestamp4 = ISO_8601_FORMATTER.parseDateTime("2015-03-15T09:26:53").withZoneRetainFields(UTC_TIMEZONE);

      final AlarmDefinitionDb alarmDefinition_90Percent = this.newAlarmDefinition(session,
          "1",
          TENANT_ID,
          "90% CPU",
          "avg(cpu.idle_perc{flavor_id=777, image_id=888, device=1}) > 10",
          AlarmSeverity.LOW,
          "flavor_id,image_id",
          true
      );
      final AlarmDefinitionDb alarmDefinition_50Percent = this.newAlarmDefinition(session,
          "234",
          TENANT_ID,
          "50% CPU",
          "avg(cpu.sys_mem{service=monitoring}) > 20 and avg(cpu.idle_perc{service=monitoring}) < 10",
          AlarmSeverity.HIGH,
          "hostname,region",
          true
      );

      final AlarmDb alarmDb_234111 = new AlarmDb(ALARM_ID, alarmDefinition_50Percent, AlarmState.UNDETERMINED, null, null, timestamp4, timestamp4, timestamp4);
      final AlarmDb alarmDb_1 = new AlarmDb("1", alarmDefinition_90Percent, AlarmState.OK, "OPEN", "http://somesite.com/this-alarm-info", timestamp1, timestamp1, timestamp1);
      final AlarmDb alarmDb_2 = new AlarmDb("2", alarmDefinition_90Percent, AlarmState.UNDETERMINED, "OPEN", null, timestamp2, timestamp2, timestamp2);
      final AlarmDb alarmDb_3 = new AlarmDb("3", alarmDefinition_90Percent, AlarmState.ALARM, null, "http://somesite.com/this-alarm-info", timestamp3, timestamp3, timestamp3);

      session.save(alarmDb_1);
      session.save(alarmDb_2);
      session.save(alarmDb_3);
      session.save(alarmDb_234111);

      final List<AlarmDb> alarmDbs = Lists.newArrayList(alarmDb_1, alarmDb_2, alarmDb_3);

      long subAlarmId = 42;
      for (int alarmIndex = 0; alarmIndex < 3; alarmIndex++) {
        final SubAlarmDefinitionDb subExpression = this.newSubAlarmDefinition(session, String.format("%d", alarmIndex + subAlarmId), alarmDefinition_50Percent);
        session.save(
            new SubAlarmDb(
                String.valueOf(subAlarmId++),
                alarmDbs.get(alarmIndex),
                subExpression,
                "avg(cpu.idle_perc{flavor_id=777, image_id=888, device=1}) > 10",
                now,
                now
            )
        );
      }

      final MetricDefinitionDb metricDefinition1 = new MetricDefinitionDb(new byte[]{1}, "cpu.idle_perc", "bob", "west");
      session.save(metricDefinition1);

      final MetricDimensionDb metricDimension1InstanceId = new MetricDimensionDb(new byte[]{1}, "instance_id", "123");
      final MetricDimensionDb metricDimensionService = new MetricDimensionDb(new byte[]{1}, "service", "monitoring");
      final MetricDimensionDb metricDimension2FlavorId = new MetricDimensionDb(new byte[]{2}, "flavor_id", "222");
      session.save(metricDimension1InstanceId);
      session.save(metricDimensionService);
      session.save(metricDimension2FlavorId);

      final MetricDefinitionDimensionsDb metricDefinitionDimensions11 = new MetricDefinitionDimensionsDb(
          new byte[]{1, 1},
          metricDefinition1,
          metricDimension1InstanceId.getId().getDimensionSetId()
      );
      final MetricDefinitionDimensionsDb metricDefinitionDimensions22 = new MetricDefinitionDimensionsDb(
          new byte[]{2, 2},
          metricDefinition1,
          metricDimension2FlavorId.getId().getDimensionSetId()
      );
      session.save(metricDefinitionDimensions11);
      session.save(metricDefinitionDimensions22);

      session.save(new AlarmMetricDb(alarmDbs.get(0), metricDefinitionDimensions11));
      session.save(new AlarmMetricDb(alarmDbs.get(0), metricDefinitionDimensions22));
      session.save(new AlarmMetricDb(alarmDbs.get(1), metricDefinitionDimensions11));
      session.save(new AlarmMetricDb(alarmDbs.get(2), metricDefinitionDimensions22));

      alarm1 =
          new Alarm("1", "1", "90% CPU", "LOW",
              buildAlarmMetrics(
                  buildMetricDefinition("cpu.idle_perc", "instance_id", "123", "service", "monitoring")
                  , buildMetricDefinition("cpu.idle_perc", "flavor_id", "222")
              )
              , AlarmState.OK, "OPEN", "http://somesite.com/this-alarm-info", timestamp1,
              timestamp1, timestamp1);

      alarm2 =
          new Alarm("2", "1", "90% CPU", "LOW", buildAlarmMetrics(buildMetricDefinition("cpu.idle_perc", "instance_id", "123", "service",
              "monitoring")), AlarmState.UNDETERMINED, "OPEN", null, timestamp2, timestamp2, timestamp2);

      alarm3 =
          new Alarm("3", "1", "90% CPU", "LOW", buildAlarmMetrics(buildMetricDefinition("cpu.idle_perc", "flavor_id", "222")), AlarmState.ALARM,
              null, "http://somesite.com/this-alarm-info", timestamp3, timestamp3, timestamp3);


      final SubAlarmDb subAlarmDb1 = new SubAlarmDb("4343", alarmDb_234111, "avg(cpu.sys_mem{service=monitoring}) > 20", now, now);
      final SubAlarmDb subAlarmDb2 = new SubAlarmDb("4242", alarmDb_234111, "avg(cpu.idle_perc{service=monitoring}) < 10", now, now);
      session.save(subAlarmDb1);
      session.save(subAlarmDb2);

      final MetricDefinitionDb metricDefinition111 = new MetricDefinitionDb(new byte[]{1, 1, 1}, "cpu.sys_mem", "bob", "west");
      final MetricDefinitionDb metricDefinition112 = new MetricDefinitionDb(new byte[]{1, 1, 2}, "cpu.idle_perc", "bob", "west");
      session.save(metricDefinition111);
      session.save(metricDefinition112);

      final MetricDefinitionDimensionsDb metricDefinitionDimension31 = new MetricDefinitionDimensionsDb(
          new byte[]{3, 1},
          metricDefinition111,
          new byte[]{2, 1}
      );
      final MetricDefinitionDimensionsDb metricDefinitionDimension32 = new MetricDefinitionDimensionsDb(
          new byte[]{3, 2},
          metricDefinition112,
          new byte[]{2, 2}
      );
      session.save(metricDefinitionDimension31);
      session.save(metricDefinitionDimension32);

      session.save(new AlarmMetricDb(alarmDb_234111, metricDefinitionDimension31));
      session.save(new AlarmMetricDb(alarmDb_234111, metricDefinitionDimension32));

      session.save(new MetricDimensionDb(new byte[]{2, 1}, "service", "monitoring"));
      session.save(new MetricDimensionDb(new byte[]{2, 2}, "service", "monitoring"));
      session.save(new MetricDimensionDb(new byte[]{2, 1}, "hostname", "roland"));
      session.save(new MetricDimensionDb(new byte[]{2, 2}, "hostname", "roland"));
      session.save(new MetricDimensionDb(new byte[]{2, 1}, "region", "colorado"));
      session.save(new MetricDimensionDb(new byte[]{2, 2}, "region", "colorado"));
      session.save(new MetricDimensionDb(new byte[]{2, 2}, "extra", "vivi"));

      session.flush();
      session.getTransaction().commit();

      compoundAlarm =
          new Alarm("234111", "234", "50% CPU", "HIGH", buildAlarmMetrics(
              buildMetricDefinition("cpu.sys_mem", "hostname", "roland", "region", "colorado", "service", "monitoring"),
              buildMetricDefinition("cpu.idle_perc", "extra", "vivi", "hostname", "roland", "region", "colorado", "service", "monitoring")),
              AlarmState.UNDETERMINED, null, null, timestamp4, timestamp4, timestamp4);

    } finally {
      if (session != null) {
        session.close();
      }
    }

  }

  private SubAlarmDefinitionDb newSubAlarmDefinition(final Session session, final String id, final AlarmDefinitionDb alarmDefinition) {
    final DateTime now = DateTime.now();
    final SubAlarmDefinitionDb db = new SubAlarmDefinitionDb(
        id,
        alarmDefinition,
        String.format("f_%s", id),
        String.format("m_%s", id),
        AlarmOperator.GT.toString(),
        0.0,
        1,
        2,
        now,
        now
    );
    session.save(db);
    return db;
  }

  private AlarmDefinitionDb newAlarmDefinition(final Session session,
                                               final String id,
                                               final String tenantId,
                                               final String name,
                                               final String expression,
                                               final AlarmSeverity severity,
                                               final String matchBy,
                                               final boolean actionEnabled) {
    final DateTime now = DateTime.now();
    final AlarmDefinitionDb db = new AlarmDefinitionDb(id, tenantId, name, null, expression, severity, matchBy, actionEnabled, now, now, null);
    session.save(db);
    return db;
  }

  private List<MetricDefinition> buildAlarmMetrics(final MetricDefinition... metricDefinitions) {
    return Arrays.asList(metricDefinitions);
  }

  private MetricDefinition buildMetricDefinition(final String metricName, final String... dimensions) {
    final Builder<String, String> builder = ImmutableMap.builder();
    for (int i = 0; i < dimensions.length; ) {
      builder.put(dimensions[i], dimensions[i + 1]);
      i += 2;
    }
    return new MetricDefinition(metricName, builder.build());
  }

  @Test(groups = "orm")
  @SuppressWarnings("unchecked")
  public void shouldDelete() {
    Session session = null;
    repo.deleteById(TENANT_ID, ALARM_ID);
    try {

      session = sessionFactory.openSession();

      List<AlarmDefinitionDb> rows = session
          .createCriteria(AlarmDefinitionDb.class, "ad")
          .add(Restrictions.eq("ad.id", "234"))
          .setReadOnly(true)
          .list();

      assertEquals(rows.size(), 1, "Alarm Definition was deleted as well");

    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Test(groups = "orm", expectedExceptions = EntityNotFoundException.class)
  public void shouldThowExceptionOnDelete() {
    repo.deleteById(TENANT_ID, "Not an alarm ID");
  }

  @Test(groups = "orm")
  public void shouldFindAlarmSubExpressions() {
    final Map<String, AlarmSubExpression> subExpressionMap = repo.findAlarmSubExpressions(ALARM_ID);
    assertEquals(subExpressionMap.size(), 2);
    assertEquals(subExpressionMap.get("4343"), AlarmSubExpression.of("avg(cpu.sys_mem{service=monitoring}) > 20"));
    assertEquals(subExpressionMap.get("4242"), AlarmSubExpression.of("avg(cpu.idle_perc{service=monitoring}) < 10"));
  }

  @Test(groups = "orm")
  public void shouldAlarmSubExpressionsForAlarmDefinition() {
    final Map<String, Map<String, AlarmSubExpression>> alarmSubExpressionMap =
        repo.findAlarmSubExpressionsForAlarmDefinition(alarm1.getAlarmDefinition().getId());
    assertEquals(alarmSubExpressionMap.size(), 3);
    long subAlarmId = 42;
    for (int alarmId = 1; alarmId <= 3; alarmId++) {
      final Map<String, AlarmSubExpression> subExpressionMap = alarmSubExpressionMap.get(String.valueOf(alarmId));
      assertEquals(subExpressionMap.get(String.valueOf(subAlarmId)),
          AlarmSubExpression.of("avg(cpu.idle_perc{flavor_id=777, image_id=888, device=1}) > 10"));
      subAlarmId++;
    }
  }

  @Test(groups = "orm")
  public void shouldFind() {
    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, null, null, 1, true), alarm1, alarm2);
    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, null, null, 2, true), alarm1, alarm2, alarm3);
    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, null, "1", 1, true), alarm2, alarm3);
    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, null, "2", 1, true), alarm3, compoundAlarm);
    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, null, "3", 1, true), compoundAlarm);

    checkUnsortedList(repo.find("Not a tenant id", null, null, null, null, null, null, null, null, null, null, 1, false));

    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, null, null, 1, false), alarm1, alarm2, alarm3, compoundAlarm);

    checkUnsortedList(repo.find(TENANT_ID, compoundAlarm.getAlarmDefinition().getId(), null, null, null, null, null, null, null, null, null, 1, false), compoundAlarm);

    checkUnsortedList(repo.find(TENANT_ID, null, "cpu.sys_mem", null, null, null, null, null, null, null, null, 1, false), compoundAlarm);

    checkUnsortedList(repo.find(TENANT_ID, null, "cpu.idle_perc", null, null, null, null, null, null, null, null, 1, false), alarm1, alarm2, alarm3, compoundAlarm);

    checkUnsortedList(repo.find(TENANT_ID, null, "cpu.idle_perc", ImmutableMap.<String, String>builder().put("flavor_id", "222").build(), null, null, null, null,
        null, null, null, 1, false), alarm1, alarm3);

    checkUnsortedList(
        repo.find(TENANT_ID, null, "cpu.idle_perc", ImmutableMap.<String, String>builder().put("service", "monitoring").put("hostname", "roland")
            .build(), null, null, null, null, null, null, null, 1, false), compoundAlarm);

    checkUnsortedList(repo.find(TENANT_ID, null, null, null, AlarmState.UNDETERMINED, null, null, null, null, null, null, 1, false), alarm2, compoundAlarm);

    checkUnsortedList(
        repo.find(TENANT_ID, alarm1.getAlarmDefinition().getId(), "cpu.idle_perc", ImmutableMap.<String, String>builder()
            .put("service", "monitoring").build(), null, null, null, null, null, null, null, 1, false), alarm1, alarm2);

    checkUnsortedList(repo.find(TENANT_ID, alarm1.getAlarmDefinition().getId(), "cpu.idle_perc", null, null, null, null, null, null, null, null, 1, false), alarm1,
        alarm2, alarm3);

    checkUnsortedList(
        repo.find(TENANT_ID, compoundAlarm.getAlarmDefinition().getId(), null, null, AlarmState.UNDETERMINED, null, null, null, null, null, null, 1, false),
        compoundAlarm);

    checkUnsortedList(repo.find(TENANT_ID, null, "cpu.sys_mem", null, AlarmState.UNDETERMINED, null, null, null, null, null, null, 1, false), compoundAlarm);

    checkUnsortedList(repo.find(TENANT_ID, null, "cpu.idle_perc", ImmutableMap.<String, String>builder().put("service", "monitoring").build(),
        AlarmState.UNDETERMINED, null, null, null, null, null, null, 1, false), alarm2, compoundAlarm);

    checkUnsortedList(
        repo.find(TENANT_ID, alarm1.getAlarmDefinition().getId(), "cpu.idle_perc", ImmutableMap.<String, String>builder()
            .put("service", "monitoring").build(), AlarmState.UNDETERMINED, null, null, null, null, null, null, 1, false), alarm2);

    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, DateTime.now(UTC_TIMEZONE), null, null, 0, false));

//    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, null, null, ISO_8601_FORMATTER.parseDateTime("2015-03-15T00:00:00Z"), null, 0, false),
//        compoundAlarm);

    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, ISO_8601_FORMATTER.parseDateTime("2015-03-14T00:00:00Z"), null, null, 1, false),
        alarm1, alarm2, alarm3, compoundAlarm);

  }

  @Test(groups = "orm")
  public void shouldFindById() {

    final Alarm alarm = repo.findById(TENANT_ID, compoundAlarm.getId());

    assertEquals(alarm.getId(), compoundAlarm.getId());
    assertEquals(alarm.getAlarmDefinition(), compoundAlarm.getAlarmDefinition());
    assertEquals(alarm.getCreatedTimestamp(), compoundAlarm.getCreatedTimestamp());
    assertEquals(alarm.getStateUpdatedTimestamp(), compoundAlarm.getStateUpdatedTimestamp());
    assertEquals(alarm.getState(), compoundAlarm.getState());
    assertEquals(alarm.getMetrics().size(), compoundAlarm.getMetrics().size());
    assertTrue(CollectionUtils.isEqualCollection(alarm.getMetrics(), compoundAlarm.getMetrics()), "Metrics not equal");
  }

  @Test(groups = "orm", expectedExceptions = EntityNotFoundException.class)
  public void shouldFindByIdThrowException() {

    repo.findById(TENANT_ID, "Not a valid alarm id");
  }

  @Test(groups = "orm")
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

  @Test(groups = "orm", expectedExceptions = EntityNotFoundException.class)
  public void shouldUpdateThrowException() {

    repo.update(TENANT_ID, "Not a valid alarm id", AlarmState.UNDETERMINED, null, null);
  }

  @Test(groups = "orm")
  public void shouldFilterBySeverity() {

    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, Lists.newArrayList(AlarmSeverity.LOW), null, null, null, null, null, 1, false),
        alarm1, alarm2, alarm3);

    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, Lists.newArrayList(AlarmSeverity.HIGH), null, null, null, null, null, 1, false),
        compoundAlarm);

    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, Lists.newArrayList(AlarmSeverity.LOW, AlarmSeverity.HIGH), null, null, null, null, null, 1, false),
        alarm1, alarm2, compoundAlarm, alarm3);

    // no alarms for those severities
    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, Lists.newArrayList(AlarmSeverity.CRITICAL), null, null, null, null, null, 1, false));
    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, Lists.newArrayList(AlarmSeverity.MEDIUM), null, null, null, null, null, 1, false));
    checkUnsortedList(repo.find(TENANT_ID, null, null, null, null, Lists.newArrayList(AlarmSeverity.CRITICAL, AlarmSeverity.MEDIUM), null, null, null, null, null, 1, false));
  }

  @Test(groups = "orm")
  public void shouldSortBy() {
    checkSortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, Lists.newArrayList("state", "severity"), null, 1, false),
        alarm1, alarm2, compoundAlarm, alarm3);
    checkSortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, Lists.newArrayList("state desc", "severity"), null, 1, false),
        alarm3, alarm2, compoundAlarm, alarm1);
    checkSortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, Lists.newArrayList("state desc", "severity asc"), null, 1, false),
        alarm3, alarm2, compoundAlarm, alarm1);
    checkSortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, Lists.newArrayList("state desc", "severity desc"), null, 1, false),
        alarm3, compoundAlarm, alarm2, alarm1);
    checkSortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, Lists.newArrayList("severity"), null, 1, false),
        alarm1, alarm2, alarm3, compoundAlarm);
    checkSortedList(repo.find(TENANT_ID, null, null, null, null, null, null, null, null, Lists.newArrayList("severity desc", "alarm_id desc"), null, 1, false),
        compoundAlarm, alarm3, alarm2, alarm1);
  }

 

  private void checkUnsortedList(List<Alarm> found, Alarm... expected) {
    this.checkUnsortedList(found, false, expected);
  }

  private void checkSortedList(List<Alarm> found, Alarm... expected) {
    this.checkUnsortedList(found, true, expected);
  }

  private void checkUnsortedList(List<Alarm> found, boolean sorted, Alarm... expected) {
    assertEquals(found.size(), expected.length);
    Alarm actual;
    int actualIndex;

    for (int expectedIndex = 0; expectedIndex < expected.length; expectedIndex++) {
      final Alarm alarm = expected[expectedIndex];
      final Optional<Alarm> alarmOptional = FluentIterable
          .from(found)
          .firstMatch(new Predicate<Alarm>() {
            @Override
            public boolean apply(@Nullable final Alarm input) {
              assert input != null;
              return input.getId().equals(alarm.getId());
            }
          });
      assertTrue(alarmOptional.isPresent());

      actual = alarmOptional.get();
      if (sorted) {
        actualIndex = found.indexOf(actual);
        assertEquals(expectedIndex, actualIndex);
      }
      assertEquals(actual, alarm, String.format("%s not equal to %s", actual, alarm));
    }

  }

  private DateTime getAlarmUpdatedDate(final String alarmId) {
    return this.getDateField(alarmId, "updatedAt");
  }

  private DateTime getAlarmStateUpdatedDate(final String alarmId) {
    return this.getDateField(alarmId, "stateUpdatedAt");
  }

  private DateTime getDateField(final String alarmId, final String fieldName) {
    Session session = null;
    DateTime time = null;

    try {
      session = sessionFactory.openSession();
      final String queryString = String.format("select %s from AlarmDb where id = :alarmId", fieldName);
      final List<?> rows = session.createQuery(queryString).setString("alarmId", alarmId).list();

      time = new DateTime(((Timestamp) rows.get(0)).getTime(), UTC_TIMEZONE);
    } finally {
      if (session != null) {
        session.close();
      }
    }

    return time;
  }
}
