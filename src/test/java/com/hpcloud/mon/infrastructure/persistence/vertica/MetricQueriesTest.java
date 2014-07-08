package com.hpcloud.mon.infrastructure.persistence.vertica;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

@Test
public class MetricQueriesTest {
  public void metricQueriesBuildJoinClauseForTest1() {
    String expectedResult =
        " inner join MonMetrics.Dimensions d0 on d0.name = :dname0 and d0" + ".value "
            + "= :dvalue0 and dd.dimension_set_id = d0.dimension_set_id inner join "
            + "MonMetrics.Dimensions d1 on d1.name = :dname1 and d1.value = :dvalue1 and dd"
            + ".dimension_set_id = d1.dimension_set_id";
    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar");
    dimsMap.put("biz", "baz");

    assert (expectedResult.equals(MetricQueries.buildJoinClauseFor(dimsMap)));
  }

  public void metricQueriesBuildJoinClauseForTest2() {
    String expectedResult = "";
    Map<String, String> dimsMap = new HashMap<>();
    assert (expectedResult.equals(MetricQueries.buildJoinClauseFor(dimsMap)));
  }

  public void metricQueriesBuildJoinClauseForTest3() {
    String expectedResult = "";
    Map<String, String> dimsMap = null;
    assert (expectedResult.equals(MetricQueries.buildJoinClauseFor(dimsMap)));
  }
}
