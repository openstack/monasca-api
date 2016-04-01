/*
 * Copyright (c) 2014,2016 Hewlett Packard Enterprise Development Company, L.P.
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

import static monasca.common.dropwizard.JsonHelpers.fromJson;
import static monasca.common.dropwizard.JsonHelpers.jsonFixture;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.testng.annotations.Test;

import monasca.api.ApiConfig;
import monasca.api.app.MetricService;
import monasca.api.app.command.CreateMetricCommand;
import monasca.api.domain.model.metric.MetricDefinitionRepo;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.api.resource.exception.ErrorMessages;
import com.sun.jersey.api.client.ClientResponse;

@Test
public class MetricResourceTest extends AbstractMonApiResourceTest {
  private Map<String, String> dimensions;
  private Map<String, String> valueMeta;
  private MetricService service;
  private MetricDefinitionRepo metricRepo;
  long timestamp;

  @Override
  @SuppressWarnings("unchecked")
  protected void setupResources() throws Exception {
    super.setupResources();
    dimensions = new HashMap<String, String>();
    dimensions.put("instance_id", "937");
    dimensions.put("service", "foo.compute");
    valueMeta = new HashMap<String, String>();
    valueMeta.put("rc", "404");
    valueMeta.put("errorMsg", "Not Found");
    timestamp = System.currentTimeMillis();

    service = mock(MetricService.class);
    doNothing().when(service).create(any(List.class), anyString(), anyString());

    metricRepo = mock(MetricDefinitionRepo.class);
    addResources(new MetricResource(new ApiConfig(), service, metricRepo, new PersistUtils()));
  }

  @SuppressWarnings("unchecked")
  public void shouldCreate() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, timestamp, 22.0,
            valueMeta));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  @SuppressWarnings("unchecked")
  public void shouldCreateSet() throws Exception {
    String json = jsonFixture("fixtures/metricSet.json");
    CreateMetricCommand[] metrics = fromJson(json, CreateMetricCommand[].class);
    metrics[0].timestamp = timestamp;
    metrics[1].timestamp = timestamp;
    ClientResponse response = createResponseFor(metrics);

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  @SuppressWarnings("unchecked")
  public void shouldCreateWithNonNumericAZ() {
    Map<String, String> dims = new HashMap<String, String>();
    dims.put("instance_id", "937");
    dims.put("service", "foo.compute");
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dims, timestamp, 22.0,
            valueMeta));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  @SuppressWarnings("unchecked")
  public void shouldErrorOnCreateWithoutTimestamp() throws Exception {
    String json = jsonFixture("fixtures/metricWithoutTimestamp.json");
    CreateMetricCommand metric = fromJson(json, CreateMetricCommand.class);
    ClientResponse response = createResponseFor(metric);

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
            "[timestamp may not be null");
  }

  @SuppressWarnings("unchecked")
  public void shouldCreateWithoutDimensions() throws Exception {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", null, timestamp, 22.0,
            valueMeta));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  @SuppressWarnings("unchecked")
  public void shouldCreateWithZeroValue() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, timestamp, 0.0,
            valueMeta));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  @SuppressWarnings("unchecked")
  public void shouldCreateWithNegativeValue() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, timestamp, -1.0,
            valueMeta));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  @SuppressWarnings("unchecked")
  public void shouldErrorOnCreateWithZeroTimestamp() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, 0L, 0.0,
            valueMeta));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
            String.format("Timestamp %s is out of legal range", 0L));
  }

  public void shouldErrorOnPostWithCrossTenant() {
    ClientResponse response =
        createResponseForCrossTenant(new CreateMetricCommand("test_metrictype", dimensions,
            timestamp, 22.0, valueMeta), "def");

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("forbidden", 403,
        "Project abc cannot POST cross tenant");
  }

  public void shouldErrorOnCreateWithIllegalCharsInName() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("hpcs{.compute%", dimensions, timestamp, 22.0,
            valueMeta));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Metric name hpcs{.compute% may not contain: > < = { } ( ) ' \" \\ , ; &");
  }

  public void shouldErrorOnCreateWithTooLongName() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand(
            "1234567890123456789012345678901234567890123456789012345678901234567890" +
            "1234567890123456789012345678901234567890123456789012345678901234567890" +
            "1234567890123456789012345678901234567890123456789012345678901234567890" +
            "1234567890123456789012345678901234567890123456789012345678901234567890" +
            "1234567890123456789012345678901234567890123456789012345678901234567890", dimensions,
            timestamp, 22.0, valueMeta));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        String.format("[name size must be between 1 and %d", CreateMetricCommand.MAX_NAME_LENGTH));
  }

  public void shouldErrorOnCreateWithReservedService() {
    Map<String, String> dims = new HashMap<>();
    dims.put("instance_id", "937");
    dims.put("service", "hpcs.compute");
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("foo", dims, timestamp, 22.0,
            valueMeta));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("forbidden", 403,
        "Project abc cannot POST metrics for the hpcs service");
  }

  public void shouldErrorOnCreateWithoutName() throws Exception {
    String json = jsonFixture("fixtures/metricWithoutName.json");
    CreateMetricCommand metric = fromJson(json, CreateMetricCommand.class);
    metric.timestamp = timestamp;
    ClientResponse response = createResponseFor(metric);
    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "[name may not be empty");
  }

  public void shouldErrorOnCreateWithMissingDimensionKey() throws Exception {
    String json = jsonFixture("fixtures/metricWithoutDimensionName.json");
    ClientResponse response = createResponseFor(json);

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Dimension name cannot be empty");
  }

  public void shouldErrorOnCreateWithBadDimensionValue() {
    Map<String, String> dims = new HashMap<String, String>();
    dims.put("blah", "");
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dims, timestamp, 22.0,
            valueMeta));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Dimension 'blah' cannot have an empty value");
  }

  public void shouldErrorOnCreateWithMissingDimensionValue() {
    Map<String, String> dims = new HashMap<String, String>();
    dims.put("instance_id", "937");
    dims.put("az", "2");
    dims.put("instance_uuid", "abc123");
    dims.put("flavor_id", "");
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dims, timestamp, 22.0,
            valueMeta));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Dimension 'flavor_id' cannot have an empty value");
  }

  public void shouldErrorOnCreateWithTooLongDimensionName() {
    Map<String, String> dims = new HashMap<String, String>();
    dims.put("instance_id", "937");
    dims.put("az", "2");
    dims.put("instance_uuid", "abc123");
    dims.put(
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789abc",
        "abc123");
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dims, timestamp, 22.0,
            valueMeta));

    ErrorMessages
        .assertThat(response.getEntity(String.class))
        .matches(
            "unprocessable_entity",
            422,
            "Dimension name '012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789abc' must be 255 characters or less");
  }

  public void shouldErrorOnCreateWithTooLongDimensionValue() {
    Map<String, String> dims = new HashMap<String, String>();
    dims.put("instance_id", "937");
    dims.put("az", "2");
    dims.put("instance_uuid", "abc123");
    dims.put(
        "abc",
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789abc");
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dims, timestamp, 22.0,
            valueMeta));

    ErrorMessages
        .assertThat(response.getEntity(String.class))
        .matches(
            "unprocessable_entity",
            422,
            "Dimension 'abc' value '012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789abc' must be 255 characters or less");
  }

  public void shouldErrorOnCreateWithHighTimestamp() {
    long local_timestamp = timestamp + 1000000;
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, local_timestamp,
            22.0, valueMeta));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Timestamp " + local_timestamp + " is out of legal range");
  }

  public void shouldErrorOnCreateWithLowTimestamp() {
    long local_timestamp = timestamp - 1309600000;
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, local_timestamp,
            22.0, valueMeta));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Timestamp " + local_timestamp + " is out of legal range");
  }

  public void shouldErrorOnCreateWithValuesToBeString() throws Exception {
    ClientResponse response =
        createResponseFor("{\"namespace\": \"foo\",\"timestamp\": 1380750420,\"value\": \"foo\"}");

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
            "Unable to process the provided JSON");
  }

  private ClientResponse createResponseFor(Object request) {
    return client().resource("/v2.0/metrics").header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON).post(ClientResponse.class, request);
  }

  private ClientResponse createResponseForCrossTenant(Object request, String crossTenantId) {
    return client().resource("/v2.0/metrics?tenant_id=" + crossTenantId)
        .header("X-Tenant-Id", "abc").header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class, request);
  }

  public void shouldErrorOnCreateWithoutValue() throws Exception {
    String json = jsonFixture("fixtures/metricWithoutValue.json");
    CreateMetricCommand metric = fromJson(json, CreateMetricCommand.class);
    metric.timestamp = timestamp;
    ClientResponse response = createResponseFor(metric);

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
            "[value may not be null");
  }
}