package com.hpcloud.mon.infrastructure.servlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Counts calls per tenant.
 * 
 * @author Jonathan Halterman
 */
public class TenantCallCountingFilter implements Filter {
  private static final Logger LOG = LoggerFactory.getLogger(TenantCallCountingFilter.class);

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
  
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }
}
