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

package monasca.api.app.command;

import static monasca.common.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import javax.ws.rs.WebApplicationException;

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonMappingException;

import monasca.api.app.command.CreateNotificationMethodCommand;
import monasca.api.domain.model.AbstractModelTest;
import monasca.api.domain.model.notificationmethod.NotificationMethodType;

@Test
public class CreateNotificationMethodTest extends AbstractModelTest {
  public void shouldDeserializeFromJson() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MySMS", NotificationMethodType.SMS, "9228675309");

    String json = jsonFixture("fixtures/newNotificationMethod.json");
    CreateNotificationMethodCommand other = fromJson(json, CreateNotificationMethodCommand.class);
    assertEquals(other, newNotificationMethod);
  }

  public void shouldDeserializeFromJsonLowerCaseEnum() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MySMS", NotificationMethodType.SMS, "9228675309");

    String json = jsonFixture("fixtures/newNotificationMethodWithLowercaseEnum.json");
    CreateNotificationMethodCommand other = fromJson(json, CreateNotificationMethodCommand.class);
    assertEquals(other, newNotificationMethod);
  }

  @Test(expectedExceptions = JsonMappingException.class)
  public void shouldDeserializeFromJsonEnumError() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MySMS", NotificationMethodType.SMS, "9228675309");

    String json = jsonFixture("fixtures/newNotificationMethodWithInvalidEnum.json");
    CreateNotificationMethodCommand other = fromJson(json, CreateNotificationMethodCommand.class);
    assertEquals(other, newNotificationMethod);
  }

  public void testValidationForEmail() {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "name@domain.com");
      newNotificationMethod.validate();
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void testValidationExceptionForEmail() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "name@domain.");

    newNotificationMethod.validate();
  }

  public void testValidationForWebhook() {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.WEBHOOK, "http://somedomain.com");
      newNotificationMethod.validate();
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void testValidationExceptionForWebhook() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyWebhook", NotificationMethodType.WEBHOOK, "ftp://localhost");

    newNotificationMethod.validate();
  }
}