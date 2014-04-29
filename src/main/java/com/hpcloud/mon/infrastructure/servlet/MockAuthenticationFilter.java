package com.hpcloud.mon.infrastructure.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Mocks authentication by converting X-Auth-Token headers to X-Tenant-Ids.
 * 
 * @author Jonathan Halterman
 */
public class MockAuthenticationFilter implements Filter {
  private static final String X_AUTH_TOKEN_HEADER = "X-Auth-Token";
  private static final String X_TENANT_ID_HEADER = "X-Tenant-Id";

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    final HttpServletRequest req = (HttpServletRequest) request;
    HttpServletRequestWrapper wrapper = requestWrapperFor(req);
    chain.doFilter(wrapper, response);
  }

  /**
   * Returns an HttpServletRequestWrapper that serves tenant id headers from request attributes.
   */
  private HttpServletRequestWrapper requestWrapperFor(final HttpServletRequest request) {
    return new HttpServletRequestWrapper(request) {
      @Override
      public String getHeader(String name) {
        if (name.equalsIgnoreCase(X_TENANT_ID_HEADER))
          return request.getHeader(X_AUTH_TOKEN_HEADER).toString();
        return super.getHeader(name);
      }

      @Override
      public Enumeration<String> getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        names.add(X_TENANT_ID_HEADER);
        return Collections.enumeration(names);
      }

      @Override
      public Enumeration<String> getHeaders(String name) {
        if (name.equalsIgnoreCase(X_TENANT_ID_HEADER))
          return Collections.enumeration(Collections.singleton(request.getHeader(
              X_AUTH_TOKEN_HEADER).toString()));
        return super.getHeaders(name);
      }
    };
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }
}
