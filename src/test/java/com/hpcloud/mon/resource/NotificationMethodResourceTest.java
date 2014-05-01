package com.hpcloud.mon.resource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.testng.annotations.Test;

import com.hpcloud.mon.app.command.CreateNotificationMethodCommand;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodType;
import com.hpcloud.mon.resource.exception.ErrorMessages;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

@Test
public class NotificationMethodResourceTest extends AbstractMonApiResourceTest {
  private NotificationMethod notificationMethod;
  private NotificationMethodRepository repo;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();
    notificationMethod = new NotificationMethod("123", "Joe's SMS", NotificationMethodType.SMS,
        "8675309");

    repo = mock(NotificationMethodRepository.class);
    when(repo.create(eq("abc"), eq("MySMS"), eq(NotificationMethodType.SMS), anyString())).thenReturn(
        notificationMethod);
    when(repo.findById(eq("abc"), eq("123"))).thenReturn(notificationMethod);
    when(repo.find(eq("abc"))).thenReturn(Arrays.asList(notificationMethod));

    addResources(new NotificationMethodResource(repo));
  }

  public void shouldCreate() {
    ClientResponse response = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class,
            new CreateNotificationMethodCommand("MySMS", NotificationMethodType.SMS, "8675309"));

    NotificationMethod newNotificationMethod = response.getEntity(NotificationMethod.class);
    String location = response.getHeaders().get("Location").get(0);
    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/notification-methods/" + newNotificationMethod.getId());
    assertEquals(newNotificationMethod, notificationMethod);
    verify(repo).create(eq("abc"), eq("MySMS"), eq(NotificationMethodType.SMS), anyString());
  }

  public void shouldUpdate() {
    when(
        repo.update(eq("abc"), anyString(), anyString(), any(NotificationMethodType.class),
            anyString())).thenReturn(notificationMethod);
    ClientResponse response = client().resource("/v2.0/notification-methods/123")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .put(ClientResponse.class,
            new CreateNotificationMethodCommand("Foo", NotificationMethodType.EMAIL, "a@a.com"));

    assertEquals(response.getStatus(), 200);
    verify(repo).update(eq("abc"), eq("123"), eq("Foo"), eq(NotificationMethodType.EMAIL),
        eq("a@a.com"));
  }

  public void should422OnBadEnum() {
    ClientResponse response = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class, new CreateNotificationMethodCommand("MySMS", null, "8675309"));

    String e = response.getEntity(String.class);
    ErrorMessages.assertThat(e).matches("unprocessable_entity", 422,
        "[type may not be null (was null)]");
  }

  public void should422OnIncorrectAddressFormat() {
    ClientResponse response = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class,
            new CreateNotificationMethodCommand("MySMS", NotificationMethodType.EMAIL, "a@"));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Address a@ is not of correct format");
  }

  public void should422OnIncorrectAddressFormat2() {
    ClientResponse response = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class,
            new CreateNotificationMethodCommand("MySMS", NotificationMethodType.EMAIL, "a@f ,"));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Address a@f , is not of correct format");
  }

  public void should422OnBadAddress() {
    ClientResponse response = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class,
            new CreateNotificationMethodCommand("MySMS", NotificationMethodType.SMS, ""));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "[address may not be empty (was )");
  }

  public void should422OnTooLongName() {
    ClientResponse response = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(
            ClientResponse.class,
            new CreateNotificationMethodCommand(
                "01234567889012345678890123456788901234567889012345678890123456788901234567889012345678890123456788901234567889"
                    + "01234567889012345678890123456788901234567889012345678890123456788901234567889012345678890123456788901234567889"
                    + "01234567889012345678890123456788901234567889012345678890123456788901234567889012345678890123456788901234567889",
                NotificationMethodType.SMS, "a@b"));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "[name size must be between 1 and 250");
  }

  public void should422OnTooLongAddress() {
    ClientResponse response = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(
            ClientResponse.class,
            new CreateNotificationMethodCommand(
                "MySMS",
                NotificationMethodType.SMS,
                "abcdefghi@0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"));

    String e = response.getEntity(String.class);
    ErrorMessages.assertThat(e).matches("unprocessable_entity", 422,
        "[address size must be between 1 and 100");
  }

  public void shouldList() {
    List<NotificationMethod> notificationMethods = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", "abc")
        .get(new GenericType<List<NotificationMethod>>() {
        });

    assertEquals(notificationMethods, Arrays.asList(notificationMethod));
    verify(repo).find(eq("abc"));
  }

  public void shouldGet() {
    assertEquals(client().resource("/v2.0/notification-methods/123")
        .header("X-Tenant-Id", "abc")
        .get(NotificationMethod.class), notificationMethod);
    verify(repo).findById(eq("abc"), eq("123"));
  }

  public void should404OnGetInvalid() {
    doThrow(new EntityNotFoundException("Not Found")).when(repo).findById(anyString(), anyString());

    try {
      client().resource("/v2.0/notification-methods/999")
          .header("X-Tenant-Id", "abc")
          .get(NotificationMethod.class);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("404"));
    }
  }

  public void shouldDelete() {
    ClientResponse response = client().resource("/v2.0/notification-methods/123")
        .header("X-Tenant-Id", "abc")
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
    doThrow(new RuntimeException("")).when(repo).find(anyString());

    try {
      client().resource("/v2.0/notification-methods")
          .header("X-Tenant-Id", "abc")
          .get(new GenericType<List<NotificationMethod>>() {
          });
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("500"));
    }
  }

  public void should422OnCreateInvalid() {
    try {
      client().resource("/v2.0/notification-methods")
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
    ClientResponse response = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class, null);

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "The request entity was empty");
  }
}