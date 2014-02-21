package com.hpcloud.mon.integration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.MonApiModule;
import com.hpcloud.mon.app.command.CreateNotificationMethodCommand;
import com.hpcloud.mon.app.representation.NotificationMethodRepresentation;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod.NotificationMethodType;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;
import com.hpcloud.mon.infrastructure.persistence.NotificationMethodRepositoryImpl;
import com.hpcloud.mon.resource.AbstractMonApiResourceTest;
import com.hpcloud.mon.resource.NotificationMethodResource;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @author Jonathan Halterman
 */
@Test(groups = "integration")
public class NotificationMethodIntegrationTest extends AbstractMonApiResourceTest {
  private static final String TENANT_ID = "notification-method-test";
  private DBI db;
  private NotificationMethod notificationMethod;
  private NotificationMethodRepository repo;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();
    Handle handle = db.open();
    handle.execute("truncate table notification_method");
    handle.execute("insert into notification_method (id, tenant_id, name, type, address, created_at, updated_at) values ('29387234', 'notification-method-test', 'MySMS', 'SMS', '8675309', NOW(), NOW())");
    db.close(handle);

    repo = new NotificationMethodRepositoryImpl(db);
    addResources(new NotificationMethodResource(repo));
  }

  @BeforeTest
  protected void beforeTest() throws Exception {
    MonApiConfiguration config = getConfiguration("config-test.yml", MonApiConfiguration.class);
    Injector injector = Guice.createInjector(new MonApiModule(environment, config));
    db = injector.getInstance(DBI.class);
    Handle handle = db.open();
    handle.execute(Resources.toString(
        NotificationMethodRepositoryImpl.class.getResource("notification_method.sql"),
        Charset.defaultCharset()));
    handle.close();

    // Fixtures
    notificationMethod = new NotificationMethod("123", "Joe's SMS", NotificationMethodType.SMS,
        "8675309");
  }

  public void shouldCreate() throws Exception {
    ClientResponse response = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", TENANT_ID)
        .type(MediaType.APPLICATION_JSON)
        .post(
            ClientResponse.class,
            new CreateNotificationMethodCommand(notificationMethod.getName(),
                notificationMethod.getType(), notificationMethod.getAddress()));
    NotificationMethod newNotificationMethod = response.getEntity(NotificationMethodRepresentation.class).notificationMethod;
    String location = response.getHeaders().get("Location").get(0);

    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/notification-methods/" + newNotificationMethod.getId());
    assertEquals(newNotificationMethod.getName(), notificationMethod.getName());
    assertEquals(newNotificationMethod.getAddress(), notificationMethod.getAddress());
    assertEquals(repo.findById(TENANT_ID, newNotificationMethod.getId()), newNotificationMethod);
  }

  public void shouldConflict() throws Exception {
    ClientResponse response = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", TENANT_ID)
        .type(MediaType.APPLICATION_JSON)
        .post(ClientResponse.class,
            new CreateNotificationMethodCommand("MySMS", NotificationMethodType.SMS, "8675309"));

    assertEquals(response.getStatus(), 409);
  }

  public void shouldDelete() {
    NotificationMethod newMethod = repo.create(TENANT_ID, notificationMethod.getName(),
        notificationMethod.getType(), notificationMethod.getAddress());
    assertNotNull(repo.findById(TENANT_ID, newMethod.getId()));

    ClientResponse response = client().resource("/v2.0/notification-methods/" + newMethod.getId())
        .header("X-Tenant-Id", TENANT_ID)
        .delete(ClientResponse.class);
    assertEquals(response.getStatus(), 204);

    try {
      assertNull(repo.findById(TENANT_ID, newMethod.getId()));
      fail();
    } catch (EntityNotFoundException expected) {
    }
  }
}
