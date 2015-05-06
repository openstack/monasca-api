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

package monasca.api.infrastructure.persistence.vertica;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

@Test
public class MetricQueriesTest {
  public void metricQueriesBuildJoinClauseForTest1() {
    String expectedResult =
        " inner join MonMetrics.Dimensions dim0 on dim0.name = :dname0 and dim0" + ".value "
            + "= :dvalue0 and defdims.dimension_set_id = dim0.dimension_set_id inner join "
            + "MonMetrics.Dimensions dim1 on dim1.name = :dname1 and dim1.value = :dvalue1 and defdims"
            + ".dimension_set_id = dim1.dimension_set_id";
    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar");
    dimsMap.put("biz", "baz");

    String s = MetricQueries.buildJoinClauseFor(dimsMap);
    assertEquals(expectedResult, s);
  }

  public void metricQueriesBuildJoinClauseForTest2() {
    String expectedResult = "";
    Map<String, String> dimsMap = new HashMap<>();
    assertEquals(expectedResult, MetricQueries.buildJoinClauseFor(dimsMap));
  }

  public void metricQueriesBuildJoinClauseForTest3() {
    String expectedResult = "";
    Map<String, String> dimsMap = null;
    assertEquals(expectedResult, MetricQueries.buildJoinClauseFor(dimsMap));
  }
}
