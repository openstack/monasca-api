/*
 * Copyright 2015 FUJITSU LIMITED
 *
 * Copyright (c) 2016 Hewlett Packard Enterprise Development Company LP
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
 *
 */

package monasca.api.infrastructure.persistence.influxdb;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


@Test
public class InfluxV9UtilsTest {

  private InfluxV9Utils instance;

  @BeforeMethod
  protected void setupClass() throws Exception {
    this.instance = new InfluxV9Utils();
  }

  @Test(groups = {"functional", "timeOffsetPart"})
  public void testTimeOffsetPart_Timestamp() throws Exception {
    final String ts = "1443009555969";
    final String tsDt = "2015-09-23T11:59:15.969Z";

    assertEquals(String.format(" and time > '%1$s'", tsDt), this.instance.timeOffsetPart(ts));
  }

  @Test(groups = {"functional", "timeOffsetPart"})
  public void testTimeOffsetPart_DateTime() throws Exception {
    final String ts = "2015-09-23T11:59:15.969Z";
    assertEquals(String.format(" and time > '%1$s'", ts), this.instance.timeOffsetPart(ts));
  }

  @Test(groups = {"functional", "timeOffsetPart"})
  public void testTimeOffsetPart_EmptyString() throws Exception {
    assertEquals(StringUtils.EMPTY, this.instance.timeOffsetPart(StringUtils.EMPTY));
  }

  @Test(groups = {"functional", "timeOffsetPart"})
  public void testTimeOffsetPart_NullString() throws Exception {
    assertEquals(StringUtils.EMPTY, this.instance.timeOffsetPart(null));
  }

  @Test(groups = {"functional", "timeOffsetPart"})
  public void testTimeOffsetPart_0() throws Exception {
    final String offset = "0";
    assertEquals(String.format(" and time > '%1$s'", offset), this.instance.timeOffsetPart(offset));
  }

  @Test(groups = {"threeDigitMillisTimestamp"})
  public void testThreeDigitMillisTimestamp_with_3digit() throws Exception {
    final String origTimestamp = "2016-01-11T16:10:34.472Z";
    assertEquals(this.instance.threeDigitMillisTimestamp(origTimestamp), origTimestamp);
  }

  @Test(groups = {"threeDigitMillisTimestamp"})
  public void testThreeDigitMillisTimestamp_with_2digit() throws Exception {
    final String origTimestamp_1 = "2016-01-11T16:10:34.47Z";
    assertEquals(this.instance.threeDigitMillisTimestamp(origTimestamp_1), "2016-01-11T16:10:34.470Z");
    final String origTimestamp_2 = "2016-01-11T16:10:34.40Z";
    assertEquals(this.instance.threeDigitMillisTimestamp(origTimestamp_2), "2016-01-11T16:10:34.400Z");
  }

  @Test(groups = {"threeDigitMillisTimestamp"})
  public void testThreeDigitMillisTimestamp_with_1digit() throws Exception {
    final String origTimestamp = "2016-01-11T16:10:34.4Z";
    assertEquals(this.instance.threeDigitMillisTimestamp(origTimestamp), "2016-01-11T16:10:34.400Z");
  }

  @Test(groups = {"threeDigitMillisTimestamp"})
  public void testThreeDigitMillisTimestamp_with_0digit() throws Exception {
    final String origTimestamp = "2016-01-11T16:10:34Z";
    assertEquals(this.instance.threeDigitMillisTimestamp(origTimestamp), "2016-01-11T16:10:34.000Z");
  }
}