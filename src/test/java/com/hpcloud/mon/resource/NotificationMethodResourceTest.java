package com.hpcloud.mon.resource;

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

import javax.ws.rs.core.MediaType;

import org.testng.annotations.Test;

import com.hpcloud.mon.app.command.CreateNotificationMethodCommand;
import com.hpcloud.mon.app.representation.NotificationMethodRepresentation;
import com.hpcloud.mon.app.representation.NotificationMethodsRepresentation;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod.NotificationMethodType;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;
import com.hpcloud.mon.resource.exception.ErrorMessages;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @author Jonathan Halterman
 */
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

    NotificationMethod newNotificationMethod = response.getEntity(NotificationMethodRepresentation.class).notificationMethod;
    String location = response.getHeaders().get("Location").get(0);
    assertEquals(response.getStatus(), 201);
    assertEquals(location, "/v2.0/notification-methods/" + newNotificationMethod.getId());
    assertEquals(newNotificationMethod, notificationMethod);
    verify(repo).create(eq("abc"), eq("MySMS"), eq(NotificationMethodType.SMS), anyString());
  }

  public void should422OnBadEnum() {
    ClientResponse response = client().resource("/v2.0/notification-methods")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class, new CreateNotificationMethodCommand("MySMS", null, "8675309"));

    String e = response.getEntity(String.class);
    ErrorMessages.assertThat(e).matches("unprocessable_entity", 422,
        "[notificationMethod.type may not be null (was null)]");
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
        "[notificationMethod.address may not be empty (was )");
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
        "[notificationMethod.name size must be between 1 and 250");
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
        "[notificationMethod.address size must be between 1 and 100");
  }

  public void shouldList() {
    NotificationMethodsRepresentation notificationMethods = client().resource(
        "/v2.0/notification-methods")
        .header("X-Tenant-Id", "abc")
        .get(NotificationMethodsRepresentation.class);

    assertEquals(notificationMethods,
        new NotificationMethodsRepresentation(Arrays.asList(notificationMethod)));
    verify(repo).find(eq("abc"));
  }

  public void shouldGet() {
    assertEquals(client().resource("/v2.0/notification-methods/123")
        .header("X-Tenant-Id", "abc")
        .get(NotificationMethodRepresentation.class).notificationMethod, notificationMethod);
    verify(repo).findById(eq("abc"), eq("123"));
  }

  public void should404OnGetInvalid() {
    doThrow(new EntityNotFoundException("Not Found")).when(repo).findById(anyString(), anyString());

    try {
      client().resource("/v2.0/notification-methods/999")
          .header("X-Tenant-Id", "abc")
          .get(NotificationMethodRepresentation.class);
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
          .get(NotificationMethodsRepresentation.class);
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
          .post(NotificationMethodRepresentation.class,
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

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("bad_request", 400,
        "HV000116: The object to be validated must not be null.");
  }
}