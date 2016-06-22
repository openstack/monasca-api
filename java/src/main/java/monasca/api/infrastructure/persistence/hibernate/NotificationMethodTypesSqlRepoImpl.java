/*
 *
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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import monasca.api.domain.model.notificationmethod.NotificationMethodTypesRepo;
import monasca.common.hibernate.db.NotificationMethodTypesDb;

/**
 * Notification method repository implementation.
 */
public class NotificationMethodTypesSqlRepoImpl
    extends BaseSqlRepo
    implements NotificationMethodTypesRepo {
  private static final Logger LOG = LoggerFactory.getLogger(NotificationMethodTypesSqlRepoImpl.class);

  @Inject
  public NotificationMethodTypesSqlRepoImpl(@Named("orm") SessionFactory sessionFactory) {
    super(sessionFactory);
  }


  @Override
  @SuppressWarnings("unchecked")
  public List<String> listNotificationMethodTypes() {

    Session session = null;
    List<String> notification_method_types = new ArrayList<String>();

    try {
      session = sessionFactory.openSession();
      //Query q = session.createSQLQuery("Select * from notification_method_type").addEntity(String.class);
      Query q = session.createQuery("from NotificationMethodTypesDb");

      List<NotificationMethodTypesDb> resultList = q.list();
      for (NotificationMethodTypesDb type : resultList){
          notification_method_types.add(type.getName());
      }
      return notification_method_types;

    } finally {
      if (session != null) {
        session.close();
      }
    }

  }

 }
