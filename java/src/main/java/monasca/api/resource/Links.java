/*
 * Copyright (c) 2014-2016 Hewlett Packard Enterprise Development Company, L.P.
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

import javax.ws.rs.core.UriInfo;

import com.google.common.base.Preconditions;

import monasca.api.ApiConfig;
import monasca.api.domain.model.alarm.AlarmCount;
import monasca.api.domain.model.common.Paged;
import monasca.api.domain.model.measurement.Measurements;
import monasca.api.domain.model.statistic.Statistics;
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
    ApiConfig config = Injector.getInstance(ApiConfig.class);
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


  /**
   * This method handles the case that the elements list size is one greater than the
   * limit. The next link will be created automatically.
   *
   * This method also handles the case that the element size is the limit. The next
   * link will not be created.
   *
   * The convention is for methods that query the DB to request limit + 1 elements.
   *
   * Only limit number of elements will be returned.
   *
   * @param limit
   * @param elements
   * @param uriInfo
   * @return
   */
  public static Object paginate(int limit, List<? extends AbstractEntity> elements, UriInfo uriInfo)
      throws UnsupportedEncodingException {

    // Check for paging turned off. Happens if maxQueryLimit is not set or is set to zero.
    if (limit == 0) {
      Paged paged = new Paged();
      paged.elements = elements != null ? elements : new ArrayList<>();
      return paged;
    }

    Paged paged = new Paged();

    paged.links.add(getSelfLink(uriInfo));

    if (elements != null) {

      if (elements.size() > limit) {

        String offset = elements.get(limit - 1).getId();

        paged.links.add(getNextLink(offset, uriInfo));

        // Truncate the list. Normally this will just truncate one extra element.
        elements = elements.subList(0, limit);
      }

      paged.elements = elements;

    } else {

      paged.elements = new ArrayList<>();

    }

    return paged;

  }

  public static Object paginateAlarming(int limit, List<? extends AbstractEntity> elements, UriInfo uriInfo)
      throws UnsupportedEncodingException {

    // Check for paging turned off. Happens if maxQueryLimit is not set or is set to zero.
    if (limit == 0) {
      Paged paged = new Paged();
      paged.elements = elements != null ? elements : new ArrayList<>();
      return paged;
    }

    Paged paged = new Paged();

    paged.links.add(getSelfLink(uriInfo));

    if (elements != null) {

      if (elements.size() > limit) {

        String offset = String.valueOf(limit);

        paged.links.add(getNextLink(offset, uriInfo));

        // Truncate the list. Normally this will just truncate one extra element.
        elements = elements.subList(0, limit);
      }

      paged.elements = elements;

    } else {

      paged.elements = new ArrayList<>();

    }

    return paged;

  }

  public static Object paginateMeasurements(int limit, List<Measurements> elements, UriInfo uriInfo)
      throws UnsupportedEncodingException {

    // Check for paging turned off. Happens if maxQueryLimit is not set or is set to zero.
    if (limit == 0) {
      Paged paged = new Paged();
      paged.elements = elements != null ? elements : new ArrayList<>();
      return paged;
    }

    Paged paged = new Paged();

    paged.links.add(getSelfLink(uriInfo));

    if (elements != null && !elements.isEmpty()) {

      Measurements m = elements.get(0);

      if (m != null) {

        List<Object[]> l = m.getMeasurements();

        if (l.size() > limit) {

          String offset = (String) l.get(limit - 1)[0];

          m.setId(offset);

          paged.links.add(getNextLink(offset, uriInfo));

          // Truncate the list. Normally this will just truncate one extra element.
          l = l.subList(0, limit);
          m.setMeasurements(l);

        }

        // Check if there are any elements.
        if (l.size() > 0) {
          // Set the id to the last date in the list.
          m.setId((String) l.get(l.size() - 1)[0]);
        }
        paged.elements = elements;

      } else {

        paged.elements = new ArrayList<>();

      }

    } else {

      paged.elements = new ArrayList<>();
    }

    return paged;

  }

  public static Object paginateStatistics(int limit, List<Statistics> elements, UriInfo uriInfo)
      throws UnsupportedEncodingException {

    // Check for paging turned off. Happens if maxQueryLimit is not set or is set to zero.
    if (limit == 0) {
      Paged paged = new Paged();
      paged.elements = elements != null ? elements : new ArrayList<>();
      return paged;
    }

    Paged paged = new Paged();

    paged.links.add(getSelfLink(uriInfo));

    if (elements != null && !elements.isEmpty()) {

      Statistics s = elements.get(0);

      if (s != null) {

        List<List<Object>> l = s.getStatistics();

        if (l.size() > limit) {

          String offset = (String) l.get(limit - 1).get(0);

          s.setId(offset);

          paged.links.add(getNextLink(offset, uriInfo));

          // Truncate the list. Normally this will just truncate one extra element.
          l = l.subList(0, limit);
          s.setStatistics(l);

        }

        // Check if there are any elements.
        if (l.size() > 0) {
          // Set the id to the last date in the list.
          s.setId((String) l.get(l.size() - 1).get(0));
        }
        paged.elements = elements;

      } else {

        paged.elements = new ArrayList<>();

      }

    } else {

      paged.elements = new ArrayList<>();
    }

    return paged;

  }

  private static Link getSelfLink(UriInfo uriInfo) {

    Link selfLink = new Link();
    selfLink.rel = "self";
    selfLink.href = uriInfo.getRequestUri().toString();
    return selfLink;
  }

  private static Link getNextLink(String offset, UriInfo uriInfo)
      throws UnsupportedEncodingException {

    Link nextLink = new Link();
    nextLink.rel = "next";

    // Create a new URL with the new offset.
    nextLink.href = uriInfo.getAbsolutePath().toString()
                    + "?offset=" + URLEncoder.encode(offset, "UTF-8");

    // Add the query parms back to the URL without the original offset.
    for (String parmKey : uriInfo.getQueryParameters().keySet()) {

      if (!parmKey.equalsIgnoreCase("offset")) {

        List<String> parmValList = uriInfo.getQueryParameters().get(parmKey);
        for (String parmVal : parmValList) {

          nextLink.href +=
              "&" + URLEncoder.encode(parmKey, "UTF-8") + "=" + URLEncoder.encode(parmVal, "UTF-8");

        }
      }
    }

    return nextLink;
  }

  public static void paginateAlarmCount(AlarmCount alarmCount, int limit, UriInfo uriInfo)
      throws UnsupportedEncodingException {
    List<Link> links = new ArrayList<>();
    links.add(getSelfLink(uriInfo));
    if (alarmCount.getCounts().size() > limit) {
      alarmCount.getCounts().remove(alarmCount.getCounts().size()-1);
      String offset = String.valueOf(limit);
      links.add(getNextLink(offset, uriInfo));
    }

    alarmCount.setLinks(links);
  }

}
