/*
 * Copyright 2015 FUJITSU LIMITED
 * (C) Copyright 2016 Hewlett Packard Enterprise Development LP
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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.joda.time.DateTime;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import monasca.api.domain.exception.EntityExistsException;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.notificationmethod.NotificationMethod;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepo;
import monasca.common.hibernate.db.NotificationMethodDb;
import monasca.common.hibernate.db.NotificationMethodTypesDb;
import monasca.common.model.alarm.AlarmNotificationMethodType;

@Test(groups = "orm")
public class NotificationMethodSqlRepositoryImplTest {
  NotificationMethodRepo repo = null;
  private SessionFactory sessionFactory;
  private Transaction tx;

  private static final String NOTIFICATION_METHOD_EMAIL = "EMAIL";

  @BeforeMethod
  protected void beforeMethod() throws Exception {
    this.sessionFactory = HibernateUtil.getSessionFactory();
    this.repo = new NotificationMethodSqlRepoImpl(sessionFactory);

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
    Session session = null;
    try {
      session = sessionFactory.openSession();

      session.beginTransaction();

      NotificationMethodDb notificationMethodDb1 =
          new NotificationMethodDb("123", "444", "MyEmail", AlarmNotificationMethodType.EMAIL, "a@b", 0, new DateTime(), new DateTime());
      NotificationMethodDb notificationMethodDb2 =
          new NotificationMethodDb("124", "444", "OtherEmail", AlarmNotificationMethodType.EMAIL, "a@b", 0, new DateTime(), new DateTime());

      session.save(notificationMethodDb1);
      session.save(notificationMethodDb2);

      NotificationMethodTypesDb notificationMethodTypeDb1 =
              new NotificationMethodTypesDb("EMAIL");
      NotificationMethodTypesDb notificationMethodTypeDb2 =
              new NotificationMethodTypesDb("WEBHOOK");
      NotificationMethodTypesDb notificationMethodTypeDb3 =
              new NotificationMethodTypesDb("PAGERDUTY");

      session.save(notificationMethodTypeDb1);
      session.save(notificationMethodTypeDb2);
      session.save(notificationMethodTypeDb3);


      session.getTransaction().commit();

    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  @Test(groups = "orm")
  public void shouldCreate() {
    NotificationMethod nmA = repo.create("555", "MyEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0);
    NotificationMethod nmB = repo.findById("555", nmA.getId());

    assertEquals(nmA, nmB);
  }

  @Test(groups = "orm")
  public void shouldExistForTenantAndNotificationMethod() {
    assertTrue(repo.exists("444", "123"));
    assertFalse(repo.exists("444", "1234"));
    assertFalse(repo.exists("333", "123"));
  }

  @Test(groups = "orm")
  public void shouldFind() {
    List<NotificationMethod> nms1 = repo.find("444", null, null, 1);

    assertEquals(nms1, Arrays.asList(new NotificationMethod("123", "MyEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0), new NotificationMethod("124",
        "OtherEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0)));

    List<NotificationMethod> nms2 = repo.find("444", null, "1", 1);

    assertEquals(nms2, Collections.singletonList(new NotificationMethod("124", "OtherEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0)));
  }

  @Test(groups = "orm")
  public void shouldSortBy() {
    // null sorts by will sort by ID
    List<NotificationMethod> nms1 = repo.find("444", null, null, 1);
    assertEquals(nms1, Arrays.asList(new NotificationMethod("123", "MyEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0),
        new NotificationMethod("124", "OtherEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0)));

    List<NotificationMethod> nms2 = repo.find("444", Arrays.asList("name desc", "address"), null, 1);
    assertEquals(nms2, Arrays.asList(new NotificationMethod("124", "OtherEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0),
        new NotificationMethod("123", "MyEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0)));
  }

  @Test(groups = "orm")
  public void shouldUpdate() {
    repo.update("444", "123", "Foo", NOTIFICATION_METHOD_EMAIL, "abc", 0);
    NotificationMethod nm = repo.findById("444", "123");

    assertEquals(nm, new NotificationMethod("123", "Foo", NOTIFICATION_METHOD_EMAIL, "abc", 0));
  }

  @Test(groups = "orm")
  public void shouldUpdateReturnValue() {
    NotificationMethod nm = repo.update("444", "123", "Foo", NOTIFICATION_METHOD_EMAIL, "abc", 0);

    NotificationMethod foundNotificationMethod = repo.findById("444", "123");
    assertEquals(nm, foundNotificationMethod);
  }

  @Test(groups = "orm")
  public void shouldDeleteById() {
    repo.deleteById("444", "123");

    try {
      repo.findById("444", "123");
      fail();
    } catch (EntityNotFoundException ignore) {
    }
  }

  @Test(groups = "orm")
  public void shouldUpdateDuplicateWithSameValues() {
    repo.update("444", "123", "Foo", NOTIFICATION_METHOD_EMAIL, "abc", 0);
    NotificationMethod nm = repo.findById("444", "123");

    assertEquals(nm, new NotificationMethod("123", "Foo", NOTIFICATION_METHOD_EMAIL, "abc", 0));
  }

  @Test(groups = "orm", expectedExceptions = EntityExistsException.class)
  public void shouldNotUpdateDuplicateWithSameName() {

    repo.update("444", "124", "MyEmail", NOTIFICATION_METHOD_EMAIL, "abc", 0);
  }
}
