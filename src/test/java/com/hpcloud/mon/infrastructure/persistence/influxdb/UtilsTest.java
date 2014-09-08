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
    String goodString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" + "-_" +
        ".?/%=&鱼鸟";

    assert (goodString.equals(Utils.SQLSanitizer.sanitize(goodString)));
  }

  @Test(expectedExceptions = {Exception.class})
  public void SQLSanitizerSanitizeBadDataTest1() throws Exception {
    String badStringWithSemicolon = "abcdefghijklmnopqrs;tuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" + "-_.";

    assert (badStringWithSemicolon.equals(Utils.SQLSanitizer.sanitize(badStringWithSemicolon)));
  }

  @Test(expectedExceptions = {Exception.class})
  public void SQLSanitizerSanitizeBadDataTest2() throws Exception {
    String badStringWithSingleQuote = "'a'bcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" + "-_.";

    assert (badStringWithSingleQuote.equals(Utils.SQLSanitizer.sanitize(badStringWithSingleQuote)));
  }

  public void whereClauseBuilderBuildTimePartTest() {
    String expectedResult = " and time > 1388538061s and time < 1388538062s";
    DateTime startTime = new DateTime(2014, 01, 01, 01, 01, 01, DateTimeZone.UTC);
    DateTime endTime = new DateTime(2014, 01, 01, 01, 01, 02, DateTimeZone.UTC);

    assert (expectedResult.equals(Utils.WhereClauseBuilder.buildTimePart(startTime, endTime)));
  }

}
