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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepo;
import monasca.common.hibernate.db.AlarmActionDb;
import monasca.common.hibernate.db.AlarmDefinitionDb;
import monasca.common.hibernate.db.NotificationMethodDb;
import monasca.common.hibernate.db.SubAlarmDefinitionDb;
import monasca.common.hibernate.db.SubAlarmDefinitionDimensionDb;
import monasca.common.model.alarm.AggregateFunction;
import monasca.common.model.alarm.AlarmNotificationMethodType;
import monasca.common.model.alarm.AlarmOperator;
import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmState;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.metric.MetricDefinition;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nullable;

@Test(groups = "orm")
public class AlarmDefinitionSqlRepositoryImplTest {

  private SessionFactory sessionFactory;
  private AlarmDefinitionRepo repo;
  private AlarmDefinition alarmDef_123;
  private AlarmDefinition alarmDef_234;
  private List<String> alarmActions;
  private Transaction tx;

  @BeforeMethod
  protected void beforeMethod() throws Exception {
    this.sessionFactory = HibernateUtil.getSessionFactory();
    this.repo = new AlarmDefinitionSqlRepoImpl(this.sessionFactory);

    alarmActions = new ArrayList<>();
    alarmActions.add("29387234");
    alarmActions.add("77778687");

    this.prepareData(this.sessionFactory);

    this.tx = this.sessionFactory.openSession().beginTransaction();
  }

  @AfterMethod
  protected void afterMethod() throws Exception {
    this.tx.rollback();

    this.sessionFactory.close();
    this.sessionFactory = null;
  }

  protected void prepareData(final SessionFactory sessionFactory) {

    Session session = sessionFactory.openSession();
    session.beginTransaction();

    final AlarmDefinitionDb alarmDefinition123 = new AlarmDefinitionDb()
        .setTenantId("bob")
        .setName("90% CPU")
        .setSeverity(AlarmSeverity.HIGH)
        .setExpression("avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu, device=1}) > 10")
        .setMatchBy("flavor_id,image_id")
        .setActionsEnabled(true);
    session.save(alarmDefinition123.setId("123"));

    final SubAlarmDefinitionDb subAlarmDefinition111 = new SubAlarmDefinitionDb()
        .setAlarmDefinition(alarmDefinition123)
        .setFunction("avg")
        .setMetricName("hpcs.compute")
        .setOperator(AlarmOperator.GT)
        .setThreshold(10d)
        .setPeriod(60)
        .setPeriods(1);
    session.save(subAlarmDefinition111.setId("111"));

    final SubAlarmDefinitionDimensionDb subAlarmDefinitionDimensionFlavor777 = new SubAlarmDefinitionDimensionDb()
        .setDimensionName("flavor_id")
        .setValue("777");
    final SubAlarmDefinitionDimensionDb subAlarmDefinitionDimensionImageId888 = new SubAlarmDefinitionDimensionDb()
        .setDimensionName("image_id")
        .setValue("888");
    final SubAlarmDefinitionDimensionDb subAlarmDefinitionDimensionFlavorMetricNameCpu = new SubAlarmDefinitionDimensionDb()
        .setDimensionName("metric_name")
        .setValue("cpu");
    final SubAlarmDefinitionDimensionDb subAlarmDefinitionDimensionDevice1 = new SubAlarmDefinitionDimensionDb()
        .setDimensionName("device")
        .setValue("1");

    session.save(subAlarmDefinitionDimensionFlavor777.setSubExpression(subAlarmDefinition111));
    session.save(subAlarmDefinitionDimensionImageId888.setSubExpression(subAlarmDefinition111));
    session.save(subAlarmDefinitionDimensionFlavorMetricNameCpu.setSubExpression(subAlarmDefinition111));
    session.save(subAlarmDefinitionDimensionDevice1.setSubExpression(subAlarmDefinition111));

    final NotificationMethodDb notificationMethodDb29387234 = new NotificationMethodDb()
        .setAddress("root@localhost")
        .setName("root2938723")
        .setTenantId("bob")
        .setType(AlarmNotificationMethodType.EMAIL)
        .setPeriod(60);
    final NotificationMethodDb notificationMethodDb77778687 = new NotificationMethodDb()
        .setAddress("root@localhost")
        .setName("root77778687")
        .setTenantId("bob")
        .setType(AlarmNotificationMethodType.EMAIL)
        .setPeriod(60);

    session.save(notificationMethodDb29387234.setId("29387234"));
    session.save(notificationMethodDb77778687.setId("77778687"));

    final AlarmActionDb alarmAction29387234 = new AlarmActionDb()
        .setActionId("29387234")
        .setAlarmDefinition(alarmDefinition123)
        .setAlarmState(AlarmState.ALARM);
    final AlarmActionDb alarmAction77778687 = new AlarmActionDb()
        .setActionId("77778687")
        .setAlarmDefinition(alarmDefinition123)
        .setAlarmState(AlarmState.ALARM);

    session.save(alarmAction29387234);
    session.save(alarmAction77778687);

    final AlarmDefinitionDb alarmDefinition234 = new AlarmDefinitionDb()
        .setTenantId("bob")
        .setName("50% CPU")
        .setSeverity(AlarmSeverity.LOW)
        .setExpression("avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=mem}) > 20 and avg(hpcs.compute) < 100")
        .setMatchBy("flavor_id,image_id")
        .setActionsEnabled(true);
    session.save(alarmDefinition234.setId("234"));

    final SubAlarmDefinitionDb subAlarmDefinition222 = new SubAlarmDefinitionDb()
        .setAlarmDefinition(alarmDefinition234)
        .setFunction("avg")
        .setMetricName("hpcs.compute")
        .setOperator(AlarmOperator.GT)
        .setThreshold(20d)
        .setPeriod(60)
        .setPeriods(1);
    final SubAlarmDefinitionDb subAlarmDefinition223 = new SubAlarmDefinitionDb()
        .setAlarmDefinition(alarmDefinition234)
        .setFunction("avg")
        .setMetricName("hpcs.compute")
        .setOperator(AlarmOperator.LT)
        .setThreshold(100d)
        .setPeriod(60)
        .setPeriods(1);

    session.save(subAlarmDefinition222.setId("222"));
    session.save(subAlarmDefinition223.setId("223"));

    session.save(
        new SubAlarmDefinitionDimensionDb().setDimensionName("flavor_id").setValue("777").setSubExpression(subAlarmDefinition222)
    );
    session.save(
        new SubAlarmDefinitionDimensionDb().setDimensionName("image_id").setValue("888").setSubExpression(subAlarmDefinition222)
    );
    session.save(
        new SubAlarmDefinitionDimensionDb().setDimensionName("metric_name").setValue("mem").setSubExpression(subAlarmDefinition222)
    );

    session.save(
        new AlarmActionDb().setAlarmDefinition(alarmDefinition234).setAlarmState(AlarmState.ALARM).setActionId("29387234")
    );
    session.save(
        new AlarmActionDb().setAlarmDefinition(alarmDefinition234).setAlarmState(AlarmState.ALARM).setActionId("77778687")
    );

    session.getTransaction().commit();
    session.close();

    alarmDef_123 =
        new AlarmDefinition("123", "90% CPU", null, "HIGH", "avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu, device=1}) > 10",
            Arrays.asList("flavor_id", "image_id"), true, Arrays.asList("29387234", "77778687"), Collections.<String>emptyList(),
            Collections.<String>emptyList());
    alarmDef_234 =
        new AlarmDefinition("234", "50% CPU", null, "LOW",
            "avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=mem}) > 20 and avg(hpcs.compute) < 100",
            Arrays.asList("flavor_id", "image_id"), true, Arrays.asList("29387234", "77778687"), Collections.<String>emptyList(),
            Collections.<String>emptyList());

  }

  @Test(groups = "orm")
  public void shouldCreate() {
    Session session = null;

    long subAlarmDimensionSize;
    long subAlarmSize;

    Map<String, AlarmSubExpression> subExpressions =
        ImmutableMap.<String, AlarmSubExpression>builder()
            .put("4433", AlarmSubExpression.of("avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu}) > 10")).build();

    AlarmDefinition alarmA =
        repo.create("555", "2345", "90% CPU", null, "LOW", "avg(hpcs.compute{flavor_id=777, image_id=888, metric_name=cpu}) > 10", subExpressions,
            Arrays.asList("flavor_id", "image_id"), alarmActions, null, null);
    AlarmDefinition alarmB = repo.findById("555", alarmA.getId());

    assertEquals(alarmA.getId(), alarmB.getId());
    assertEquals(alarmA.getName(), alarmB.getName());
    assertEquals(alarmA.getAlarmActions().size(), alarmB.getAlarmActions().size());

    for (String alarmAction : alarmA.getAlarmActions()) {
      assertTrue(alarmB.getAlarmActions().contains(alarmAction));
    }

    // Assert that sub-alarm and sub-alarm-dimensions made it to the db
    try {
      session = sessionFactory.openSession();

      subAlarmSize = (Long) session
          .createCriteria(SubAlarmDefinitionDb.class)
          .add(Restrictions.eq("id", "4433"))
          .setProjection(Projections.rowCount())
          .uniqueResult();

      subAlarmDimensionSize = (Long) session.createCriteria(SubAlarmDefinitionDimensionDb.class)
          .add(Restrictions.eq("subAlarmDefinitionDimensionId.subExpression.id", "4433"))
          .setProjection(Projections.rowCount())
          .uniqueResult();

    } finally {
      if (session != null) {
        session.close();
      }
    }
    assertEquals(subAlarmSize, (long) 1);
    assertEquals(subAlarmDimensionSize, (long) 3);
  }

  @Test(groups = "orm")
  public void shouldUpdate() {

    List<String> oldSubAlarmIds = Arrays.asList("222");
    AlarmSubExpression changedSubExpression = AlarmSubExpression.of("avg(hpcs.compute) <= 200");
    Map<String, AlarmSubExpression> changedSubExpressions =
        ImmutableMap.<String, AlarmSubExpression>builder().put("223", changedSubExpression).build();
    AlarmSubExpression newSubExpression = AlarmSubExpression.of("avg(foo{flavor_id=777}) > 333");
    Map<String, AlarmSubExpression> newSubExpressions = ImmutableMap.<String, AlarmSubExpression>builder().put("555", newSubExpression).build();

    repo.update("bob", "234", false, "90% CPU", null, "avg(foo{flavor_id=777}) > 333 and avg(hpcs.compute) <= 200",
        Arrays.asList("flavor_id", "image_id"), "LOW", false, oldSubAlarmIds, changedSubExpressions, newSubExpressions, alarmActions, null, null);

    AlarmDefinition alarm = repo.findById("bob", "234");
    AlarmDefinition expected =
        new AlarmDefinition("234", "90% CPU", null, "LOW", "avg(foo{flavor_id=777}) > 333 and avg(hpcs.compute) <= 200", Arrays.asList("flavor_id",
            "image_id"), false, alarmActions, Collections.<String>emptyList(), Collections.<String>emptyList());

    assertEquals(expected.getId(), alarm.getId());
    assertEquals(expected.getName(), alarm.getName());
    assertEquals(expected.getExpressionData(), alarm.getExpressionData());
    assertEquals(expected.getAlarmActions().size(), alarm.getAlarmActions().size());
    for (String alarmAction : expected.getAlarmActions()) {
      assertTrue(alarm.getAlarmActions().contains(alarmAction));
    }

    Map<String, AlarmSubExpression> subExpressions = repo.findSubExpressions("234");
    assertEquals(subExpressions.get("223"), changedSubExpression);
    assertEquals(subExpressions.get("555"), newSubExpression);
  }

  @Test(groups = "orm")
  public void shouldFindById() {
    Session session = null;
    AlarmDefinition alarmDef_123_repo = repo.findById("bob", "123");
    assertEquals(alarmDef_123.getDescription(), alarmDef_123_repo.getDescription());
    assertEquals(alarmDef_123.getExpression(), alarmDef_123_repo.getExpression());
    assertEquals(alarmDef_123.getExpressionData(), alarmDef_123_repo.getExpressionData());
    assertEquals(alarmDef_123.getName(), alarmDef_123_repo.getName());
    // Make sure it still finds AlarmDefinitions with no notifications
    try {
      session = sessionFactory.openSession();

      session.createQuery("delete from AlarmActionDb").executeUpdate();

    } finally {
      if (session != null) {
        session.close();
      }
    }
    alarmDef_123.setAlarmActions(new ArrayList<String>(0));
    assertEquals(alarmDef_123, repo.findById("bob", "123"));
  }

  @Test(groups = "orm")
  public void shouldFindSubAlarmMetricDefinitions() {

    assertEquals(repo.findSubAlarmMetricDefinitions("123").get("111"), new MetricDefinition("hpcs.compute", ImmutableMap.<String, String>builder()
        .put("flavor_id", "777").put("image_id", "888").put("metric_name", "cpu").put("device", "1").build()));

    assertEquals(repo.findSubAlarmMetricDefinitions("234").get("222"), new MetricDefinition("hpcs.compute", ImmutableMap.<String, String>builder()
        .put("flavor_id", "777").put("image_id", "888").put("metric_name", "mem").build()));

    assertTrue(repo.findSubAlarmMetricDefinitions("asdfasdf").isEmpty());
  }

  @Test(groups = "orm")
  public void shouldFindSubExpressions() {

    assertEquals(repo.findSubExpressions("123").get("111"), new AlarmSubExpression(AggregateFunction.AVG, new MetricDefinition("hpcs.compute",
        ImmutableMap.<String, String>builder().put("flavor_id", "777").put("image_id", "888").put("metric_name", "cpu").put("device", "1").build()),
        AlarmOperator.GT, 10, 60, 1));

    assertEquals(repo.findSubExpressions("234").get("223"), new AlarmSubExpression(AggregateFunction.AVG, new MetricDefinition("hpcs.compute",
        new HashMap<String, String>()), AlarmOperator.LT, 100, 60, 1));

    assertTrue(repo.findSubAlarmMetricDefinitions("asdfasdf").isEmpty());
  }

  @Test(groups = "orm")
  public void testExists() {
    assertEquals(repo.exists("bob", "90% CPU"), "123");

    // Negative
    assertNull(repo.exists("bob", "999% CPU"));
  }

  @Test(groups = "orm")
  public void shouldDeleteById() {
    repo.deleteById("bob", "123");

    try {
      assertNull(repo.findById("bob", "123"));
      fail();
    } catch (EntityNotFoundException expected) {
    }
    assertEquals(Arrays.asList(alarmDef_234), repo.find("bob", null, null, null, null, null, 1));
  }

  public void shouldFindByDimension() {
    final Map<String, String> dimensions = new HashMap<>();
    dimensions.put("image_id", "888");

    List<AlarmDefinition> result = repo.find("bob", null, dimensions, null, null, null, 1);

    assertEquals(Arrays.asList(alarmDef_123, alarmDef_234), result);

    dimensions.clear();
    dimensions.put("device", "1");
    assertEquals(Arrays.asList(alarmDef_123), repo.find("bob", null, dimensions, null, null, null, 1));

    dimensions.clear();
    dimensions.put("Not real", "AA");
    assertEquals(0, repo.find("bob", null, dimensions, null, null, null, 1).size());
  }

  public void shouldFindByName() {
    final Map<String, String> dimensions = new HashMap<>();
    dimensions.put("image_id", "888");

    List<AlarmDefinition> result = repo.find("bob", "90% CPU", dimensions, null, null, null, 1);

    assertEquals(Arrays.asList(alarmDef_123), result);

  }

  public void shouldSortBy() {
    // null sorts by will sort by ID
    this.checkList(repo.find("bob", null, null, null, null, null, 0),
        this.alarmDef_123, this.alarmDef_234);
    this.checkList(repo.find("bob", null, null, null, Arrays.asList("severity"), null, 0),
        this.alarmDef_123, this.alarmDef_234);
    this.checkList(repo.find("bob", null, null, null, Arrays.asList("state", "severity"), null, 0),
        this.alarmDef_234, this.alarmDef_123);
    this.checkList(repo.find("bob", null, null, null, Arrays.asList("name", "state", "severity"), null, 0),
        this.alarmDef_234, this.alarmDef_123);
  }

  public void shouldFilterBySeverity() {
    checkList(repo.find("bob", null, null, Lists.newArrayList(AlarmSeverity.HIGH), null, null, 1),
        this.alarmDef_123);
    checkList(repo.find("bob", null, null, Lists.newArrayList(AlarmSeverity.LOW), null, null, 1),
        this.alarmDef_234);
    checkList(repo.find("bob", null, null, Lists.newArrayList(AlarmSeverity.HIGH, AlarmSeverity.LOW), null, null, 1),
        this.alarmDef_123, this.alarmDef_234);
  }

  public void shouldFindWithOffset() {
    // create more alarm definition for this test
    final AlarmDefinition localAd1 = new AlarmDefinition("999", "60% CPU", null, "LOW",
        "avg(hpcs.compute{flavor_id=888, image_id=888, metric_name=mem}) > 20 and avg(hpcs.compute) < 100",
        Arrays.asList("flavor_id", "image_id"), true, Arrays.asList("29387234", "77778687"), Collections.<String>emptyList(),
        Collections.<String>emptyList());
    final AlarmDefinition localAd2 = new AlarmDefinition("9999", "70% CPU", null, "LOW",
        "avg(hpcs.compute{flavor_id=999, image_id=888, metric_name=mem}) > 20 and avg(hpcs.compute) < 99",
        Arrays.asList("flavor_id", "image_id"), true, Arrays.asList("29387234", "77778687"), Collections.<String>emptyList(),
        Collections.<String>emptyList());
    final AlarmDefinition localAd3 = new AlarmDefinition("99999", "80% CPU", null, "LOW",
        "avg(hpcs.compute{flavor_id=1111, image_id=888, metric_name=mem}) > 20 and avg(hpcs.compute) < 88",
        Arrays.asList("flavor_id", "image_id"), true, Arrays.asList("29387234", "77778687"), Collections.<String>emptyList(),
        Collections.<String>emptyList());

    final Session session = sessionFactory.openSession();
    session.beginTransaction();

    for (final AlarmDefinition ad : Lists.newArrayList(localAd1, localAd2, localAd3)) {
      final AlarmDefinitionDb adDb = new AlarmDefinitionDb()
          .setTenantId("bob")
          .setName(ad.getName())
          .setSeverity(AlarmSeverity.valueOf(ad.getSeverity()))
          .setExpression(ad.getExpression())
          .setMatchBy(Joiner.on(",").join(ad.getMatchBy()))
          .setActionsEnabled(ad.isActionsEnabled());

      session.save(adDb.setId(ad.getId()));

      for (final String alarmActionId : ad.getAlarmActions()) {
        session.save(new AlarmActionDb()
            .setActionId(alarmActionId)
            .setAlarmDefinition(adDb)
            .setAlarmState(AlarmState.ALARM)
        );
      }
    }
    session.getTransaction().commit();
    session.close();

    // run tests
    checkList(repo.find("bob", null, null, null, null, null, 1), this.alarmDef_123, this.alarmDef_234);
    checkList(repo.find("bob", null, null, null, null, "1", 1), this.alarmDef_234, localAd1);
    checkList(repo.find("bob", null, null, null, null, "2", 1), localAd1, localAd2);
    checkList(repo.find("bob", null, null, null, null, "3", 1), localAd2, localAd3);
    checkList(repo.find("bob", null, null, null, null, "4", 1), localAd3);
    checkList(repo.find("bob", null, null, null, null, "5", 1));

    checkList(repo.find("bob", null, null, null, null, null, 0),
        this.alarmDef_123, this.alarmDef_234, localAd1, localAd2, localAd3);
    checkList(repo.find("bob", null, null, null, null, null, 6),
        this.alarmDef_123, this.alarmDef_234, localAd1, localAd2, localAd3);
    checkList(repo.find("bob", null, null, null, null, "2", 3),
        localAd1, localAd2, localAd3);
  }

  private void checkList(List<AlarmDefinition> found, AlarmDefinition... expected) {
    assertEquals(found.size(), expected.length);
    AlarmDefinition actual;

    for (final AlarmDefinition alarmDefinition : expected) {
      final Optional<AlarmDefinition> alarmOptional = FluentIterable
          .from(found)
          .firstMatch(new Predicate<AlarmDefinition>() {
            @Override
            public boolean apply(@Nullable final AlarmDefinition input) {
              assert input != null;
              return input.getId().equals(alarmDefinition.getId());
            }
          });
      assertTrue(alarmOptional.isPresent());

      actual = alarmOptional.get();
      assertEquals(actual, alarmDefinition, String.format("%s not equal to %s", actual, alarmDefinition));
    }

  }

}
