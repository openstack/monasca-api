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

package monasca.api.infrastructure.persistence.vertica;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

@Test
public class MetricQueriesTest {

  private final static String TABLE_TO_JOIN_DIMENSIONS_ON = "defdims";

  public void metricQueriesBuildDimensionAndClauseTest1() {
    String expectedResult =
        " and defdims.id in ( SELECT defDimsSub2.id FROM MonMetrics.Dimensions AS dimSub "
        + "JOIN MonMetrics.DefinitionDimensions AS defDimsSub2 "
        + "ON defDimsSub2.dimension_set_id = dimSub.dimension_set_id WHERE"
        + " ((name = :dname0 and value = :dvalue0) or (name = :dname1 and value = :dvalue1))"
        + " GROUP BY defDimsSub2.id,dimSub.dimension_set_id HAVING count(*) = 2) ";

    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar");
    dimsMap.put("biz", "baz");

    String s = MetricQueries.buildDimensionAndClause(dimsMap, TABLE_TO_JOIN_DIMENSIONS_ON);
    assertEquals(expectedResult, s);
  }

  public void metricQueriesBuildDimensionAndClauseTest2() {
    String expectedResult = "";
    Map<String, String> dimsMap = new HashMap<>();
    assertEquals(expectedResult, MetricQueries.buildDimensionAndClause(dimsMap, TABLE_TO_JOIN_DIMENSIONS_ON));
  }

  public void metricQueriesBuildDimensionAndClauseForTest3() {
    String expectedResult = "";
    Map<String, String> dimsMap = null;
    assertEquals(expectedResult, MetricQueries.buildDimensionAndClause(dimsMap, TABLE_TO_JOIN_DIMENSIONS_ON));
  }

  public void metricQueriesBuildDimensionAndClauseTest4() {
    String expectedResult =
        " and defdims.id in ( SELECT defDimsSub2.id FROM MonMetrics.Dimensions AS dimSub "
        + "JOIN MonMetrics.DefinitionDimensions AS defDimsSub2 "
        + "ON defDimsSub2.dimension_set_id = dimSub.dimension_set_id WHERE"
        + " ((name = :dname0 and ( value = :dvalue0_0 or value = :dvalue0_1)))"
        + " GROUP BY defDimsSub2.id,dimSub.dimension_set_id HAVING count(*) = 1) ";

    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar|baz");

    String s = MetricQueries.buildDimensionAndClause(dimsMap, TABLE_TO_JOIN_DIMENSIONS_ON);
    assertEquals(expectedResult, s);
  }

  public void metricQueriesBuildDimensionAndClauseTest5() {
    String expectedResult =
        " and defdims.id in ( SELECT defDimsSub2.id FROM MonMetrics.Dimensions AS dimSub "
        + "JOIN MonMetrics.DefinitionDimensions AS defDimsSub2 "
        + "ON defDimsSub2.dimension_set_id = dimSub.dimension_set_id WHERE"
        + " ((name = :dname0 and ( value = :dvalue0_0 or value = :dvalue0_1))"
        + " or (name = :dname1 and ( value = :dvalue1_0 or value = :dvalue1_1)))"
        + " GROUP BY defDimsSub2.id,dimSub.dimension_set_id HAVING count(*) = 2) ";

    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar|baz");
    dimsMap.put("biz", "baz|baf");

    String s = MetricQueries.buildDimensionAndClause(dimsMap, TABLE_TO_JOIN_DIMENSIONS_ON);
    assertEquals(expectedResult, s);
  }
}
