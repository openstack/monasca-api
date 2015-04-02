/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P.
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
package monasca.api.resource.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import monasca.api.domain.exception.MultipleMetricsException;

@Provider
public class MultipleMetricsExceptionMapper implements ExceptionMapper<MultipleMetricsException> {

  private static final String
      MULTIPLE_METRICS_ERROR_MSG =
      "Found multiple metrics matching metric name and dimensions. "
      + "Please refine your search criteria using a unique metric name or additional dimensions. "
      + "Alternatively, you may specify 'merge_metrics=true' as a query param to combine "
      + "all metrics matching search criteria into a single series.";


  @Override
  public Response toResponse(MultipleMetricsException exception) {

    String details = String.format("search criteria: {metric name: %s, dimensions: %s}",
                                   exception.getMetricName(),  exception.getDimensions());

    return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON).entity(
        Exceptions
            .buildLoggedErrorMessage(Exceptions.FaultType.CONFLICT, MULTIPLE_METRICS_ERROR_MSG,
                                     details, null)).build();

  }
}
