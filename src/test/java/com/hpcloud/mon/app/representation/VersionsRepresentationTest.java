package com.hpcloud.mon.app.representation;

import static com.hpcloud.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;

import org.testng.annotations.Test;

import com.hpcloud.mon.domain.model.AbstractModelTest;
import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.version.Version;
import com.hpcloud.mon.domain.model.version.Version.VersionStatus;

@Test
public class VersionsRepresentationTest extends AbstractModelTest {
  public void shouldSerializeToJson() throws Exception {
    Version version = new Version("1.0", VersionStatus.CURRENT, new Date(1355253328));
    version.setLinks(Arrays.asList(new Link("self",
        "https://region-a.geo-1.maas.hpcloudsvc.com/v1.0")));

    String json = toJson(Arrays.asList(version));
    assertEquals(json, jsonFixture("fixtures/versions.json"));
  }
}
