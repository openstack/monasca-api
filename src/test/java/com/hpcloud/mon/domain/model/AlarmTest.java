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

package com.hpcloud.mon.domain.model;

import static com.hpcloud.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.common.Link;

@Test
public class AlarmTest extends AbstractModelTest {
  private final Alarm alarm;
  private final Map<String, String> dimensions;

  public AlarmTest() {
    dimensions = new HashMap<String, String>();
    dimensions.put("instance_id", "666");
    dimensions.put("image_id", "345");
    alarm =
        new Alarm("123", "90% CPU", null, "LOW",
            "avg(hpcs.compute{instance_id=666, image_id=345}) >= 90", AlarmState.OK, false,
            Arrays.asList("123345345", "23423"), null, null);
    alarm.setLinks(Arrays
        .asList(new Link("self", "https://region-a.geo-1.maas.hpcloudsvc.com/v1.0")));
  }

  public void shouldSerializeToJson() throws Exception {
    String json = toJson(alarm);
    assertEquals(json, jsonFixture("fixtures/alarm.json"));
  }

  public void shouldDeserializeFromJson() throws Exception {
    String json = jsonFixture("fixtures/alarm.json");
    Alarm detail = fromJson(json, Alarm.class);
    assertEquals(alarm, detail);
  }
}
