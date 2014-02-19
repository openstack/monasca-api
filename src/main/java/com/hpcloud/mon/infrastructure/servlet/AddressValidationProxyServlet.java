package com.hpcloud.mon.infrastructure.servlet;

import org.eclipse.jetty.http.HttpURI;

/**
 * @author Tim Potter
 */
public class AddressValidationProxyServlet extends org.eclipse.jetty.servlets.ProxyServlet {
  private String baseURL;

  public AddressValidationProxyServlet(final String baseURL) {
    this.baseURL = baseURL;
  }

  protected HttpURI proxyHttpURI(final String scheme, final String serverName, int serverPort,
      final String uri) {
    return new HttpURI(this.baseURL + uri);
  }
}
