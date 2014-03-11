package com.hpcloud.mon.app.validation;

import static org.testng.Assert.assertEquals;

import javax.ws.rs.WebApplicationException;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.metric.MetricDefinition;

/**
 * @author Jonathan Halterman
 */
@Test
public class AlarmExpressionsTest {
  public void shouldNormalizeFields() {
    AlarmExpression expr = AlarmValidation.validateNormalizeAndGet("avg(hpcs.compute.net_out_bytes{instance_id=5, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, az=1}) > 4");
    MetricDefinition metricDef = expr.getSubExpressions().get(0).getMetricDefinition();

    assertEquals(metricDef.name, "hpcs.compute.net_out_bytes");
    assertEquals(
        metricDef.dimensions,
        ImmutableMap.builder()
            .put("instance_id", "5")
            .put("instance_uuid", "0ff588fc-d298-482f-bb11-4b52d56801a4")
            .put("az", "1")
            .build());
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldThrowOnInvalidOperator() throws Exception {
    AlarmValidation.validateNormalizeAndGet("avg(hpcs.compute.net_out_bytes) & abc");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldThrowOnDuplicateDimensions() throws Exception {
    AlarmValidation.validateNormalizeAndGet("avg(hpcs.compute.net_out_bytes{instance_id=5, instance_id=4}) > 4");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldThrowOnInvalidThreshold() throws Exception {
    AlarmValidation.validateNormalizeAndGet("avg(hpcs.compute.net_out_bytes) > abc");
  }
}
