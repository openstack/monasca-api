package com.hpcloud.mon.domain.model;

import static com.hpcloud.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod.NotificationMethodType;

@Test
public class NotificationMethodTest extends AbstractModelTest {
  private final NotificationMethod notificationMethod;

  public NotificationMethodTest() {
    notificationMethod = new NotificationMethod("123", "MySMS", NotificationMethodType.SMS,
        "9228675309");
    notificationMethod.setLinks(Arrays.asList(new Link("self",
        "https://region-a.geo-1.maas.hpcloudsvc.com/v1.0")));
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
