package com.hpcloud.mon.infrastructure.persistence;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class UtilsTest {

  @Test
  public void SQLSanitizerSanitizeGoodDataTest() throws Exception {

    String goodString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" + "-_.";

    assert (goodString.equals(Utils.SQLSanitizer.sanitize(goodString)));
  }

  @Test(expectedExceptions = {Exception.class})
  public void SQLSanitizerSanitizeBadDataTest1() throws Exception {

    String badString = "';abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" + "-_.";

    assert (badString.equals(Utils.SQLSanitizer.sanitize(badString)));
  }

  @Test(expectedExceptions = {Exception.class})
  public void SQLSanitizerSanitizeBadDataTest2() throws Exception {

    String badStrng = "'a'bcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" + "-_.";

    assert (badStrng.equals(Utils.SQLSanitizer.sanitize(badStrng)));
  }

  @Test
  public void whereClauseBuilderBuildTimePartTest() {

    String expectedResult = " and time > 1388563261s and time < 1388563262s";
    DateTime startTime = new DateTime(2014, 01, 01, 01, 01, 01);
    DateTime endTime = new DateTime(2014, 01, 01, 01, 01, 02);

    assert (expectedResult.equals(Utils.WhereClauseBuilder.buildTimePart(startTime, endTime)));
  }

  @Test
  public void whereClauseBuilderBuildDimsPartTest1() throws Exception {

    String expectedResult = "";
    Map<String, String> dimsMap = new HashMap<>();
    assert (expectedResult.equals(Utils.WhereClauseBuilder.buildDimsPart(dimsMap)));

  }

  @Test
  public void whereClauseBuilderBuildDimsPartTest2() throws Exception {

    String expectedResult = " and foo = 'bar'";
    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar");
    assert (expectedResult.equals(Utils.WhereClauseBuilder.buildDimsPart(dimsMap)));

  }

  @Test
  public void whereClauseBuilderBuildDimsPartTest3() throws Exception {

    String expectedResult = " and foo = 'bar' and biz = 'baz'";
    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar");
    dimsMap.put("biz", "baz");
    assert (expectedResult.equals(Utils.WhereClauseBuilder.buildDimsPart(dimsMap)));

  }

  @Test
  public void metricQueriesBuildJoinClauseForTest1() {

    String expectedResult = " inner join MonMetrics.Dimensions d0 on d0.name = :dname0 and d0" +
        ".value " + "= :dvalue0 and dd.dimension_set_id = d0.dimension_set_id inner join " +
        "MonMetrics.Dimensions d1 on d1.name = :dname1 and d1.value = :dvalue1 and dd" +
        ".dimension_set_id = d1.dimension_set_id";
    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar");
    dimsMap.put("biz", "baz");

    assert (expectedResult.equals(Utils.MetricQueries.buildJoinClauseFor(dimsMap)));
  }

  @Test
  public void metricQueriesBuildJoinClauseForTest2() {

    String expectedResult = "";
    Map<String, String> dimsMap = new HashMap<>();
    assert (expectedResult.equals(Utils.MetricQueries.buildJoinClauseFor(dimsMap)));
  }

  @Test
  public void metricQueriesBuildJoinClauseForTest3() {

    String expectedResult = "";
    Map<String, String> dimsMap = null;
    assert (expectedResult.equals(Utils.MetricQueries.buildJoinClauseFor(dimsMap)));
  }

  @Test
  public void metricQueriesSubAlarmQueriesTest1() {

    String expectedResult = " inner join sub_alarm_dimension d0 on d0.dimension_name = :dname0 " +
        "and d0.value = :dvalue0 and dim.sub_alarm_id = d0.sub_alarm_id inner join " +
        "sub_alarm_dimension d1 on d1.dimension_name = :dname1 and d1.value = :dvalue1 and dim" +
        ".sub_alarm_id = d1.sub_alarm_id";
    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar");
    dimsMap.put("biz", "baz");

    assert (expectedResult.equals(Utils.SubAlarmQueries.buildJoinClauseFor(dimsMap)));

  }

  @Test
  public void metricQueriesSubAlarmQueriesTest2() {

    String expectedResult = "";
    Map<String, String> dimsMap = new HashMap<>();

    assert (expectedResult.equals(Utils.SubAlarmQueries.buildJoinClauseFor(dimsMap)));

  }

  @Test
  public void metricQueriesSubAlarmQueriesTest3() {

    String expectedResult = "";
    Map<String, String> dimsMap = null;

    assert (expectedResult.equals(Utils.SubAlarmQueries.buildJoinClauseFor(dimsMap)));

  }

}
