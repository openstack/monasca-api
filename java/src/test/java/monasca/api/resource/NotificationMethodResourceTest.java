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

package monasca.api.resource;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import monasca.api.app.command.CreateNotificationMethodCommand;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.common.Link;
import monasca.api.domain.model.common.Paged;
import monasca.api.domain.model.notificationmethod.NotificationMethod;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepo;
import monasca.api.domain.model.notificationmethod.NotificationMethodType;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.api.resource.exception.ErrorMessages;

import org.mockito.internal.matchers.Not;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Test
public class NotificationMethodResourceTest extends AbstractMonApiResourceTest {
  private NotificationMethod notificationMethod, notificationMethodWebhook, notificationMethodPagerduty;
  private NotificationMethodRepo repo;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();
    notificationMethod =
        new NotificationMethod("123", "Joe's Email", NotificationMethodType.EMAIL, "a@b");
    notificationMethodWebhook =
        new NotificationMethod("1234", "MyWh", NotificationMethodType.WEBHOOK, "http://localhost");
    notificationMethodPagerduty =
        new NotificationMethod("12345", "MyPd", NotificationMethodType.PAGERDUTY, "nzH2LVRdMzun11HNC2oD");

    repo = mock(NotificationMethodRepo.class);
    when(repo.create(eq("abc"), eq("MyEmail"), eq(NotificationMethodType.EMAIL), anyString()))
        .thenReturn(notificationMethod);
    when(repo.create(eq("abc"), eq("MyWh"), eq(NotificationMethodType.WEBHOOK), anyString()))
        .thenReturn(notificationMethodWebhook);
    when(repo.create(eq("abc"), eq("MyPd"), eq(NotificationMethodType.PAGERDUTY), anyString()))
        .thenReturn(notificationMethodPagerduty);
    when(repo.findById(eq("abc"), eq("123"))).thenReturn(notificationMethod);
    when(repo.find(eq("abc"), anyString(), anyInt())).thenReturn(Arrays.asList(notificationMethod));

    addResources(new NotificationMethodResource(repo, new PersistUtils()));
  }

  public void shouldCreate() {
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "a@a.com"));

    NotificationMethod newNotificationMethod = response.getEntity(NotificationMethod.class);
    String location = response.getHeaders().get("Location").get(0);
    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/notification-methods/" + newNotificationMethod.getId());
    assertEquals(newNotificationMethod, notificationMethod);
    verify(repo).create(eq("abc"), eq("MyEmail"), eq(NotificationMethodType.EMAIL), anyString());
  }

  public void shouldUpdate() {
    when(
        repo.update(eq("abc"), anyString(), anyString(), any(NotificationMethodType.class),
            anyString())).thenReturn(notificationMethod);
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods/123")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .put(ClientResponse.class,
                new CreateNotificationMethodCommand("Foo", NotificationMethodType.EMAIL, "a@a.com"));

    assertEquals(response.getStatus(), 200);
    verify(repo).update(eq("abc"), eq("123"), eq("Foo"), eq(NotificationMethodType.EMAIL),
        eq("a@a.com"));
  }

  public void should422OnBadEnum() {
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateNotificationMethodCommand("MyEmail", null, "a@b"));

    String e = response.getEntity(String.class);
    ErrorMessages.assertThat(e).matches("unprocessable_entity", 422,
        "[type may not be null (was null)]");
  }

  public void should422OnIncorrectAddressFormat() {
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "a@"));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Address a@ is not of correct format");
  }

  public void should422OnIncorrectAddressFormat2() {
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "a@f ,"));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Address a@f , is not of correct format");
  }

  public void should422OnBadAddress() {
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, ""));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "[address may not be empty (was )");
  }

  public void should422OnTooLongName() {
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(
                ClientResponse.class,
                new CreateNotificationMethodCommand(
                    "01234567889012345678890123456788901234567889012345678890123456788901234567889012345678890123456788901234567889"
                        + "01234567889012345678890123456788901234567889012345678890123456788901234567889012345678890123456788901234567889"
                        + "01234567889012345678890123456788901234567889012345678890123456788901234567889012345678890123456788901234567889",
                    NotificationMethodType.EMAIL, "a@b"));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "[name size must be between 1 and 250");
  }

  public void should422OnTooLongAddress() {
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(
                ClientResponse.class,
                new CreateNotificationMethodCommand(
                    "MyEmail",
                    NotificationMethodType.EMAIL,
                    "abcdefghi@0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"));

    String e = response.getEntity(String.class);
    ErrorMessages.assertThat(e).matches("unprocessable_entity", 422,
        "[address size must be between 1 and 100");
  }

  public void shouldList() {


    Map
        lhm =
        (Map) client().resource("/v2.0/notification-methods").header("X-Tenant-Id", "abc")
            .get(Paged.class).elements.get(0);

    NotificationMethod
        nm =
        new NotificationMethod((String) lhm.get("id"), (String) lhm.get("name"),
                               NotificationMethodType.fromJson((String) lhm.get("type")),
                               (String) lhm.get("address"));

    List<NotificationMethod> notificationMethods = Arrays.asList(nm);
    assertEquals(notificationMethods, Arrays.asList(notificationMethod));
    verify(repo).find(eq("abc"), anyString(), anyInt());
  }

  public void shouldGet() {
    assertEquals(client().resource("/v2.0/notification-methods/123").header("X-Tenant-Id", "abc")
        .get(NotificationMethod.class), notificationMethod);
    verify(repo).findById(eq("abc"), eq("123"));
  }

  public void should404OnGetInvalid() {
    doThrow(new EntityNotFoundException("Not Found")).when(repo).findById(anyString(), anyString());

    try {
      client().resource("/v2.0/notification-methods/999").header("X-Tenant-Id", "abc")
          .get(NotificationMethod.class);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("404"));
    }
  }

  public void shouldDelete() {
    ClientResponse response =
        client().resource("/v2.0/notification-methods/123").header("X-Tenant-Id", "abc")
            .delete(ClientResponse.class);
    assertEquals(response.getStatus(), 204);
    verify(repo).deleteById(eq("abc"), eq("123"));
  }

  public void should404OnDeleteInvalid() {
    doThrow(new EntityNotFoundException("Not Found")).when(repo).deleteById(anyString(),
        anyString());

    try {
      client().resource("/v2.0/notification-methods/999").header("X-Tenant-Id", "abc").delete();
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("404"));
    }
  }

  public void should500OnInternalException() {
    doThrow(new RuntimeException("")).when(repo).find(anyString(), anyString(), anyInt());

    try {
      client().resource("/v2.0/notification-methods").header("X-Tenant-Id", "abc")
          .get(new GenericType<List<NotificationMethod>>() {});
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("500"));
    }
  }

  public void should422OnCreateInvalid() {
    try {
      client()
          .resource("/v2.0/notification-methods")
          .header("X-Tenant-Id", "abc")
          .header("Content-Type", MediaType.APPLICATION_JSON)
          .post(NotificationMethod.class,
              new CreateNotificationMethodCommand(null, null, "8675309"));
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("422"));
    }
  }

  public void shouldFailNullInput() {
    ClientResponse response =
        client().resource("/v2.0/notification-methods").header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON).post(ClientResponse.class, null);

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "The request entity was empty");
  }

  public void shouldCreateWebhookNotification() {
    ClientResponse response =
        client().resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateNotificationMethodCommand("MyWh", NotificationMethodType.WEBHOOK,
                    "http://localhost"));

    NotificationMethod newNotificationMethod = response.getEntity(NotificationMethod.class);
    String location = response.getHeaders().get("Location").get(0);

    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/notification-methods/" + newNotificationMethod.getId());
    assertEquals(newNotificationMethod, notificationMethodWebhook);
    verify(repo).create(eq("abc"), eq("MyWh"), eq(NotificationMethodType.WEBHOOK), anyString());
  }

  public void shouldCreatePagerdutyNotification() {
    ClientResponse response =
        client().resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateNotificationMethodCommand("MyPd", NotificationMethodType.PAGERDUTY,
                    "http://localhost"));

    NotificationMethod newNotificationMethod = response.getEntity(NotificationMethod.class);
    String location = response.getHeaders().get("Location").get(0);

    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/notification-methods/" + newNotificationMethod.getId());
    assertEquals(newNotificationMethod, notificationMethodPagerduty);
    verify(repo).create(eq("abc"), eq("MyPd"), eq(NotificationMethodType.PAGERDUTY), anyString());
  }
}
