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

package monasca.api.infrastructure.servlet;

import monasca.api.resource.exception.Exceptions;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import static monasca.api.infrastructure.servlet.PostAuthenticationFilter.X_MONASCA_AGENT;

public class RoleAuthorizationFilter implements ContainerRequestFilter {

    @Context
    private HttpServletRequest httpServletRequest;
    private static final String VALID_MONASCA_AGENT_PATH = "/v2.0/metrics";

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        String method = containerRequest.getMethod();
        Object isAgent = httpServletRequest.getAttribute(X_MONASCA_AGENT);
        String pathInfo = httpServletRequest.getPathInfo();

        if (isAgent != null) {
            if (!pathInfo.equals(VALID_MONASCA_AGENT_PATH)) {
                throw Exceptions.badRequest("Tenant is missing a required role to perform this request");
            } else if (pathInfo.equals(VALID_MONASCA_AGENT_PATH) && !method.equals("POST")) {
                throw Exceptions.badRequest("Tenant is missing a required role to perform this request");
            }
        }
        return containerRequest;
    }
}
