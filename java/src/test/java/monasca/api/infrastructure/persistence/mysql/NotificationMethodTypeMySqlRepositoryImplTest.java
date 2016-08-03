/*
 * (C) Copyright 2014-2016 Hewlett Packard Enterprise Development LP
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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Resources;

import monasca.api.infrastructure.persistence.PersistUtils;

@Test
public class NotificationMethodTypeMySqlRepositoryImplTest {
  private DBI db;
  private Handle handle;
  private NotificationMethodTypesMySqlRepoImpl repo;

  private final static List<String> DEFAULT_NOTIFICATION_METHODS = Arrays.asList("Email", "PagerDuty", "WebHook");

  @BeforeClass
  protected void beforeClass() throws Exception {
    db = new DBI("jdbc:h2:mem:test;MODE=MySQL");
    handle = db.open();
    handle.execute(Resources.toString(getClass().getResource("notification_method_type.sql"),
        Charset.defaultCharset()));
    repo = new NotificationMethodTypesMySqlRepoImpl(db, new PersistUtils());
  }

  @AfterClass
  protected void afterClass() {
    handle.close();
  }

  @BeforeMethod
  protected void beforeMethod() {
    handle.execute("truncate table notification_method_type");
    createNotificationMethodTypes();
  }


  private void createNotificationMethodTypes() {
    try (Handle h = db.open()) {
      h.begin();
      for (String methodType : DEFAULT_NOTIFICATION_METHODS){
         h.insert("insert into notification_method_type (name) values (?)", methodType);
      }
      h.commit();
    }
  }

  public void shouldListNotificationMethodTypes() {
       List<String>  notification_method_types = repo.listNotificationMethodTypes();
       assertEquals(notification_method_types, DEFAULT_NOTIFICATION_METHODS);
  }

}
