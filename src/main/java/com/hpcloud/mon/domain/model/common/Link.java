package com.hpcloud.mon.domain.model.common;

public class Link {
  public String rel;
  public String href;

  public Link() {
  }

  public Link(String rel, String href) {
    this.rel = rel;
    this.href = href;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((href == null) ? 0 : href.hashCode());
    result = prime * result + ((rel == null) ? 0 : rel.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Link other = (Link) obj;
    if (href == null) {
      if (other.href != null)
        return false;
    } else if (!href.equals(other.href))
      return false;
    if (rel == null) {
      if (other.rel != null)
        return false;
    } else if (!rel.equals(other.rel))
      return false;
    return true;
  }
}
