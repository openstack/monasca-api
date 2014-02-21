package com.hpcloud.mon.resource;

import static com.hpcloud.dropwizard.JsonHelpers.fromJson;
import static com.hpcloud.dropwizard.JsonHelpers.jsonFixture;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
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

import org.joda.time.DateTime;
import org.testng.annotations.Test;

import com.hpcloud.mon.app.MetricService;
import com.hpcloud.mon.app.command.CreateMetricCommand;
import com.hpcloud.mon.domain.model.metric.DatapointRepository;
import com.hpcloud.mon.resource.exception.ErrorMessages;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @author Todd Walk
 */
@Test(enabled = false)
@SuppressWarnings("unchecked")
public class MetricResourceTest extends AbstractMonApiResourceTest {
  private Map<String, String> dimensions;
  private MetricService service;
  private DatapointRepository datapointRepo;
  long timestamp;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();
    dimensions = new HashMap<String, String>();
    dimensions.put("instance_id", "937");
    dimensions.put("az", "2");
    dimensions.put("instance_uuid", "abc123");
    dimensions.put("metric_name", "test_metrictype");
    timestamp = System.currentTimeMillis() / 1000L;

    service = mock(MetricService.class);
    doNothing().when(service).create(any(List.class), anyString(), anyString(), anyString());

    datapointRepo = mock(DatapointRepository.class);
    addResources(new MetricResource(service, datapointRepo));
  }

  public void shouldCreate() {
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        dimensions, timestamp, 22.0));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString(), anyString());
  }

  public void shouldCreateWithNonNumericAZ() {
    Map<String, String> dimensions_local = new HashMap<String, String>();
    dimensions_local.put("instance_id", "937");
    dimensions_local.put("az", "region-a");
    dimensions_local.put("instance_uuid", "abc123");
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        dimensions_local, timestamp, 22.0));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString(), anyString());
  }

  public void shouldCreateWithNullTimestamp() throws Exception {
    String json = jsonFixture("fixtures/metricNullTimestamp.json");
    CreateMetricCommand metric = fromJson(json, CreateMetricCommand.class);
    ClientResponse response = createResponseFor(metric);

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString(), anyString());
  }

  public void shouldCreateWithZeroValue() {
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        dimensions, timestamp, 0.0));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString(), anyString());
  }

  public void shouldCreateWithZeroTimestamp() {
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        dimensions, 0L, 0.0));

    assertEquals(response.getStatus(), 204);
    verify(service).create(any(List.class), eq("abc"), anyString(), anyString());
  }

  public void shouldCreateList() {
    double timestampD = (double) timestamp;
    double[][] timeValues = { { timestampD, 22.0 }, { timestampD + 1, 23.0 } };
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        dimensions, timestamp, timeValues));

    assertEquals(response.getStatus(), 204);
  }

  public void shouldErrorOnPostWithCrossTenant() {
    ClientResponse response = createResponseForCrossTenant(new CreateMetricCommand(
        "test_namespace", dimensions, timestamp, 22.0), "def");

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("forbidden", 403,
        "Project abc cannot POST cross tenant");
  }

  public void shouldErrorOnCreateWithIllegalCharsInNamespace() {
    ClientResponse response = createResponseFor(new CreateMetricCommand("hpcs@.compute%",
        dimensions, timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "namespace hpcs@.compute% may only contain: a-z A-Z 0-9 _ - .");
  }

  public void shouldErrorOnCreateWithTooLongNamespace() {
    ClientResponse response = createResponseFor(new CreateMetricCommand(
        "1234567890123456789012345678901234567890123456789012345678901234567890", dimensions,
        timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class))
        .matches(
            "unprocessable_entity",
            422,
            "namespace 1234567890123456789012345678901234567890123456789012345678901234567890 must be 64 characters or less");
  }

  public void shouldErrorOnCreateWithReservedNamespace() {
    ClientResponse response = createResponseFor(new CreateMetricCommand("hpcs.compute", dimensions,
        timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("forbidden", 403,
        "Project abc cannot create metrics for hpcs.compute");
  }

  public void shouldErrorOnCreateWithNullNamespace() throws Exception {
    String json = jsonFixture("fixtures/metricNullNamespace.json");
    CreateMetricCommand metric = fromJson(json, CreateMetricCommand.class);
    ClientResponse response = createResponseFor(metric);

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("bad_request", 400,
        "The request entity had the following errors:", "[namespace may not be empty (was null)]");
  }

  public void shouldErrorOnCreateWithMissingDimensionKey() {
    Map<String, String> local_dimensions = new HashMap<String, String>();
    local_dimensions.put("instance_id", "937");
    local_dimensions.put("az", "2");
    local_dimensions.put("instance_uuid", "abc123");
    local_dimensions.put("", "abc");
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        local_dimensions, timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Dimension name cannot be empty");
  }

  public void shouldErrorOnCreateWithBadValue() {
    Map<String, String> local_dimensions = new HashMap<String, String>();
    local_dimensions.put("blah", "");
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        local_dimensions, timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Dimension name blah cannot have an emtpy value");
  }

  public void shouldErrorOnCreateWithMissingDimensionValue() {
    Map<String, String> local_dimensions = new HashMap<String, String>();
    local_dimensions.put("instance_id", "937");
    local_dimensions.put("az", "2");
    local_dimensions.put("instance_uuid", "abc123");
    local_dimensions.put("flavor_id", "");
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        local_dimensions, timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Dimension name flavor_id cannot have an emtpy value");
  }

  public void shouldErrorOnCreateWithTooLongDimensionType() {
    Map<String, String> local_dimensions = new HashMap<String, String>();
    local_dimensions.put("instance_id", "937");
    local_dimensions.put("az", "2");
    local_dimensions.put("instance_uuid", "abc123");
    local_dimensions.put("abcdefghijabcdefghijabcdefghijabcdefghijabcdefghij1", "abc123");
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        local_dimensions, timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class))
        .matches("unprocessable_entity", 422,
            "Dimension name abcdefghijabcdefghijabcdefghijabcdefghijabcdefghij1 must be 50 characters or less");
  }

  public void shouldErrorOnCreateWithTooLongDimensionValue() {
    Map<String, String> local_dimensions = new HashMap<String, String>();
    local_dimensions.put("instance_id", "937");
    local_dimensions.put("az", "2");
    local_dimensions.put("instance_uuid", "abc123");
    local_dimensions.put(
        "abc",
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789abc");
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        local_dimensions, timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class))
        .matches(
            "unprocessable_entity",
            422,
            "Dimension value 012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789abc must be 300 characters or less");
  }

  public void shouldErrorOnCreateWithHighTimestamp() {
    long local_timestamp = timestamp + 1000;
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        dimensions, local_timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Timestamp " + local_timestamp + " is out of range or not legal");
  }

  public void shouldErrorOnCreateWithLowTimestamp() {
    long local_timestamp = timestamp - 1309600;
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        dimensions, local_timestamp, 22.0));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Timestamp " + local_timestamp + " is out of range or not legal");
  }

  public void shouldErrorOnCreateWithHighValue() {
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        dimensions, timestamp, 1.174271e+109));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Value 1.174271E109 is out of range or not legal");
  }

  public void shouldErrorOnCreateWithLowValue() {
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        dimensions, timestamp, 8.515920e-110));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Value 8.51592E-110 is out of range or not legal");
  }

  public void shouldErrorOnCreateWithTimestampHighInTimeValues() {
    double timestampD = (double) timestamp + 1000;
    double[][] timeValues = { { timestampD, 22.0 }, { timestampD + 1, 23.0 } };
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        dimensions, timestamp, timeValues));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Timestamp " + (long) timestampD + " is out of range or not legal");
  }

  public void shouldErrorOnCreateWithValueHighInTimeValues() {
    double timestampD = (double) timestamp;
    double[][] timeValues = { { timestampD, 1.174271e+109 }, { timestampD + 1, 23.0 } };
    ClientResponse response = createResponseFor(new CreateMetricCommand("test_namespace",
        dimensions, timestamp, timeValues));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Value 1.174271E109 is out of range or not legal");
  }

  public void shouldCreateWithAggregate() throws Exception {
    String json = jsonFixture("fixtures/metricAggregate.json");
    CreateMetricCommand[] metrics = fromJson(json, CreateMetricCommand[].class);
    ClientResponse response = createResponseForAggregate(metrics);

    assertEquals(response.getStatus(), 204);
  }

  public void shouldRequireMetricValuesToBeDoubles() throws Exception {
    ClientResponse response = createResponseFor("{\"namespace\": \"foo\",\"timestamp\": 1380750420,\"value\": \"foo\"}");
    assertEquals(response.getStatus(), 400);
  }

  public void shouldQueryWithDefaultParams() throws Exception {
    client().resource(
        "/v2.0/metrics?namespace=hpcs.compute&start_time=2013-11-20T18:43Z"
            + "&dimensions=metric_name:cpu_utilization,%20instance_id:123,az:1,instance_uuid:8499a88b-5830-4c85-9507-fe7d4382919c&statistics=avg,%20min,%20max&period=60")
        .header("X-Tenant-Id", "abc")
        .get(ClientResponse.class);
    verify(datapointRepo).find(anyString(), anyString(), any(DateTime.class), any(DateTime.class),
        any(Map.class), any(List.class), anyInt());
  }

  public void queryShouldThrowOnInvalidDateFormat() throws Exception {
    ClientResponse response = client().resource(
        "/v2.0/metrics?namespace=hpcs.compute&dimensions=metric_name:cpu_utilization,%20instance_id:123&start_time=2013-1120&statistics=avg")
        .header("X-Tenant-Id", "abc")
        .get(ClientResponse.class);
    assertEquals(response.getStatus(), 422);
  }

  public void queryShouldThrowOnInvalidPeriodDataType() throws Exception {
    ClientResponse response = client().resource(
        "/v2.0/metrics?namespace=hpcs.compute&dimensions=metric_name:cpu_utilization,%20instance_id:123&start_time=2013-11-20T18:43Z&statistics=avg&period=foo")
        .header("X-Tenant-Id", "abc")
        .get(ClientResponse.class);
    assertEquals(response.getStatus(), 422);
  }

  public void queryShouldThrowOnInvalidStatistics() throws Exception {
    ClientResponse response = client().resource(
        "/v2.0/metrics?namespace=hpcs.compute&dimensions=metric_name:cpu_utilization,%20instance_id:123&start_time=2013-11-20T18:43Z&statistics=foo,bar")
        .header("X-Tenant-Id", "abc")
        .get(ClientResponse.class);
    assertEquals(response.getStatus(), 422);
  }

  public void queryShouldThrowOnInvalidPeriod() throws Exception {
    ClientResponse response = client().resource(
        "/v2.0/metrics?namespace=hpcs.compute&dimensions=metric_name:cpu_utilization,%20instance_id:123&start_time=2013-11-20T18:43Z&statistics=avg&period=foo")
        .header("X-Tenant-Id", "abc")
        .get(ClientResponse.class);
    assertEquals(response.getStatus(), 422);
  }

  private ClientResponse createResponseFor(Object request) {
    return client().resource("/v2.0/metrics")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class, request);
  }

  private ClientResponse createResponseForAggregate(Object request) {
    return client().resource("/v2.0/metrics/aggregate")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class, request);
  }

  private ClientResponse createResponseForCrossTenant(Object request, String crossTenantId) {
    return client().resource("/v2.0/metrics?tenant_id=" + crossTenantId)
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class, request);
  }
}
