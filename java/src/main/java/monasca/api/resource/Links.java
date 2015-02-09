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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import com.google.common.base.Preconditions;

import monasca.api.MonApiConfiguration;
import monasca.api.domain.model.common.Paged;
import monasca.api.domain.model.measurement.Measurements;
import monasca.common.model.domain.common.AbstractEntity;
import monasca.api.domain.model.common.Link;
import monasca.api.domain.model.common.Linked;
import monasca.common.util.Injector;

/**
 * Utilities for working with links.
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
      hydrate(resource, absolutePath, false, children);
    return resources;
  }

  /**
   * Hydrates the {@code resource} with links for the {@code uriInfo}.
   * 
   * @param resource to obtain id from
   * @param uriInfo to obtain path from
   * @throws NullPointerException if {@code resource} is null
   */
  public static <T extends AbstractEntity & Linked> T hydrate(T resource, UriInfo uriInfo) {
    return hydrate(resource, prefixForHttps(uriInfo.getAbsolutePath().toString()), false);
  }

  /**
   * Hydrates the {@code resource} with links for the {@code uriInfo}.
   * 
   * @param resource to obtain id from
   * @param uriInfo to obtain base path from
   * @param resourcePath path to type of resource
   * @throws NullPointerException if {@code resource} is null
   */
  public static <T extends AbstractEntity & Linked> T hydrate(T resource, UriInfo uriInfo, String resourcePath) {
    return hydrate(resource, concatPaths(uriInfo.getBaseUri().toString(), resourcePath) + "/", false);
  }

  private static String concatPaths(final String first, final String second) {
    // Check if this would cause two slashes in a row or a slash at the start
    if ((first.isEmpty() || first.endsWith("/")) && !second.isEmpty() && second.startsWith("/")) {
      return first + second.substring(1);
    }
    else {
      return first + second;
    }
  }

  /**
   * Hydrates the {@code resource} with links for the {@code uriInfo}.
   * 
   * @param resource to obtain id from
   * @param uriInfo to obtain path from
   * @param uriInfoForSpecificResource whether the uriInfo is for a specific resource
   * @param children child link elements to create
   * @throws NullPointerException if {@code resource} is null
   */
  public static <T extends AbstractEntity & Linked> T hydrate(T resource, UriInfo uriInfo,
      boolean uriInfoForSpecificResource, String... children) {
    return hydrate(resource, prefixForHttps(uriInfo.getAbsolutePath().toString()),
        uriInfoForSpecificResource, children);
  }

  /**
   * Returns a string that is prefixed for prefixForHttp if https is being used.
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
      boolean pathForSpecificResource, String... children) {
    Preconditions.checkNotNull(resource, "resource");

    List<Link> links = new ArrayList<>(children.length + 1);
    if (!pathForSpecificResource) {
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


  public static Object paginate(String offset, List<? extends AbstractEntity> elements,
                                UriInfo uriInfo) {

    if (offset != null) {

      Paged paged = new Paged();
      Link selfLink = new Link();
      selfLink.rel = "self";
      selfLink.href = uriInfo.getRequestUri().toString();
      paged.links.add(selfLink);

      if (elements != null) {
        if (elements.size() >= Paged.LIMIT) {
          Link nextLink = new Link();
          nextLink.rel = "next";
          // Create a new URL with the new offset.
          nextLink.href =
              uriInfo.getAbsolutePath().toString() + "?offset=" + elements.get(elements.size() - 1)
                  .getId();
          // Add the query parms back to the URL without the original offset.
          for (String parmKey : uriInfo.getQueryParameters().keySet()) {
            if (!parmKey.equalsIgnoreCase("offset")) {
              List<String> parmValList = uriInfo.getQueryParameters().get(parmKey);
              for (String parmVal : parmValList) {
                nextLink.href += "&" + parmKey + "=" + parmVal;
              }
            }
          }
          paged.links.add(nextLink);
        }
      }

      paged.elements = elements != null ? elements : new ArrayList();

      return paged;

    } else {

      return elements;
    }
  }

  public static Object paginateMeasurements(String offset, Measurements measurements,
                                            UriInfo uriInfo) throws UnsupportedEncodingException {

    if (offset != null) {

      Paged paged = new Paged();
      Link selfLink = new Link();
      selfLink.rel = "self";
      selfLink.href = uriInfo.getRequestUri().toString();
      paged.links.add(selfLink);

      if (measurements.getMeasurements().size() >= Paged.LIMIT) {
        Link nextLink = new Link();
        nextLink.rel = "next";
        // Create a new URL with the new offset.
        nextLink.href = uriInfo.getAbsolutePath().toString() + "?offset=" + measurements.getId();

        // Add the query parms back to the URL without the original offset and dimensions.
        for (String parmKey : uriInfo.getQueryParameters().keySet()) {
          if (!parmKey.equalsIgnoreCase("offset") && !parmKey.equalsIgnoreCase("dimensions")) {
            List<String> parmValList = uriInfo.getQueryParameters().get(parmKey);
            for (String parmVal : parmValList) {
              nextLink.href += "&" + parmKey + "=" + parmVal;
            }
          }
        }

        // Add the dimensions for this particular measurement.
        Map<String, String> dimensionsMap = measurements.getDimensions();
        if (dimensionsMap != null && !dimensionsMap.isEmpty()) {
          nextLink.href += "&dimensions=";
          boolean firstDimension = true;
          for (String dimensionKey : dimensionsMap.keySet()) {
            String dimensionVal = dimensionsMap.get(dimensionKey);
            if (firstDimension) {
              firstDimension = false;
            } else {
              nextLink.href += URLEncoder.encode(",", "UTF-8");
            }
            nextLink.href += dimensionKey + URLEncoder.encode(":", "UTF-8") + dimensionVal;
          }
        }

        paged.links.add(nextLink);
      }

      List<Measurements> measurementsList = new ArrayList();
      measurementsList.add(measurements);
      paged.elements = measurementsList;

      return paged;

    } else {

      return measurements;
    }
  }

}
