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

package monasca.api.domain.model;

import static monasca.common.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.common.Link;

@Test
public class AlarmDefinitionTest extends AbstractModelTest {
  private final AlarmDefinition alarm;
  private final Map<String, String> dimensions;

  public AlarmDefinitionTest() {
    dimensions = new HashMap<String, String>();
    dimensions.put("instance_id", "666");
    dimensions.put("image_id", "345");
    alarm =
        new AlarmDefinition("123", "90% CPU", null, "LOW",
            "avg(hpcs.compute{instance_id=666, image_id=345}) >= 90",
            Collections.<String>emptyList(), false, Arrays.asList("123345345", "23423"), null, null);
    alarm.setLinks(Arrays
        .asList(new Link("self", "https://cloudsvc.example.com/v1.0")));
  }

  public void shouldSerializeToJson() throws Exception {
    String json = toJson(alarm);
    assertEquals(json, jsonFixture("fixtures/alarm.json"));
  }
}
