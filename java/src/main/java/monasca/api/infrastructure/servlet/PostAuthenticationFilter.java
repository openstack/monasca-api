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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import monasca.api.infrastructure.servlet.PreAuthenticationFilter.ErrorCapturingServletResponseWrapper;

/**
 * Authenticates requests using header information from the CsMiddleware. Provides the X-TENANT-ID
 * servlet attribute as a request header. Intended to be added to a servlet filter chain after the
 * CsMiddleware TokenAuth filter.
 */
public class PostAuthenticationFilter implements Filter {
  static final String CONFIRMED_STATUS = "CONFIRMED";
  static final String X_ROLES_ATTRIBUTE = "X-ROLES";
  static final String X_MONASCA_AGENT = "X-MONASCA_AGENT";
  static final String X_IDENTITY_STATUS_ATTRIBUTE = "X-IDENTITY-STATUS";
  private static final String X_TENANT_ID_ATTRIBUTE = "X-PROJECT-ID";
  static final String X_TENANT_ID_HEADER = "X-Tenant-Id";
  static final String X_ROLES_HEADER = "X-Roles";

  private final List<String> defaultAuthorizedRoles = new ArrayList<String>();
  private final List<String> agentAuthorizedRoles = new ArrayList<String>();
  private final List<String> readOnlyAuthorizedRoles = new ArrayList<String>();

  public PostAuthenticationFilter(List<String> defaultAuthorizedRoles,
                                  List<String> agentAuthorizedRoles,
                                  List<String> readOnlyAuthorizedRoles) {
    for (String defaultRole : defaultAuthorizedRoles) {
      this.defaultAuthorizedRoles.add(defaultRole.toLowerCase());
    }
    for (String agentRole : agentAuthorizedRoles) {
      this.agentAuthorizedRoles.add(agentRole.toLowerCase());
    }

    //
    // Check for null here so we can support backward compatibility
    // of not setting readOnlyAuthorizedRoles in the config file.
    //
    if (null != readOnlyAuthorizedRoles) {
      for (String readOnlyRole : readOnlyAuthorizedRoles) {
        this.readOnlyAuthorizedRoles.add(readOnlyRole.toLowerCase());
      }
    }
  }

  @Override
  public void destroy() {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    final HttpServletRequest req = (HttpServletRequest) request;
    ErrorCapturingServletResponseWrapper res = (ErrorCapturingServletResponseWrapper) response;
    String tenantIdStr = null;

    try {
      // According to CORS spec OPTIONS method does not pass auth info
      if (req.getMethod().equals("OPTIONS")) {
        chain.doFilter(request, response);
        return;
      }

      Object tenantId = request.getAttribute(X_TENANT_ID_ATTRIBUTE);

      if (tenantId == null) {
        sendAuthError(res, null, null, null);
        return;
      }
      tenantIdStr = tenantId.toString();

      boolean authenticated = isAuthenticated(req);
      boolean authorized = isAuthorized(req);

      if (authenticated && authorized) {
        HttpServletRequestWrapper wrapper = requestWrapperFor(req);
        chain.doFilter(wrapper, response);
        return;
      }

      if (authorized)
        sendAuthError(res, tenantIdStr, null, null);
      else
        sendAuthError(res, tenantIdStr, "Tenant is missing a required role to access this service",
            null);
    } catch (Exception e) {
      try {
        sendAuthError(res, tenantIdStr, null, e);
      } catch (IOException ignore) {
      }
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  /**
   * @return true if the request is authenticated else false
   */
  private boolean isAuthenticated(HttpServletRequest request) {
    Object identityStatus = request.getAttribute(X_IDENTITY_STATUS_ATTRIBUTE);
    return identityStatus != null && CONFIRMED_STATUS.equalsIgnoreCase(identityStatus.toString());
  }

  /**
   * @return true if the request is authorized else false
   */
  private boolean isAuthorized(HttpServletRequest request) {
    Object rolesFromKeystone = request.getAttribute(X_ROLES_ATTRIBUTE);
    if (rolesFromKeystone == null)
      return false;

    boolean agentUser = false;
    boolean readOnlyUser = false;

    for (String role : rolesFromKeystone.toString().split(",")) {
      String lowerCaseRole = role.toLowerCase();
      if ((defaultAuthorizedRoles != null) && defaultAuthorizedRoles.contains(lowerCaseRole)) {
        return true;
      }
      if ((agentAuthorizedRoles != null) && agentAuthorizedRoles.contains(lowerCaseRole)) {
        agentUser = true;
      }
      if ((readOnlyAuthorizedRoles != null) && readOnlyAuthorizedRoles.contains(lowerCaseRole)) {
        readOnlyUser = true;
      }
    }

    if (agentUser) {
      request.setAttribute(X_MONASCA_AGENT, true);
      return true;
    }

    if (readOnlyUser && request.getMethod().equals("GET")) {
      return true;
    }

    return false;
  }

  /**
   * Returns an HttpServletRequestWrapper that serves tenant id headers from request attributes.
   */
  private HttpServletRequestWrapper requestWrapperFor(final HttpServletRequest request) {
    return new HttpServletRequestWrapper(request) {
      @Override
      public String getHeader(String name) {
        if (name.equalsIgnoreCase(X_TENANT_ID_HEADER))
          return request.getAttribute(X_TENANT_ID_ATTRIBUTE).toString();
        else if (name.equalsIgnoreCase(X_ROLES_HEADER))
          return request.getAttribute(X_ROLES_ATTRIBUTE).toString();
        return super.getHeader(name);
      }

      @Override
      public Enumeration<String> getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        names.add(X_TENANT_ID_HEADER);
        names.add(X_ROLES_HEADER);
        return Collections.enumeration(names);
      }

      @Override
      public Enumeration<String> getHeaders(String name) {
        if (name.equalsIgnoreCase(X_TENANT_ID_HEADER))
          return Collections.enumeration(Collections.singleton(request.getAttribute(
              X_TENANT_ID_ATTRIBUTE).toString()));
        else if (name.equalsIgnoreCase(X_ROLES_HEADER))
          return Collections.enumeration(Collections.singleton(request.getAttribute(
              X_ROLES_ATTRIBUTE).toString()));
        return super.getHeaders(name);
      }
    };
  }

  private void sendAuthError(ErrorCapturingServletResponseWrapper response,
      @Nullable String tenantId, @Nullable String message, @Nullable Exception exception)
      throws IOException {
    response.setContentType(MediaType.APPLICATION_JSON);

    if (message == null)
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          tenantId == null ? "Failed to authenticate request"
              : "Failed to authenticate request for " + tenantId, exception);
    else
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, String.format(message, tenantId));
  }
}
