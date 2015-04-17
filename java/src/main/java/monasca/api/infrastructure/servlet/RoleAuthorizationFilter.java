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
import monasca.common.middleware.AuthConstants;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import static monasca.api.infrastructure.servlet.PostAuthenticationFilter.X_MONASCA_AGENT;

public class RoleAuthorizationFilter implements ContainerRequestFilter {
  private static final Logger logger = LoggerFactory.getLogger
      (ContainerRequestFilter.class);
    @Context
    private HttpServletRequest httpServletRequest;
    private static final String[] VALID_MONASCA_AGENT_POST_PATHS = new String[] { "/v2.0/metrics" };
    private static final String[] VALID_MONASCA_AGENT_GET_PATHS =  new String[] { "/", "/v2.0" };

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        String method = containerRequest.getMethod();
        Object isAgent = httpServletRequest.getAttribute(X_MONASCA_AGENT);
        String pathInfo = httpServletRequest.getPathInfo();

        // X_MONASCA_AGENT is only set if the only valid role for this user is an agent role
        if (isAgent != null) {
            if (!(method.equals("POST") && validPath(pathInfo, VALID_MONASCA_AGENT_POST_PATHS)) && 
                !(method.equals("GET") && validPath(pathInfo, VALID_MONASCA_AGENT_GET_PATHS))) { 
                logger.warn("User {} is missing a required role to {} on {}",
                                                    httpServletRequest.getAttribute(AuthConstants.AUTH_USER_NAME),
                                                    method, pathInfo);
                throw Exceptions.badRequest("User is missing a required role to perform this request");
            }
        }
        return containerRequest;
    }

    private boolean validPath(String pathInfo, String[] paths) {
      // Make the comparison easier by getting rid of trailing slashes
      while (!pathInfo.isEmpty() && !"/".equals(pathInfo) && pathInfo.endsWith("/")) {
        pathInfo = pathInfo.substring(0, pathInfo.length() - 1);
      }
      for (final String validPath : paths) {
        if (validPath.equals(pathInfo)) {
          return true;
        }
      }
      return false;
    }
}
