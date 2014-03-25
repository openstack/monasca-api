package com.hpcloud.mon.app.representation;

import static com.hpcloud.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.model.AbstractModelTest;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.common.Link;

@Test
public class AlarmsRepresentationTest extends AbstractModelTest {
  public void shouldSerializeToJson() throws Exception {
    Map<String, String> dimensions = new HashMap<String, String>();
    dimensions.put("instance_id", "666");
    dimensions.put("image_id", "345");
    Alarm alarm = new Alarm("123", "90% CPU", null, "avg(hpcs.compute:cpu:1:{instance_id=5}) > 5",
        AlarmState.OK, true);
    alarm.setLinks(Arrays.asList(new Link("self", "https://region-a.geo-1.maas.hpcloudsvc.com/v1.0")));

    String json = toJson(Arrays.asList(alarm));
    assertEquals(json, jsonFixture("fixtures/alarms.json"));
  }
}
