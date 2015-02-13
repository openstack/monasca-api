/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

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
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.notificationmethod.NotificationMethod;
import monasca.api.domain.model.notificationmethod.NotificationMethodType;

@Test
public class NotificationMethodMySqlRepositoryImplTest {
  private DBI db;
  private Handle handle;
  private NotificationMethodMySqlRepoImpl repo;

  @BeforeClass
  protected void beforeClass() throws Exception {
    db = new DBI("jdbc:h2:mem:test;MODE=MySQL");
    handle = db.open();
    handle.execute(Resources.toString(getClass().getResource("notification_method.sql"),
        Charset.defaultCharset()));
    repo = new NotificationMethodMySqlRepoImpl(db);
  }

  @AfterClass
  protected void afterClass() {
    handle.close();
  }

  @BeforeMethod
  protected void beforeMethod() {
    handle.execute("truncate table notification_method");
    handle
        .execute("insert into notification_method (id, tenant_id, name, type, address, created_at, updated_at) values ('123', '444', 'MyEmail', 'EMAIL', 'a@b', NOW(), NOW())");
  }

  public void shouldCreate() {
    NotificationMethod nmA = repo.create("555", "MyEmail", NotificationMethodType.EMAIL, "a@b");
    NotificationMethod nmB = repo.findById("555", nmA.getId());

    assertEquals(nmA, nmB);
  }

  public void shouldExistForTenantAndNotificationMethod() {
    assertTrue(repo.exists("444", "123"));
    assertFalse(repo.exists("444", "1234"));
    assertFalse(repo.exists("333", "123"));
  }

  public void shouldFindById() {
    NotificationMethod nm = repo.findById("444", "123");

    assertEquals(nm.getId(), "123");
    assertEquals(nm.getType(), NotificationMethodType.EMAIL);
    assertEquals(nm.getAddress(), "a@b");
  }

  public void shouldFind() {
    List<NotificationMethod> nms = repo.find("444", null);

    assertEquals(nms, Arrays.asList(new NotificationMethod("123", "MyEmail",
        NotificationMethodType.EMAIL, "a@b")));
  }

  public void shouldUpdate() {
    repo.update("444", "123", "Foo", NotificationMethodType.EMAIL, "abc");
    NotificationMethod nm = repo.findById("444", "123");

    assertEquals(nm, new NotificationMethod("123", "Foo", NotificationMethodType.EMAIL, "abc"));
  }

  public void shouldDeleteById() {
    repo.deleteById("444", "123");

    try {
      repo.findById("444", "123");
      fail();
    } catch (EntityNotFoundException expected) {
    }
  }
}
