/*
 * Copyright 2015 FUJITSU LIMITED
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

import static monasca.api.infrastructure.persistence.hibernate.TestHelper.randomByteArray;
import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Maps;
import monasca.common.hibernate.db.AlarmDb;
import monasca.common.hibernate.db.AlarmDefinitionDb;
import monasca.common.hibernate.db.AlarmMetricDb;
import monasca.common.hibernate.db.MetricDefinitionDb;
import monasca.common.hibernate.db.MetricDefinitionDimensionsDb;
import monasca.common.hibernate.db.MetricDimensionDb;
import monasca.common.model.alarm.AlarmSeverity;
import monasca.common.model.alarm.AlarmState;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "orm")
public class AlarmHibernateUtilsTest {

  private static final DateTimeFormatter ISO_8601_FORMATTER = ISODateTimeFormat.dateOptionalTimeParser().withZoneUTC();
  private static final String LUK_TENANT_ID = "luk";
  private static final String BOB_TENANT_ID = "bob";
  private static final String ALARM_DEF_NAME = "90%";
  private static final String ALARM_DEF_EXPRESSION = "avg(cpu.idle_perc{flavor_id=777, image_id=888, device=1}) > 10";
  private static final String ALARM_MATCH_BY = "flavor_id,image_id";
  private static final int BINARY_KEY_LENGTH = 20;
  private AlarmHibernateUtils repo;
  private SessionFactory sessionFactory;
  private Transaction tx;

  @BeforeMethod
  protected void beforeMethod() {
    this.sessionFactory = HibernateUtil.getSessionFactory();
    this.prepareData(this.sessionFactory);
    this.repo = new AlarmHibernateUtils(sessionFactory);

    this.tx = this.sessionFactory.openSession().beginTransaction();
  }

  @AfterMethod
  protected void afterMethod() throws Exception {
    this.tx.rollback();

    this.sessionFactory.close();
    this.sessionFactory = null;
  }

  private void prepareData(final SessionFactory sessionFactory) {
    Session session = sessionFactory.openSession();

    session.beginTransaction();

    DateTime timestamp1 = ISO_8601_FORMATTER.parseDateTime("2015-03-14T09:26:53");

    final AlarmDefinitionDb alarmDefinitionBob = this.newAlarmDefinition(session, "1", BOB_TENANT_ID);
    final AlarmDefinitionDb alarmDefinitionLuk = this.newAlarmDefinition(session, "2", LUK_TENANT_ID);
    session.save(alarmDefinitionBob);
    session.save(alarmDefinitionLuk);

    final AlarmDb alarmDb1 = new AlarmDb("1", alarmDefinitionBob, AlarmState.OK, "OPEN", "http://somesite.com/this-alarm-info", timestamp1, timestamp1, timestamp1);
    final AlarmDb alarmDb2 = new AlarmDb("2", alarmDefinitionLuk, AlarmState.OK, "OPEN", "http://somesite.com/this-alarm-info", timestamp1, timestamp1, timestamp1);
    session.save(alarmDb1);
    session.save(alarmDb2);

    final MetricDefinitionDb md1 = new MetricDefinitionDb(new byte[]{1}, "metric", BOB_TENANT_ID, "eu");
    session.save(md1);

    final MetricDimensionDb mDim1Instance = new MetricDimensionDb(randomByteArray(BINARY_KEY_LENGTH), "instance_id", "123");
    final MetricDimensionDb mDim1Service = new MetricDimensionDb(randomByteArray(BINARY_KEY_LENGTH), "service", "monitoring");
    final MetricDimensionDb mDim2Flavor = new MetricDimensionDb(randomByteArray(BINARY_KEY_LENGTH), "flavor_id", "222");
    session.save(mDim1Instance);
    session.save(mDim1Service);
    session.save(mDim2Flavor);

    final MetricDefinitionDimensionsDb mdd11 = new MetricDefinitionDimensionsDb(randomByteArray(BINARY_KEY_LENGTH), md1, mDim1Instance.getId().getDimensionSetId());
    final MetricDefinitionDimensionsDb mdd22 = new MetricDefinitionDimensionsDb(randomByteArray(BINARY_KEY_LENGTH), md1, mDim2Flavor.getId().getDimensionSetId());
    session.save(mdd11);
    session.save(mdd22);

    session.save(new AlarmMetricDb(alarmDb1, mdd11));
    session.save(new AlarmMetricDb(alarmDb1, mdd22));
    session.save(new AlarmMetricDb(alarmDb2, mdd11));

    session.getTransaction().commit();
    session.close();
  }

  private AlarmDefinitionDb newAlarmDefinition(final Session session,
                                               final String id,
                                               final String tenantId) {
    final String str = "AlarmDefinition" + 1;
    final DateTime now = DateTime.now();
    final AlarmDefinitionDb definition = new AlarmDefinitionDb(id, tenantId, ALARM_DEF_NAME, str, ALARM_DEF_EXPRESSION, AlarmSeverity.LOW, ALARM_MATCH_BY, true, now, now, null);
    session.save(definition);
    return definition;
  }

  public void testNullArguments() {

    List<String> result = repo.findAlarmIds(null, null);

    assertEquals(result.size(), 0, "No alarms");
  }

  public void testWithTenantIdNoExist() {

    List<String> result = repo.findAlarmIds("fake_id", null);

    assertEquals(result.size(), 0, "No alarms");
  }

  public void testWithTenantId() {

    List<String> result = repo.findAlarmIds(BOB_TENANT_ID, new HashMap<String, String>());

    assertEquals(result.size(), 1, "Alarm found");
    assertEquals(result.get(0), "1", "Alarm with id 1 found");

    result = repo.findAlarmIds(LUK_TENANT_ID, new HashMap<String, String>());
    assertEquals(result.size(), 1, "Alarm found");
    assertEquals(result.get(0), "2", "Alarm with id 2 found");
  }

  public void testWithDimensions() {

    Map<String, String> dimensions = Maps.newHashMap();
    dimensions.put("flavor_id", "222");

    List<String> result = repo.findAlarmIds(BOB_TENANT_ID, dimensions);

    assertEquals(result.size(), 1, "Alarm found");
    assertEquals(result.get(0), "1", "Alarm with id 1 found");
  }

  public void testWithNotExixtingDimensions() {

    Map<String, String> dimensions = Maps.newHashMap();
    dimensions.put("a", "b");

    List<String> result = repo.findAlarmIds(BOB_TENANT_ID, dimensions);

    assertEquals(result.size(), 0, "Alarm not found");
  }
}
