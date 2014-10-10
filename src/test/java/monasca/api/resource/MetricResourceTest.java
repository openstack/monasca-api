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

import static com.hpcloud.dropwizard.JsonHelpers.fromJson;
import static com.hpcloud.dropwizard.JsonHelpers.jsonFixture;
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

import monasca.api.app.MetricService;
import monasca.api.app.command.CreateMetricCommand;
import monasca.api.domain.model.metric.MetricDefinitionRepository;
import monasca.api.resource.exception.ErrorMessages;
import com.sun.jersey.api.client.ClientResponse;

@Test
public class MetricResourceTest extends AbstractMonApiResourceTest {
  private Map<String, String> dimensions;
  private MetricService service;
  private MetricDefinitionRepository metricRepo;
  long timestamp;

  @Override
  @SuppressWarnings("unchecked")
  protected void setupResources() throws Exception {
    super.setupResources();
    dimensions = new HashMap<String, String>();
    dimensions.put("instance_id", "937");
    dimensions.put("service", "foo.compute");
    timestamp = System.currentTimeMillis() / 1000L;

    service = mock(MetricService.class);
    doNothing().when(service).create(any(List.class), anyString(), anyString());

    metricRepo = mock(MetricDefinitionRepository.class);
    addResources(new MetricResource(service, metricRepo));
  }

  @SuppressWarnings("unchecked")
  public void shouldCreate() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, timestamp, 22.0));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  @SuppressWarnings("unchecked")
  public void shouldCreateSet() throws Exception {
    String json = jsonFixture("fixtures/metricSet.json");
    CreateMetricCommand[] metrics = fromJson(json, CreateMetricCommand[].class);
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
        createResponseFor(new CreateMetricCommand("test_metrictype", dims, timestamp, 22.0));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  @SuppressWarnings("unchecked")
  public void shouldCreateWithoutTimestamp() throws Exception {
    String json = jsonFixture("fixtures/metricWithoutTimestamp.json");
    CreateMetricCommand metric = fromJson(json, CreateMetricCommand.class);
    ClientResponse response = createResponseFor(metric);

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  @SuppressWarnings("unchecked")
  public void shouldCreateWithoutDimensions() throws Exception {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", null, timestamp, 22.0));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  @SuppressWarnings("unchecked")
  public void shouldCreateWithZeroValue() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, timestamp, 0.0));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  @SuppressWarnings("unchecked")
  public void shouldCreateWithZeroTimestamp() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, 0L, 0.0));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString());
  }

  public void shouldCreateList() {
    double timestampD = (double) timestamp;
    double[][] timeValues = { {timestampD, 22.0}, {timestampD + 1, 23.0}};
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, timestamp,
            timeValues));

    assertEquals(response.getStatus(), 204);
  }

  public void shouldErrorOnPostWithCrossTenant() {
    ClientResponse response =
        createResponseForCrossTenant(new CreateMetricCommand("test_metrictype", dimensions,
            timestamp, 22.0), "def");

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("forbidden", 403,
        "Project abc cannot POST cross tenant");
  }

  public void shouldErrorOnCreateWithIllegalCharsInName() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("hpcs@.compute%", dimensions, timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Metric name hpcs@.compute% may only contain: a-z A-Z 0-9 _ - .");
  }

  public void shouldErrorOnCreateWithTooLongName() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand(
            "1234567890123456789012345678901234567890123456789012345678901234567890", dimensions,
            timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "[name size must be between 1 and 64");
  }

  public void shouldErrorOnCreateWithReservedService() {
    Map<String, String> dims = new HashMap<>();
    dims.put("instance_id", "937");
    dims.put("service", "hpcs.compute");
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("foo", dims, timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("forbidden", 403,
        "Project abc cannot POST metrics for the hpcs service");
  }

  public void shouldErrorOnCreateWithoutName() throws Exception {
    String json = jsonFixture("fixtures/metricWithoutName.json");
    ClientResponse response = createResponseFor(json);

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "[name may not be empty");
  }

  public void shouldErrorOnCreateWithMissingDimensionKey() throws Exception {
    String json = jsonFixture("fixtures/metricWithoutDimensionName.json");
    ClientResponse response = createResponseFor(json);

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Dimension name cannot be empty");
  }

  public void shouldErrorOnCreateWithBadValue() {
    Map<String, String> dims = new HashMap<String, String>();
    dims.put("blah", "");
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dims, timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Dimension blah cannot have an empty value");
  }

  public void shouldErrorOnCreateWithMissingDimensionValue() {
    Map<String, String> dims = new HashMap<String, String>();
    dims.put("instance_id", "937");
    dims.put("az", "2");
    dims.put("instance_uuid", "abc123");
    dims.put("flavor_id", "");
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dims, timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Dimension flavor_id cannot have an empty value");
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
        createResponseFor(new CreateMetricCommand("test_metrictype", dims, timestamp, 22.0));

    ErrorMessages
        .assertThat(response.getEntity(String.class))
        .matches(
            "unprocessable_entity",
            422,
            "Dimension name 012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789abc must be 255 characters or less");
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
        createResponseFor(new CreateMetricCommand("test_metrictype", dims, timestamp, 22.0));

    ErrorMessages
        .assertThat(response.getEntity(String.class))
        .matches(
            "unprocessable_entity",
            422,
            "Dimension value 012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789abc must be 255 characters or less");
  }

  public void shouldErrorOnCreateWithHighTimestamp() {
    long local_timestamp = timestamp + 1000;
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, local_timestamp,
            22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Timestamp " + local_timestamp + " is out of legal range");
  }

  public void shouldErrorOnCreateWithLowTimestamp() {
    long local_timestamp = timestamp - 1309600;
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, local_timestamp,
            22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Timestamp " + local_timestamp + " is out of legal range");
  }

  public void shouldErrorOnCreateWithHighValue() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, timestamp,
            1.174271e+109));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Value 1.174271E109 is out of legal range");
  }

  public void shouldErrorOnCreateWithLowValue() {
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, timestamp,
            8.515920e-110));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Value 8.51592E-110 is out of legal range");
  }

  public void shouldErrorOnCreateWithTimestampHighInTimeValues() {
    double timestampD = (double) timestamp + 1000;
    double[][] timeValues = { {timestampD, 22.0}, {timestampD + 1, 23.0}};
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, timestamp,
            timeValues));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Timestamp " + (long) timestampD + " is out of legal range");
  }

  public void shouldErrorOnCreateWithValueHighInTimeValues() {
    double timestampD = (double) timestamp;
    double[][] timeValues = { {timestampD, 1.174271e+109}, {timestampD + 1, 23.0}};
    ClientResponse response =
        createResponseFor(new CreateMetricCommand("test_metrictype", dimensions, timestamp,
            timeValues));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Value 1.174271E109 is out of legal range");
  }

  public void shouldRequireMetricValuesToBeDoubles() throws Exception {
    ClientResponse response =
        createResponseFor("{\"namespace\": \"foo\",\"timestamp\": 1380750420,\"value\": \"foo\"}");
    assertEquals(response.getStatus(), 400);
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
}
