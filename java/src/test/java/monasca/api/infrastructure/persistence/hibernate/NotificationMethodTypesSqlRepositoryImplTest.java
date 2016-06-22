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

import java.util.Arrays;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "orm")
public class NotificationMethodTypesSqlRepositoryImplTest {
    NotificationMethodTypesSqlRepoImpl repo = null;
  private SessionFactory sessionFactory;
  private Transaction tx;

  private final static List<String> DEFAULT_NOTIFICATION_METHODS = Arrays.asList("Email", "PagerDuty", "WebHook");

  @BeforeMethod
  protected void beforeMethod() throws Exception {
    this.sessionFactory = HibernateUtil.getSessionFactory();
    this.repo = new NotificationMethodTypesSqlRepoImpl(sessionFactory);

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

      for (String method: DEFAULT_NOTIFICATION_METHODS){
        SQLQuery insertQuery = session.createSQLQuery("INSERT INTO NOTIFICATION_METHOD_TYPE (name) VALUES(?)");
        insertQuery.setParameter(0, method);
        insertQuery.executeUpdate();
      }

      session.getTransaction().commit();

    } finally {
      if (session != null) {
        session.close();
      }
    }
  }



  @Test(groups = "orm")
  public void shouldList() {
    List<String> result  = repo.listNotificationMethodTypes();

    assertEquals(DEFAULT_NOTIFICATION_METHODS, result);
  }

}
