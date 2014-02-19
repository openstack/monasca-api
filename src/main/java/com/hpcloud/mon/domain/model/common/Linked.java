package com.hpcloud.mon.domain.model.common;

import java.util.List;

/**
 * Defines a type that can be described via a set of links.
 * 
 * @author Jonathan Halterman
 */
public interface Linked {
  List<Link> getLinks();

  void setLinks(List<Link> links);
}
