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
 * Validates tenant ids.
 * 
 * @author Jonathan Halterman
 */
public class TenantValidationFilter implements Filter {
  private static final Logger LOG = LoggerFactory.getLogger(TenantValidationFilter.class);

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
