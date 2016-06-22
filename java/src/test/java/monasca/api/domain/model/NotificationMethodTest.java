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

package monasca.api.domain.model;

import static monasca.common.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import monasca.api.domain.model.common.Link;
import monasca.api.domain.model.notificationmethod.NotificationMethod;


@Test
public class NotificationMethodTest extends AbstractModelTest {
  private final NotificationMethod notificationMethod;

  public NotificationMethodTest() {
    notificationMethod =
        new NotificationMethod("123", "MyEmail", "EMAIL", "a@b", 0);
    notificationMethod.setLinks(Arrays.asList(new Link("self",
        "https://cloudsvc.example.com/v1.0")));
  }

  public void shouldSerializeToJson() throws Exception {
    String json = toJson(notificationMethod);
    assertEquals(json, jsonFixture("fixtures/notificationMethod.json"));
  }

  public void shouldDeserializeFromJson() throws Exception {
    String json = jsonFixture("fixtures/notificationMethod.json");
    NotificationMethod detail = fromJson(json, NotificationMethod.class);
    assertEquals(notificationMethod, detail);
  }
}
