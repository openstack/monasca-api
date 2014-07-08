package com.hpcloud.mon.resource;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.version.Version;
import com.hpcloud.mon.domain.model.version.Version.VersionStatus;
import com.hpcloud.mon.domain.model.version.VersionRepository;
import com.sun.jersey.api.client.GenericType;

@Test
public class VersionResourceTest extends AbstractMonApiResourceTest {
  private Version version;
  private VersionRepository repo;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();
    version = new Version("v2.0", VersionStatus.CURRENT, new DateTime(1355253328));
    version.setLinks(Arrays.asList(new Link("self",
        "https://region-a.geo-1.maas.hpcloudsvc.com/v2.0")));

    repo = mock(VersionRepository.class);
    when(repo.findById(eq("v2.0"))).thenReturn(version);
    when(repo.find()).thenReturn(Arrays.asList(version));
    addResources(new VersionResource(repo));
  }

  public void shouldList() {
    List<Version> versions = client().resource("/").get(new GenericType<List<Version>>() {});
    assertEquals(versions, Arrays.asList(version));
    verify(repo).find();
  }

  public void shouldGet() {
    assertEquals(client().resource("/v2.0").get(Version.class), version);
    verify(repo).findById(eq("v2.0"));
  }

  public void should404OnGetInvalid() {
    doThrow(new EntityNotFoundException("")).when(repo).findById(anyString());

    try {
      client().resource("/v9.9").get(Version.class);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("404"));
    }
  }

  public void should500OnInternalException() {
    doThrow(new RuntimeException("")).when(repo).find();

    try {
      client().resource("/").get(new GenericType<List<Version>>() {});
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("500"));
    }
  }
}
