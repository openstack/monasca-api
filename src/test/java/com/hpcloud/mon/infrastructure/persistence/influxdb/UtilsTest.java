package com.hpcloud.mon.infrastructure.persistence.influxdb;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.Test;

@Test
public class UtilsTest {
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

  public void whereClauseBuilderBuildTimePartTest() {
    String expectedResult = " and time > 1388538061s and time < 1388538062s";
    DateTime startTime = new DateTime(2014, 01, 01, 01, 01, 01, DateTimeZone.UTC);
    DateTime endTime = new DateTime(2014, 01, 01, 01, 01, 02, DateTimeZone.UTC);

    assert (expectedResult.equals(Utils.WhereClauseBuilder.buildTimePart(startTime, endTime)));
  }

  public void whereClauseBuilderBuildDimsPartTest1() throws Exception {
    String expectedResult = "";
    Map<String, String> dimsMap = new HashMap<>();
    assert (expectedResult.equals(Utils.WhereClauseBuilder.buildDimsPart(dimsMap)));
  }

  public void whereClauseBuilderBuildDimsPartTest2() throws Exception {
    String expectedResult = " and foo = 'bar'";
    Map<String, String> dimsMap = new HashMap<>();
    dimsMap.put("foo", "bar");
    assert (expectedResult.equals(Utils.WhereClauseBuilder.buildDimsPart(dimsMap)));
  }

  public void whereClauseBuilderBuildDimsPartTest3() throws Exception {
    String expectedResult = " and foo = 'bar' and biz = 'baz'";
    Map<String, String> dimsMap = new LinkedHashMap<>();
    dimsMap.put("foo", "bar");
    dimsMap.put("biz", "baz");
    assert (expectedResult.equals(Utils.WhereClauseBuilder.buildDimsPart(dimsMap)));
  }
}
