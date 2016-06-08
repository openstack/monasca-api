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

package monasca.api.app.validation;

import static org.testng.Assert.assertEquals;

import javax.ws.rs.WebApplicationException;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import monasca.common.model.alarm.AlarmExpression;
import monasca.common.model.metric.MetricDefinition;

@Test
public class AlarmExpressionsTest {
  public void shouldNormalizeFields() {
    AlarmExpression expr =
        AlarmValidation
            .validateNormalizeAndGet("avg(hpcs.compute.net_out_bytes{instance_id=5, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, az=1}) > 4");
    MetricDefinition metricDef = expr.getSubExpressions().get(0).getMetricDefinition();

    assertEquals(metricDef.name, "hpcs.compute.net_out_bytes");
    assertEquals(
        metricDef.dimensions,
        ImmutableMap.builder().put("instance_id", "5")
            .put("instance_uuid", "0ff588fc-d298-482f-bb11-4b52d56801a4").put("az", "1").build());
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldThrowOnInvalidOperator() throws Exception {
    AlarmValidation.validateNormalizeAndGet("avg(hpcs.compute.net_out_bytes) & abc");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldThrowOnDuplicateDimensions() throws Exception {
    AlarmValidation
        .validateNormalizeAndGet("avg(hpcs.compute.net_out_bytes{instance_id=5, instance_id=4}) > 4");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldThrowOnInvalidThreshold() throws Exception {
    AlarmValidation.validateNormalizeAndGet("avg(hpcs.compute.net_out_bytes) > abc");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldThrowOnMalformedDeterministicKeyword() throws Exception {
    AlarmValidation.validateNormalizeAndGet("avg(hpcs.compute.net_out_bytes,determ) > 1");
  }
}
