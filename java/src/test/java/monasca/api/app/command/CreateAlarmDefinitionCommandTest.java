/*
 * (C) Copyright 2014,2016 Hewlett Packard Enterprise Development LP
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import monasca.api.domain.model.AbstractModelTest;

@Test
public class CreateAlarmDefinitionCommandTest extends AbstractModelTest {
  public void shouldDeserializeFromJson() throws Exception {
    Map<String, String> dimensions = new HashMap<String, String>();
    dimensions.put("instanceId", "392633");
    /** todo: Check the null value to get works **/
    CreateAlarmDefinitionCommand newAlarm =
        new CreateAlarmDefinitionCommand("Disk Exceeds 1k Operations", null,
            "avg(hpcs.compute:cpu:1:{instance_id=5}) > 5", null, null, Arrays.asList("123345345",
                "23423"), null, null);

    String json = jsonFixture("fixtures/newAlarm.json");
    CreateAlarmDefinitionCommand alarm = fromJson(json, CreateAlarmDefinitionCommand.class);
    assertEquals(alarm, newAlarm);
  }
}
