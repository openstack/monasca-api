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

package com.hpcloud.mon.domain.model;

import static com.hpcloud.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.Test;

import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.version.Version;
import com.hpcloud.mon.domain.model.version.Version.VersionStatus;

@Test
public class VersionTest extends AbstractModelTest {
  private final Version version;

  public VersionTest() {
    version =
        new Version("1.0", VersionStatus.CURRENT, new DateTime(1355253328000L, DateTimeZone.UTC));
    version.setLinks(Arrays.asList(new Link("self",
        "https://region-a.geo-1.maas.hpcloudsvc.com/v1.0")));
  }

  public void shouldSerializeToJson() throws Exception {
    String json = toJson(version);
    assertEquals(json, jsonFixture("fixtures/version.json"));
  }

  public void shouldDeserializeFromJson() throws Exception {
    String json = jsonFixture("fixtures/version.json");
    Version detail = fromJson(json, Version.class);
    assertEquals(version, detail);
  }
}
