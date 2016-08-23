/*
 * (C) Copyright 2016 Hewlett Packard Enterprise Development LP
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

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import java.text.ParseException;

@Test
public class PersistUtilsTest {
  private final PersistUtils persistUtils = new PersistUtils();

  public void test3DigitWithSpace() throws ParseException {
    checkParseTimestamp("2016-01-01 01:01:01.123Z", "2016-01-01 01:01:01.123Z");
  }

  public void test2DigitWithSpace() throws ParseException {
    checkParseTimestamp("2016-01-01 01:01:01.15Z", "2016-01-01 01:01:01.150Z");
  }

  public void test1DigitWithSpace() throws ParseException {
    checkParseTimestamp("2016-01-01 01:01:01.1Z", "2016-01-01 01:01:01.100Z");
  }

  public void test3DigitWithT() throws ParseException {
    checkParseTimestamp("2016-01-01T01:01:01.123Z", "2016-01-01T01:01:01.123Z");
  }

  public void test2DigitWithT() throws ParseException {
    checkParseTimestamp("2016-01-01T01:01:01.15Z", "2016-01-01T01:01:01.150Z");
  }

  public void test1DigitWithT() throws ParseException {
    checkParseTimestamp("2016-01-01T01:01:01.1Z", "2016-01-01T01:01:01.100Z");
  }

  private void checkParseTimestamp(final String start, final String expected) throws ParseException {
    assertEquals(persistUtils.parseTimestamp(start).getTime(), persistUtils.parseTimestamp(expected).getTime());
  }
}
