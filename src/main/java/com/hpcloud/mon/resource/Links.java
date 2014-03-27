package com.hpcloud.mon.resource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriInfo;

import com.google.common.base.Preconditions;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.domain.common.AbstractEntity;
import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.common.Linked;
import com.hpcloud.util.Injector;

/**
 * Utilities for working with links.
 * 
 * @author Jonathan Halterman
 */
public final class Links {
  static boolean accessedViaHttps;

  static {
    MonApiConfiguration config = Injector.getInstance(MonApiConfiguration.class);
    if (config != null && config.accessedViaHttps != null)
      accessedViaHttps = config.accessedViaHttps;
  }

  /**
   * Hydrates the {@code resources} with links for the {@code uriInfo}.
   * 
   * @throws NullPointerException if {@code resource} is null
   */
  public static <T extends AbstractEntity & Linked> List<T> hydrate(List<T> resources,
      UriInfo uriInfo, String... children) {
    Preconditions.checkNotNull(resources, "resources");

    // Safe since this path should not be specific to a resource
    String absolutePath = prefixForHttps(uriInfo.getAbsolutePath().toString());
    for (T resource : resources)
      hydrate(resource, absolutePath, children);
    return resources;
  }

  /**
   * Hydrates the {@code resource} with links for the {@code uriInfo}.
   * 
   * @throws NullPointerException if {@code resource} is null
   */
  public static <T extends AbstractEntity & Linked> T hydrate(T resource, UriInfo uriInfo,
      String... children) {
    return hydrate(resource, prefixForHttps(uriInfo.getAbsolutePath().toString()), children);
  }

  /**
   * Returns a string that is prefixed for prefixForHttp is https is being used.
   */
  static String prefixForHttps(String path) {
    if (accessedViaHttps && !path.toLowerCase().startsWith("https"))
      path = "https" + path.substring(path.indexOf("://"));
    return path;
  }

  /**
   * Hydrates the {@code resource} with links for the {@code path}.
   * 
   * @throws NullPointerException if {@code resource} is null
   */
  private static <T extends AbstractEntity & Linked> T hydrate(T resource, String path,
      String... children) {
    Preconditions.checkNotNull(resource, "resource");

    List<Link> links = new ArrayList<>(children.length + 1);
    if (!path.endsWith(resource.getId())) {
      boolean pathEndsInSlash = path.length() > 0 && path.charAt(path.length() - 1) == '/';
      if (!pathEndsInSlash)
        path += "/";
      path += resource.getId();
    }

    links.add(new Link("self", path));
    for (String child : children)
      links.add(new Link(child, path + "/" + child));

    resource.setLinks(links);
    return resource;
  }
}
