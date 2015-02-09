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
package monasca.api.resource.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import monasca.api.domain.exception.EntityExistsException;
import monasca.api.resource.exception.Exceptions.FaultType;

@Provider
public class EntityExistsExceptionMapper implements ExceptionMapper<EntityExistsException> {
  @Override
  public Response toResponse(EntityExistsException e) {
    return Response.status(Status.CONFLICT).type(MediaType.APPLICATION_JSON)
        .entity(Exceptions.buildLoggedErrorMessage(FaultType.CONFLICT, e.getMessage())).build();
  }
}
