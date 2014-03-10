package com.hpcloud.mon.app.representation;

import static com.hpcloud.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.hpcloud.mon.domain.model.AbstractModelTest;
import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod.NotificationMethodType;

@Test
public class NotificationMethodsRepresentationTest extends AbstractModelTest {
  private final NotificationMethod notificationMethod;

  public NotificationMethodsRepresentationTest() {
    notificationMethod = new NotificationMethod("123", "MySMS", NotificationMethodType.SMS,
        "9228675309");
    notificationMethod.setLinks(Arrays.asList(new Link("self",
        "https://region-a.geo-1.maas.hpcloudsvc.com/v1.0")));
  }

  public void shouldSerializeToJson() throws Exception {
    String json = toJson(new NotificationMethodsRepresentation(Arrays.asList(notificationMethod)));
    assertEquals(json, jsonFixture("fixtures/notificationMethods.json"));
  }
}
