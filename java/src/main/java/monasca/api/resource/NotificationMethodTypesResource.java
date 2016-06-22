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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import com.codahale.metrics.annotation.Timed;

import monasca.api.ApiConfig;
import monasca.api.domain.model.notificationmethod.NotificationMethodType;
import monasca.api.domain.model.notificationmethod.NotificationMethodTypesRepo;
import monasca.api.infrastructure.persistence.PersistUtils;


/**
 * Notification Method resource implementation.
 */
@Path("/v2.0/notification-methods/types")
public class NotificationMethodTypesResource {


  NotificationMethodTypesRepo repo = null;
  private final PersistUtils persistUtils;


  @Inject
  public NotificationMethodTypesResource(ApiConfig config, NotificationMethodTypesRepo repo,
                                         PersistUtils persistUtils) {
    this.repo = repo;
    this.persistUtils = persistUtils;
  }

  @GET
  @Timed
  @Produces(MediaType.APPLICATION_JSON)
  public Object list(@Context UriInfo uriInfo,  @QueryParam("sort_by") String sortByStr,
              @QueryParam("offset") String offset,
              @QueryParam("limit") String limit) throws UnsupportedEncodingException {

     List<NotificationMethodType> resources = new ArrayList<NotificationMethodType>();
     for (String method_type: repo.listNotificationMethodTypes()){
       resources.add(new NotificationMethodType(method_type));
     }

     final int paging_limit = this.persistUtils.getLimit(limit);
     return Links.paginate(paging_limit, resources, uriInfo);
  }
}
