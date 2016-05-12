/*
 * Copyright (c) 2014,2016 Hewlett-Packard Development Company, L.P.
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

import monasca.api.ApiConfig;
import monasca.api.domain.model.statistic.StatisticRepo;
import monasca.api.infrastructure.persistence.PersistUtils;

import com.sun.jersey.api.client.ClientResponse;

@Test
public class StatisticResourceTest extends AbstractMonApiResourceTest {
  private StatisticRepo statisticRepo;
  private ApiConfig apiConfig;
  long timestamp;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();

    statisticRepo = mock(StatisticRepo.class);
    apiConfig = mock(ApiConfig.class);
    addResources(new StatisticResource(apiConfig, statisticRepo, new PersistUtils()));
  }

  @SuppressWarnings("unchecked")
  public void shouldQueryWithDefaultParams() throws Exception {

    client()
        .resource(
            "/v2.0/metrics/statistics?name=cpu_utilization&start_time=2013-11-20T18:43Z&dimensions=service:hpcs.compute,%20instance_id:123&statistics=avg,%20min,%20max&period=60")
        .header("X-Tenant-Id", "abc").get(ClientResponse.class);
    verify(statisticRepo).find(anyString(), anyString(), any(Map.class), any(DateTime.class),
        any(DateTime.class), any(List.class), anyInt(), any(String.class), anyInt(),
        anyBoolean(), anyString());
  }

  public void queryShouldThrowOnInvalidDateFormat() throws Exception {
    ClientResponse response =
        client()
            .resource(
                "/v2.0/metrics/statistics?name=cpu_utilization&dimensions=service:hpcs.compute,%20instance_id:123&start_time=2013-1120&statistics=avg")
            .header("X-Tenant-Id", "abc").get(ClientResponse.class);
    assertEquals(response.getStatus(), 422);
  }

  public void queryShouldThrowOnInvalidPeriodDataType() throws Exception {
    ClientResponse response =
        client()
            .resource(
                "/v2.0/metrics/statistics?name=cpu_utilization&dimensions=service:hpcs.compute,%20instance_id:123&start_time=2013-11-20T18:43Z&statistics=avg&period=foo")
            .header("X-Tenant-Id", "abc").get(ClientResponse.class);
    assertEquals(response.getStatus(), 422);
  }

  public void queryShouldThrowOnInvalidStatistics() throws Exception {
    ClientResponse response =
        client()
            .resource(
                "/v2.0/metrics/statistics?name=cpu_utilization&dimensions=service:hpcs.compute,%20instance_id:123&start_time=2013-11-20T18:43Z&statistics=foo,bar")
            .header("X-Tenant-Id", "abc").get(ClientResponse.class);
    assertEquals(response.getStatus(), 422);
  }

  public void queryShouldThrowOnInvalidPeriod() throws Exception {
    ClientResponse response =
        client()
            .resource(
                "/v2.0/metrics/statistics?name=cpu_utilization&dimensions=service:hpcs.compute,%20instance_id:123&start_time=2013-11-20T18:43Z&statistics=avg&period=foo")
            .header("X-Tenant-Id", "abc").get(ClientResponse.class);
    assertEquals(response.getStatus(), 422);
  }
}
