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

package monasca.api.resource;

import monasca.api.domain.model.alarm.Alarm;
import monasca.api.domain.model.common.Link;
import monasca.common.model.alarm.AlarmState;
import static org.testng.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.UriInfo;

@Test
public class LinksTest {
  public void shouldPrefixForHttps() {
    Links.accessedViaHttps = true;
    assertEquals(Links.prefixForHttps("http://abc123blah/blah/blah"),
        "https://abc123blah/blah/blah");
    assertEquals(Links.prefixForHttps("https://abc123blah/blah/blah"),
        "https://abc123blah/blah/blah");

    // Negative
    Links.accessedViaHttps = false;
    assertEquals(Links.prefixForHttps("http://abc123blah/blah/blah"), "http://abc123blah/blah/blah");
    assertEquals(Links.prefixForHttps("https://abc123blah/blah/blah"),
        "https://abc123blah/blah/blah");
  }

  public void shouldHydrate() throws URISyntaxException {
    checkHydrate("http://localhost:8080/");
    checkHydrate("https://localhost/");
    checkHydrate("https://localhost//");
    checkHydrate(""); // Should not happen but handle relative pathss
  }

  private void checkHydrate(final String base) throws URISyntaxException {
    final UriInfo uriInfo = mock(UriInfo.class);
    final String alarmId = "b6a454b7-b557-426a-af51-657d0a7384d6";
    final URI uri = new URI(base);
    when(uriInfo.getBaseUri()).thenReturn(uri);
    final String alarmDefinitionId = "af72b3d8-51f3-4eee-8086-535b5e7a9dc8";
    final Alarm alarm = new Alarm(alarmId, alarmDefinitionId, "Test", "LOW", null, AlarmState.OK);
    alarm.setId("42");
    Links.hydrate(alarm.getAlarmDefinition(), uriInfo, AlarmDefinitionResource.ALARM_DEFINITIONS_PATH);
    assertEquals(alarm.getAlarmDefinition().getLinks().size(), 1);
    assertEquals(alarm.getAlarmDefinition().getLinks().get(0), new Link("self", base
        // Have to cut the first / off of AlarmDefinitionResource.ALARM_DEFINITIONS_PATH
        + AlarmDefinitionResource.ALARM_DEFINITIONS_PATH.substring(1) + "/"
        + alarmDefinitionId));
  }
}
