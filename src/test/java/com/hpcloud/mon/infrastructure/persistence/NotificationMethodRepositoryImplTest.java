package com.hpcloud.mon.infrastructure.persistence;

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
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodType;

@Test
public class NotificationMethodRepositoryImplTest {
  private DBI db;
  private Handle handle;
  private NotificationMethodRepositoryImpl repo;

  @BeforeClass
  protected void beforeClass() throws Exception {
    db = new DBI("jdbc:h2:mem:test;MODE=MySQL");
    handle = db.open();
    handle.execute(Resources.toString(getClass().getResource("notification_method.sql"),
        Charset.defaultCharset()));
    repo = new NotificationMethodRepositoryImpl(db);
  }

  @AfterClass
  protected void afterClass() {
    handle.close();
  }

  @BeforeMethod
  protected void beforeMethod() {
    handle.execute("truncate table notification_method");
    handle.execute("insert into notification_method (id, tenant_id, name, type, address, created_at, updated_at) values ('123', '444', 'MySMS', 'SMS', '8675309', NOW(), NOW())");
  }

  public void shouldCreate() {
    NotificationMethod nmA = repo.create("555", "MySMS", NotificationMethodType.SMS, "5555555");
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
    assertEquals(nm.getType(), NotificationMethodType.SMS);
    assertEquals(nm.getAddress(), "8675309");
  }

  public void shouldFind() {
    List<NotificationMethod> nms = repo.find("444");

    assertEquals(
        nms,
        Arrays.asList(new NotificationMethod("123", "MySMS", NotificationMethodType.SMS, "8675309")));
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
