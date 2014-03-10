package com.hpcloud.mon.app.request;

import static com.hpcloud.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.hpcloud.mon.app.command.CreateNotificationMethodCommand;
import com.hpcloud.mon.domain.model.AbstractModelTest;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod.NotificationMethodType;

@Test
public class NewNotificationMethodTest extends AbstractModelTest {
  public void shouldDeserializeFromJson() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod = new CreateNotificationMethodCommand(
        "MySMS", NotificationMethodType.SMS, "9228675309");

    String json = jsonFixture("fixtures/newNotificationMethod.json");
    CreateNotificationMethodCommand other = fromJson(json, CreateNotificationMethodCommand.class);
    assertEquals(other, newNotificationMethod);
  }

  public void shouldDeserializeFromJsonLowerCaseEnum() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod = new CreateNotificationMethodCommand(
        "MySMS", NotificationMethodType.SMS, "9228675309");

    String json = jsonFixture("fixtures/newNotificationMethodWithLowercaseEnum.json");
    CreateNotificationMethodCommand other = fromJson(json, CreateNotificationMethodCommand.class);
    assertEquals(other, newNotificationMethod);
  }

  @Test(expectedExceptions = JsonMappingException.class)
  public void shouldDeserializeFromJsonEnumError() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod = new CreateNotificationMethodCommand(
        "MySMS", NotificationMethodType.SMS, "9228675309");

    String json = jsonFixture("fixtures/newNotificationMethodWithInvalidEnum.json");
    CreateNotificationMethodCommand other = fromJson(json, CreateNotificationMethodCommand.class);
    assertEquals(other, newNotificationMethod);
  }
}
