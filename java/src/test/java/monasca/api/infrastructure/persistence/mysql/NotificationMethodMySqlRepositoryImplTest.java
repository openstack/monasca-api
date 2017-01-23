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

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import monasca.api.domain.exception.EntityExistsException;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.notificationmethod.NotificationMethod;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Resources;
import monasca.api.infrastructure.persistence.PersistUtils;

@Test
public class NotificationMethodMySqlRepositoryImplTest {
  private DBI db;
  private Handle handle;
  private NotificationMethodMySqlRepoImpl repo;
  Handle realHandle;
  Handle spyHandle;
  private boolean shouldRollback = false;

  private static final String NOTIFICATION_METHOD_WEBHOOK = "WEBHOOK";
  private static final String NOTIFICATION_METHOD_EMAIL   = "EMAIL";
  private static final String NOTIFICATION_METHOD_PAGERDUTY   = "PAGERDUTY";

  @BeforeClass
  protected void beforeClass() throws Exception {
    db = new DBI("jdbc:h2:mem:test;MODE=MySQL");
    handle = db.open();
    handle.execute(Resources.toString(getClass().getResource("notification_method.sql"),
        Charset.defaultCharset()));
    handle.execute(Resources.toString(getClass().getResource("notification_method_type.sql"),
            Charset.defaultCharset()));
    handle
    .execute("insert into notification_method_type ( name) values ('EMAIL')");
    handle
    .execute("insert into notification_method_type ( name) values ('PAGERDUTY')");
    handle
    .execute("insert into notification_method_type ( name) values ('WEBHOOK')");
    final DBI mockDb = mock(DBI.class);
    when(mockDb.open()).thenAnswer(new Answer<Handle>() {
      public Handle answer(InvocationOnMock invocation) {
        realHandle = db.open();
        spyHandle = spy(realHandle);
        // Ensure there is no active transaction when the handle is closed.
        // Have to do this in the close() method because calling isInTransaction()
        // after the close throws an exception
        doAnswer(new Answer<Void>() {
          public Void answer(InvocationOnMock invocation) {
            assertFalse(spyHandle.isInTransaction());
            realHandle.close();
            return null;
          }
        }).when(spyHandle).close();
        return spyHandle;
      }
    });
    repo = new NotificationMethodMySqlRepoImpl(mockDb, new PersistUtils());
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
    handle
    .execute("insert into notification_method (id, tenant_id, name, type, address, created_at, updated_at) values ('124', '444', 'OtherEmail', 'EMAIL', 'a@b', NOW(), NOW())");
    shouldRollback = false;
  }

  @AfterMethod
  protected void afterMethod() {
    if (shouldRollback) {
      verify(spyHandle, times(1)).rollback();
    }
  }

  public void shouldCreateEmail() {
    NotificationMethod nmA = repo.create("555", "MyEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0);
    verify(spyHandle, times(1)).commit();
    NotificationMethod nmB = repo.findById("555", nmA.getId());

    assertEquals(nmA, nmB);
  }

  @Test(expectedExceptions = EntityExistsException.class)
  public void shouldNotCreateDuplicateEmail() {
    shouldRollback = true;
   repo.create("444", "MyEmail", NOTIFICATION_METHOD_EMAIL, "c@d", 0);
  }

  public void shouldCreateWebhookNonZeroPeriod() {
    NotificationMethod nmA = repo.create("555", "MyWebhook", NOTIFICATION_METHOD_WEBHOOK, "http://localhost:33", 60);
    verify(spyHandle, times(1)).commit();
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
    assertEquals(nm.getType(), NOTIFICATION_METHOD_EMAIL);
    assertEquals(nm.getAddress(), "a@b");
  }

  public void shouldFind() {
    List<NotificationMethod> nms1 = repo.find("444", null, null, 1);

    assertEquals(nms1, Arrays.asList(new NotificationMethod("123", "MyEmail",
        NOTIFICATION_METHOD_EMAIL, "a@b", 0),new NotificationMethod("124", "OtherEmail",
            NOTIFICATION_METHOD_EMAIL, "a@b", 0)));

    List<NotificationMethod> nms2 = repo.find("444", null, "1", 1);

    assertEquals(nms2, Arrays.asList(new NotificationMethod("124", "OtherEmail",
        NOTIFICATION_METHOD_EMAIL, "a@b", 0)));
  }

  public void shouldSortBy() {
    // null sorts by will sort by ID
    List<NotificationMethod> nms1 = repo.find("444", null, null, 1);
    assertEquals(nms1, Arrays.asList(new NotificationMethod("123", "MyEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0),
        new NotificationMethod("124", "OtherEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0)));

    List<NotificationMethod> nms2 = repo.find("444", Arrays.asList("name desc", "address"), null, 1);
    assertEquals(nms2, Arrays.asList(new NotificationMethod("124", "OtherEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0),
        new NotificationMethod("123", "MyEmail", NOTIFICATION_METHOD_EMAIL, "a@b", 0)));
  }

  public void shouldUpdate() {
    repo.update("444", "123", "Foo", NOTIFICATION_METHOD_EMAIL, "abc", 0);
    verify(spyHandle, times(1)).commit();
    NotificationMethod nm = repo.findById("444", "123");

    assertEquals(nm, new NotificationMethod("123", "Foo", NOTIFICATION_METHOD_EMAIL, "abc", 0));
  }

  public void shouldUpdateWebhookWithNonZeroPeriod() {
    NotificationMethod nmOriginal = repo.create("555", "MyWebhook", NOTIFICATION_METHOD_WEBHOOK, "http://localhost:33", 0);
    repo.update("555", nmOriginal.getId(), "MyWebhook", NOTIFICATION_METHOD_WEBHOOK, "http://localhost:33", 60);
    verify(spyHandle, times(1)).commit();
    NotificationMethod nmUpdated = repo.findById("555", nmOriginal.getId());

    assertEquals(nmUpdated.getPeriod(), 60);
  }

  public void shouldDeleteById() {
    repo.deleteById("444", "123");

    try {
      repo.findById("444", "123");
      fail();
    } catch (EntityNotFoundException expected) {
    }
  }

  public void shouldUpdateDuplicateWithSameValues() {
      repo.update("444", "123", "Foo", NOTIFICATION_METHOD_EMAIL, "abc", 0);
      verify(spyHandle, times(1)).commit();
      NotificationMethod nm = repo.findById("444", "123");

      assertEquals(nm, new NotificationMethod("123", "Foo", NOTIFICATION_METHOD_EMAIL, "abc", 0));
    }

  @Test(expectedExceptions = EntityExistsException.class)
  public void shouldNotUpdateDuplicateWithSameName() {
      shouldRollback = true;
      repo.update("444", "124", "MyEmail", NOTIFICATION_METHOD_EMAIL, "abc", 0);
    }
}
