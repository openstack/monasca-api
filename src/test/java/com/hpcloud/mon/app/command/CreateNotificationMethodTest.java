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

package com.hpcloud.mon.app.command;

import static com.hpcloud.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.hpcloud.mon.app.command.CreateNotificationMethodCommand;
import com.hpcloud.mon.domain.model.AbstractModelTest;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodType;

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
}
