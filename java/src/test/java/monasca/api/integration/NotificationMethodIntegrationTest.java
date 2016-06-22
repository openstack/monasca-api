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

package monasca.api.integration;

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
import monasca.api.ApiConfig;
import monasca.api.MonApiModule;
import monasca.api.app.command.CreateNotificationMethodCommand;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.notificationmethod.NotificationMethod;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepo;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.api.infrastructure.persistence.mysql.NotificationMethodMySqlRepoImpl;
import monasca.api.resource.AbstractMonApiResourceTest;
import monasca.api.resource.NotificationMethodResource;
import com.sun.jersey.api.client.ClientResponse;

@Test(groups = "integration")
public class NotificationMethodIntegrationTest extends AbstractMonApiResourceTest {
  private static final String TENANT_ID = "notification-method-test";
  private DBI db;
  private NotificationMethod notificationMethod;
  private NotificationMethodRepo repo;
  private ApiConfig config;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();
    Handle handle = db.open();
    handle.execute("truncate table notification_method");
    handle
        .execute("insert into notification_method (id, tenant_id, name, type, address, created_at, updated_at) values ('29387234', 'notification-method-test', 'MyEmaila', 'EMAIL', 'a@b', NOW(), NOW())");
    db.close(handle);
    repo = new NotificationMethodMySqlRepoImpl(db, new PersistUtils());
    addResources(new NotificationMethodResource(config, repo, new PersistUtils()));
  }

  @BeforeTest
  protected void beforeTest() throws Exception {
    ApiConfig config = getConfiguration("config-test.yml", ApiConfig.class);
    Injector injector = Guice.createInjector(new MonApiModule(environment, config));
    db = injector.getInstance(DBI.class);
    Handle handle = db.open();
    handle.execute(Resources.toString(
        NotificationMethodMySqlRepoImpl.class.getResource("notification_method.sql"),
        Charset.defaultCharset()));
    handle.close();

    // Fixtures
    notificationMethod =
        new NotificationMethod("123", "Joe's Email", "EMAIL", "a@b", 0);
  }

  public void shouldCreate() throws Exception {
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", TENANT_ID)
            .type(MediaType.APPLICATION_JSON)
            .post(
                ClientResponse.class,
                new CreateNotificationMethodCommand(notificationMethod.getName(),
                    notificationMethod.getType(), notificationMethod.getAddress(), "0"));
    NotificationMethod newNotificationMethod = response.getEntity(NotificationMethod.class);
    String location = response.getHeaders().get("Location").get(0);

    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/notification-methods/" + newNotificationMethod.getId());
    assertEquals(newNotificationMethod.getName(), notificationMethod.getName());
    assertEquals(newNotificationMethod.getAddress(), notificationMethod.getAddress());
    assertEquals(repo.findById(TENANT_ID, newNotificationMethod.getId()), newNotificationMethod);
  }

  public void shouldConflict() throws Exception {
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", TENANT_ID)
            .type(MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateNotificationMethodCommand("MyEmail", "EMAIL", "a@b", "0"));

    assertEquals(response.getStatus(), 409);
  }

  public void shouldDelete() {
    NotificationMethod newMethod =
        repo.create(TENANT_ID, notificationMethod.getName(), notificationMethod.getType(),
            notificationMethod.getAddress(), 0);
    assertNotNull(repo.findById(TENANT_ID, newMethod.getId()));

    ClientResponse response =
        client().resource("/v2.0/notification-methods/" + newMethod.getId())
            .header("X-Tenant-Id", TENANT_ID).delete(ClientResponse.class);
    assertEquals(response.getStatus(), 204);

    try {
      assertNull(repo.findById(TENANT_ID, newMethod.getId()));
      fail();
    } catch (EntityNotFoundException expected) {
    }
  }
}
