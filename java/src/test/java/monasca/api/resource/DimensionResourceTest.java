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

package monasca.api.resource;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.testng.annotations.Test;

import monasca.api.ApiConfig;
import monasca.api.domain.model.dimension.DimensionRepo;
import monasca.api.infrastructure.persistence.PersistUtils;

import com.sun.jersey.api.client.ClientResponse;

@Test
public class DimensionResourceTest extends AbstractMonApiResourceTest {
  private DimensionRepo dimensionRepo;
  private ApiConfig apiConfig;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();

    dimensionRepo = mock(DimensionRepo.class);
    apiConfig = mock(ApiConfig.class);
    addResources(new DimensionResource(apiConfig, dimensionRepo, new PersistUtils()));
  }

  @SuppressWarnings("unchecked")
  public void shouldQueryDimensionValuesWithDefaultParams() throws Exception {

    client()
        .resource(
            "/v2.0/metrics/dimensions/names/values?dimension_name=hpcs.compute")
        .header("X-Tenant-Id", "abc").get(ClientResponse.class);
    verify(dimensionRepo).findValues(anyString(), anyString(), anyString(), anyString(),
           anyInt());
  }

  public void shouldQueryDimensionValuesWithOptionalMetricName() throws Exception {

    client()
        .resource(
            "/v2.0/metrics/dimensions/names/values?dimension_name=hpcs.compute&metric_name=cpu_utilization")
        .header("X-Tenant-Id", "abc").get(ClientResponse.class);
    verify(dimensionRepo).findValues(anyString(), anyString(), anyString(), anyString(),
           anyInt());
  }

  public void shouldQueryDimensionNamesWithDefaultParams() throws Exception {

    client()
       .resource(
           "/v2.0/metrics/dimensions/names")
       .header("X-Tenant-Id", "abc").get(ClientResponse.class);
    verify(dimensionRepo).findNames(anyString(), anyString(), anyString(), anyInt());
  }
}
