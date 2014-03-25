package com.hpcloud.mon.domain.model;

import static com.hpcloud.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.model.alarm.AlarmDetail;
import com.hpcloud.mon.domain.model.common.Link;

@Test
public class AlarmTest extends AbstractModelTest {
  private final AlarmDetail alarm;
  private final Map<String, String> dimensions;

  public AlarmTest() {
    dimensions = new HashMap<String, String>();
    dimensions.put("instance_id", "666");
    dimensions.put("image_id", "345");
    alarm = new AlarmDetail("123", "90% CPU", null,
        "avg(hpcs.compute:cpu:1:{instance_id=666, image_id=345}) >= 90", AlarmState.OK, false,
        Arrays.asList("123345345", "23423"), null, null);
    alarm.setLinks(Arrays.asList(new Link("self", "https://region-a.geo-1.maas.hpcloudsvc.com/v1.0")));
  }

  public void shouldSerializeToJson() throws Exception {
    String json = toJson(alarm);
    assertEquals(json, jsonFixture("fixtures/alarm.json"));
  }

  public void shouldDeserializeFromJson() throws Exception {
    String json = jsonFixture("fixtures/alarm.json");
    AlarmDetail detail = fromJson(json, AlarmDetail.class);
    assertEquals(alarm, detail);
  }
}
