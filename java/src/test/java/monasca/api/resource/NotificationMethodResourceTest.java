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

package monasca.api.resource;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

import monasca.api.ApiConfig;
import monasca.api.app.command.CreateNotificationMethodCommand;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.common.Paged;
import monasca.api.domain.model.notificationmethod.NotificationMethod;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepo;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.api.resource.exception.ErrorMessages;

import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@Test
public class NotificationMethodResourceTest extends AbstractMonApiResourceTest {
  private NotificationMethod notificationMethod, notificationMethodWebhook, notificationMethodPagerduty;
  private NotificationMethodRepo repo;
  private ApiConfig config;

  private static final String NOTIFICATION_METHOD_WEBHOOK = "WEBHOOK";
  private static final String NOTIFICATION_METHOD_EMAIL   = "EMAIL";
  private static final String NOTIFICATION_METHOD_PAGERDUTY   = "PAGERDUTY";

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();
    notificationMethod =
        new NotificationMethod("123", "Joe's Email", NOTIFICATION_METHOD_EMAIL, "a@b", 0);
    notificationMethodWebhook =
        new NotificationMethod("1234", "MyWh", NOTIFICATION_METHOD_WEBHOOK, "http://localhost", 60);
    notificationMethodPagerduty =
        new NotificationMethod("12345", "MyPd", NOTIFICATION_METHOD_PAGERDUTY, "nzH2LVRdMzun11HNC2oD", 0);

    repo = mock(NotificationMethodRepo.class);
    when(repo.create(eq("abc"), eq("MyEmail"), eq(NOTIFICATION_METHOD_EMAIL), anyString(), eq(0)))
        .thenReturn(notificationMethod);
    when(repo.create(eq("abc"), eq("MyWh"), eq(NOTIFICATION_METHOD_WEBHOOK), anyString(), anyInt()))
        .thenReturn(notificationMethodWebhook);
    when(repo.create(eq("abc"), eq("MyPd"), eq(NOTIFICATION_METHOD_PAGERDUTY), anyString(), eq(0)))
        .thenReturn(notificationMethodPagerduty);
    when(repo.findById(eq("abc"), eq("123"))).thenReturn(notificationMethod);
    when(repo.find(eq("abc"), (List<String>) anyList(), anyString(), anyInt()))
        .thenReturn(Arrays.asList(notificationMethod));

    config = mock(ApiConfig.class);
    config.validNotificationPeriods = Arrays.asList(0, 60);
    addResources(new NotificationMethodResource(config, repo, new PersistUtils()));
  }

  public void shouldCreate() {
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateNotificationMethodCommand("MyEmail", NOTIFICATION_METHOD_EMAIL, "a@a.com", "0"));

    NotificationMethod newNotificationMethod = response.getEntity(NotificationMethod.class);
    String location = response.getHeaders().get("Location").get(0);
    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/notification-methods/" + newNotificationMethod.getId());
    assertEquals(newNotificationMethod, notificationMethod);
    verify(repo).create(eq("abc"), eq("MyEmail"), eq(NOTIFICATION_METHOD_EMAIL), anyString(), eq(0));
  }

  public void shouldUpdate() {
    when(
        repo.update(eq("abc"), anyString(), anyString(), any(String.class),
            anyString(), eq(0))).thenReturn(notificationMethod);
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods/123")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .put(ClientResponse.class,
                new CreateNotificationMethodCommand("Foo", NOTIFICATION_METHOD_EMAIL, "a@a.com", "0"));

    assertEquals(response.getStatus(), 200);
    verify(repo).update(eq("abc"), eq("123"), eq("Foo"), eq(NOTIFICATION_METHOD_EMAIL),
        eq("a@a.com"), eq(0));
  }

  public void should422OnBadEnum() {
    ClientResponse response =
        client()
            .resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateNotificationMethodCommand("MyEmail", null, "a@b", "0"));

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
                new CreateNotificationMethodCommand("MyEmail", NOTIFICATION_METHOD_EMAIL, "a@", "0"));

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
                new CreateNotificationMethodCommand("MyEmail", NOTIFICATION_METHOD_EMAIL, "a@f ,", "0"));

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
                new CreateNotificationMethodCommand("MyEmail", NOTIFICATION_METHOD_EMAIL, "", "0"));

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
                        NOTIFICATION_METHOD_EMAIL, "a@b", "0"));

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
                    NOTIFICATION_METHOD_EMAIL,
                    "abcdefghi@0123456789012345678901234567890"
                        + "12345678901234567890123456789012345678901234567890"
                        + "12345678901234567890123456789012345678901234567890"
                        + "12345678901234567890123456789012345678901234567890"
                        + "12345678901234567890123456789012345678901234567890"
                        + "12345678901234567890123456789012345678901234567890"
                        + "12345678901234567890123456789012345678901234567890"
                        + "12345678901234567890123456789012345678901234567890"
                        + "12345678901234567890123456789012345678901234567890"
                        + "12345678901234567890123456789012345678901234567890"
                        + "12345678901234567890123456789012345678901234567890"
                        + "123456789012345678901234567890", "0"));

    String e = response.getEntity(String.class);
    ErrorMessages.assertThat(e).matches("unprocessable_entity", 422,
        "[address size must be between 1 and 512");
  }

  public void should422OnNonZeroPeriodForEmail() {
    ClientResponse response =
            client()
                    .resource("/v2.0/notification-methods")
                    .header("X-Tenant-Id", "abc")
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class,
                            new CreateNotificationMethodCommand("MyEmail", NOTIFICATION_METHOD_EMAIL, "a@a.com", "60"));

    String e = response.getEntity(String.class);
    ErrorMessages.assertThat(e).matches("unprocessable_entity", 422,
            "Period can not be non zero for EMAIL");
  }

  public void should422OnNonZeroPeriodForPagerduty() {
    ClientResponse response =
            client().resource("/v2.0/notification-methods")
                    .header("X-Tenant-Id", "abc")
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class,
                            new CreateNotificationMethodCommand("MyPd", NOTIFICATION_METHOD_PAGERDUTY,
                                    "http://localhost", "60"));

    String e = response.getEntity(String.class);
    ErrorMessages.assertThat(e).matches("unprocessable_entity", 422,
            "Period can not be non zero for PAGERDUTY");
  }

  public void should422OnInvalidPeriodForWebhook() {
    ClientResponse response =
            client().resource("/v2.0/notification-methods")
                    .header("X-Tenant-Id", "abc")
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class,
                            new CreateNotificationMethodCommand("MyWh", NOTIFICATION_METHOD_WEBHOOK,
                                    "http://localhost", "5"));

    String e = response.getEntity(String.class);
    ErrorMessages.assertThat(e).matches("unprocessable_entity", 422,
            "5 is not a valid period");
  }

  public void shouldList() {


    Map
        lhm =
        (Map) client().resource("/v2.0/notification-methods").header("X-Tenant-Id", "abc")
            .get(Paged.class).elements.get(0);

    NotificationMethod
        nm =
        new NotificationMethod((String) lhm.get("id"), (String) lhm.get("name"),
                               (String) lhm.get("type"),
                               (String) lhm.get("address"), 0);

    List<NotificationMethod> notificationMethods = Arrays.asList(nm);
    assertEquals(notificationMethods, Arrays.asList(notificationMethod));
    verify(repo).find(eq("abc"), (List<String>) anyList(), anyString(), anyInt());
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
    doThrow(new RuntimeException("")).when(repo).find(anyString(), (List<String>) anyList(),
                                                      anyString(), anyInt());

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
              new CreateNotificationMethodCommand(null, null, "8675309", "0"));
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
                new CreateNotificationMethodCommand("MyWh", NOTIFICATION_METHOD_WEBHOOK,
                    "http://localhost", "0"));

    NotificationMethod newNotificationMethod = response.getEntity(NotificationMethod.class);
    String location = response.getHeaders().get("Location").get(0);

    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/notification-methods/" + newNotificationMethod.getId());
    assertEquals(newNotificationMethod, notificationMethodWebhook);
    verify(repo).create(eq("abc"), eq("MyWh"), eq(NOTIFICATION_METHOD_WEBHOOK), anyString(), eq(0));
  }

  public void shouldCreateWebhookNotificationWithNonZeroPeriod() {
    ClientResponse response =
            client().resource("/v2.0/notification-methods")
                    .header("X-Tenant-Id", "abc")
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class,
                            new CreateNotificationMethodCommand("MyWh", NOTIFICATION_METHOD_WEBHOOK,
                                    "http://localhost", "60"));

    NotificationMethod newNotificationMethod = response.getEntity(NotificationMethod.class);
    String location = response.getHeaders().get("Location").get(0);

    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/notification-methods/" + newNotificationMethod.getId());
    assertEquals(newNotificationMethod, notificationMethodWebhook);
    verify(repo).create(eq("abc"), eq("MyWh"), eq(NOTIFICATION_METHOD_WEBHOOK), anyString(), eq(60));
  }

  public void shouldCreatePagerdutyNotification() {
    ClientResponse response =
        client().resource("/v2.0/notification-methods")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(ClientResponse.class,
                new CreateNotificationMethodCommand("MyPd", NOTIFICATION_METHOD_PAGERDUTY,
                    "http://localhost", "0"));

    NotificationMethod newNotificationMethod = response.getEntity(NotificationMethod.class);
    String location = response.getHeaders().get("Location").get(0);

    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/notification-methods/" + newNotificationMethod.getId());
    assertEquals(newNotificationMethod, notificationMethodPagerduty);
    verify(repo).create(eq("abc"), eq("MyPd"), eq(NOTIFICATION_METHOD_PAGERDUTY), anyString(), eq(0));
  }
}
