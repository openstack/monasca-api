package com.hpcloud.mon.infrastructure.persistence;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

@Test
public class SubAlarmQueriesTest {
  public void metricQueriesSubAlarmQueriesTest1() {
    String expectedResult =
        " inner join sub_alarm_dimension d0 on d0.dimension_name = :dname0 "
            + "and d0.value = :dvalue0 and dim.sub_alarm_id = d0.sub_alarm_id inner join "
            + "sub_alarm_dimension d1 on d1.dimension_name = :dname1 and d1.value = :dvalue1 and dim"
            + ".sub_alarm_id = d1.sub_alarm_id";
    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar");
    dimsMap.put("biz", "baz");

    assert (expectedResult.equals(SubAlarmQueries.buildJoinClauseFor(dimsMap)));
  }

  public void metricQueriesSubAlarmQueriesTest2() {
    String expectedResult = "";
    Map<String, String> dimsMap = new HashMap<>();

    assert (expectedResult.equals(SubAlarmQueries.buildJoinClauseFor(dimsMap)));
  }

  public void metricQueriesSubAlarmQueriesTest3() {
    String expectedResult = "";
    Map<String, String> dimsMap = null;

    assert (expectedResult.equals(SubAlarmQueries.buildJoinClauseFor(dimsMap)));
  }
}
