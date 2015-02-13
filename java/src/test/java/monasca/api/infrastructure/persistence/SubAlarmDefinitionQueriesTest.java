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

package monasca.api.infrastructure.persistence;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

@Test
public class SubAlarmDefinitionQueriesTest {
  public void metricQueriesSubAlarmQueriesTest1() {
    String expectedResult =
        " inner join sub_alarm_definition_dimension d0 on d0.dimension_name = :dname0 "
            + "and d0.value = :dvalue0 and dim.sub_alarm_definition_id = d0.sub_alarm_definition_id inner join "
            + "sub_alarm_definition_dimension d1 on d1.dimension_name = :dname1 and d1.value = :dvalue1 and dim"
            + ".sub_alarm_definition_id = d1.sub_alarm_definition_id";
    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar");
    dimsMap.put("biz", "baz");

    assertEquals(SubAlarmDefinitionQueries.buildJoinClauseFor(dimsMap), expectedResult);
  }

  public void metricQueriesSubAlarmQueriesTest2() {
    String expectedResult = "";
    Map<String, String> dimsMap = new HashMap<>();

    assertEquals(SubAlarmDefinitionQueries.buildJoinClauseFor(dimsMap), expectedResult);
  }

  public void metricQueriesSubAlarmQueriesTest3() {
    String expectedResult = "";
    Map<String, String> dimsMap = null;

    assertEquals(SubAlarmDefinitionQueries.buildJoinClauseFor(dimsMap), expectedResult);
  }
}
