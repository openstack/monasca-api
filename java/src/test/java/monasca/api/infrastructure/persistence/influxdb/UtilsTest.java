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

package monasca.api.infrastructure.persistence.influxdb;

import static org.testng.Assert.assertEquals;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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

  public void testBuildSerieNameRegex() throws Exception {
    String tenantId = "bob";
    String region = "42";
    final String seriesNoDims = tenantId + "?" + region + "&" + "cpu.idle_perc";
    final String seriesWithDims = seriesNoDims + "&barney=rubble&fred=flintstone&loony=toon";

    // This doesn't ensure that influxdb will evaluate this correctly because it is written in GO, but it
    // should give a good idea of the likelihood of  success
    checkRegex(Utils.buildSerieNameRegex(tenantId, region, "cpu.idle_perc", null), seriesNoDims, true);
    checkRegex(Utils.buildSerieNameRegex(tenantId, region, "cpu.idle_perc", null), seriesWithDims, true);

    // There was a bug where it was effectively doing a "startsWith" instead of pure match so test that
    checkRegex(Utils.buildSerieNameRegex(tenantId, region, "cpu.idle_per", null), seriesNoDims, false);
    checkRegex(Utils.buildSerieNameRegex(tenantId, region, "cpu.idle_per", null), seriesWithDims, false);

    // Make sure it works with the dimension to find in the front, middle and end of the dimensions
    // and that it does an exact match
    checkDimensionsRegex(tenantId, region, seriesWithDims, "barney", "rubble", "rubbl");
    checkDimensionsRegex(tenantId, region, seriesWithDims, "fred", "flintstone", "flint");
    checkDimensionsRegex(tenantId, region, seriesWithDims, "loony", "toon", "to");
  }

  private void checkDimensionsRegex(String tenantId, String region, final String seriesWithDims,
      String name, String goodValue, String badValue) throws Exception {
    final Map<String, String> dimensions = new HashMap<String, String>();
    dimensions.put(name, goodValue);
    checkRegex(Utils.buildSerieNameRegex(tenantId, region, "cpu.idle_perc", dimensions), seriesWithDims, true);

    dimensions.put(name, badValue);
    checkRegex(Utils.buildSerieNameRegex(tenantId, region, "cpu.idle_perc", dimensions), seriesWithDims, false);
  }

  private void checkRegex(String regex, final String seriesNoDims, final boolean expected) {
    // The real check in influxDb is a start with so add the ".*" to emulate that behavior
    assertEquals(Pattern.matches(regex + ".*", seriesNoDims), expected);
  }
}
