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

import com.codahale.metrics.annotation.Timed;

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import monasca.api.domain.model.version.Version;
import monasca.api.domain.model.version.VersionRepo;
import monasca.api.infrastructure.persistence.PersistUtils;

/**
 * Version resource implementation.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource {
  private final VersionRepo repository;
  private final PersistUtils persistUtils;

  @Inject
  public VersionResource(VersionRepo repository,
                         PersistUtils persistUtils) {
    this.repository = repository;
    this.persistUtils = persistUtils;
  }

  @GET
  @Timed
  public Object list(@Context UriInfo uriInfo,
                     @QueryParam("offset") String offset,
                     @QueryParam("limit") String limit) throws UnsupportedEncodingException {

    return Links.paginate(this.persistUtils.getLimit(limit),
                          Links.hydrate(repository.find(), uriInfo), uriInfo);
  }

  @GET
  @Timed
  @Path("{version_id}")
  public Version get(@Context UriInfo uriInfo, @PathParam("version_id") String versionId) {
    return Links.hydrate(repository.findById(versionId), uriInfo, true);
  }
}
