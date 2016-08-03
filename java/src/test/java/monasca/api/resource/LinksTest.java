/*
 * (C) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
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

package monasca.api.resource;

import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.common.Link;
import monasca.api.domain.model.common.Paged;
import monasca.common.model.alarm.AlarmState;
import static org.testng.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

@Test
public class LinksTest {
  private static final String ALARM_DEF_ID = "af72b3d8-51f3-4eee-8086-535b5e7a9dc8";

  public void shouldPrefixForHttps() throws UnsupportedEncodingException, URISyntaxException {
    Links.accessedViaHttps = true;
    assertEquals(Links.prefixForHttps("http://abc123blah/blah/blah"),
        "https://abc123blah/blah/blah");
    assertEquals(Links.prefixForHttps("https://abc123blah/blah/blah"),
        "https://abc123blah/blah/blah");

    checkSelfNextLinks(true);

    // Negative
    Links.accessedViaHttps = false;
    assertEquals(Links.prefixForHttps("http://abc123blah/blah/blah"), "http://abc123blah/blah/blah");
    assertEquals(Links.prefixForHttps("https://abc123blah/blah/blah"),
        "https://abc123blah/blah/blah");

    checkSelfNextLinks(false);
  }

  private void checkSelfNextLinks(final boolean returnHttps) throws URISyntaxException,
      UnsupportedEncodingException {
    final String base = "http://TheVip:8070/v2.0/alarms";
    final String limitParam = "limit=1";
    final String url = base + "?" + limitParam;
    final UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getRequestUri()).thenReturn(new URI(url));
    when(uriInfo.getAbsolutePath()).thenReturn(new URI(base));

    final Map<String, String> params = new HashMap<>();
    params.put("limit", "1");
    @SuppressWarnings("unchecked")
    final MultivaluedMap<String, String> mockParams = mock(MultivaluedMap.class);
    when(uriInfo.getQueryParameters()).thenReturn(mockParams);
    when(mockParams.keySet()).thenReturn(params.keySet());
    when(mockParams.get("limit")).thenReturn(Arrays.asList("1"));

    // Since limit is 1, need to give two elements so code knows to add next link
    final List<Alarm> elements = new ArrayList<>();
    elements.add(createAlarm());
    elements.add(createAlarm());

    final int limit = 1;
    final Paged expected = new Paged();
    final List<Link> links = new ArrayList<>();
    String expectedSelf = url;
    String expectedNext = base + "?offset=" + elements.get(0).getId() + "&" + limitParam;
    if (returnHttps) {
      expectedSelf = expectedSelf.replace("http", "https");
      expectedNext = expectedNext.replace("http", "https");
    }
    links.add(new Link("self", expectedSelf));
    links.add(new Link("next", expectedNext));
    expected.links = links;

    final ArrayList<Alarm> expectedElements = new ArrayList<Alarm>();
    // Since limit is one, only the first element is returned
    expectedElements.add(elements.get(0));
    expected.elements = expectedElements;
    final Paged actual = Links.paginate(limit, elements, uriInfo);
    assertEquals(actual, expected);
  }

  private Alarm createAlarm() {
    final String alarmId = UUID.randomUUID().toString();
    final Alarm alarm = new Alarm(alarmId, ALARM_DEF_ID, "Test", "LOW", null, AlarmState.OK,
                                  "OPEN", null,
                                  DateTime.parse("2015-03-14T09:26:53"),
                                  DateTime.parse("2015-03-14T09:26:53"),
                                  DateTime.parse("2015-03-14T09:26:53"));
    return alarm;
  }

  public void shouldHydrate() throws URISyntaxException {
    checkHydrate("http://localhost:8070/");
    checkHydrate("https://localhost/");
    checkHydrate("https://localhost//");
    checkHydrate(""); // Should not happen but handle relative paths
  }

  private void checkHydrate(final String base) throws URISyntaxException {
    final UriInfo uriInfo = mock(UriInfo.class);
    final URI uri = new URI(base);
    when(uriInfo.getBaseUri()).thenReturn(uri);
    final Alarm alarm = createAlarm();
    alarm.setId("42");
    Links.hydrate(alarm.getAlarmDefinition(), uriInfo, AlarmDefinitionResource.ALARM_DEFINITIONS_PATH);
    assertEquals(alarm.getAlarmDefinition().getLinks().size(), 1);
    assertEquals(alarm.getAlarmDefinition().getLinks().get(0), new Link("self", base
        // Have to cut the first / off of AlarmDefinitionResource.ALARM_DEFINITIONS_PATH
        + AlarmDefinitionResource.ALARM_DEFINITIONS_PATH.substring(1) + "/"
        + ALARM_DEF_ID));
  }

}
