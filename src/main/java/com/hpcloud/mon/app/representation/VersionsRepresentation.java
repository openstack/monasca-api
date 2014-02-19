package com.hpcloud.mon.app.representation;

import java.util.List;

import com.hpcloud.mon.domain.model.version.Version;

/**
 * Versions list response.
 * 
 * @author Jonathan Halterman
 */
public class VersionsRepresentation {
  public List<Version> versions;

  public VersionsRepresentation() {
  }

  public VersionsRepresentation(List<Version> versions) {
    this.versions = versions;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((versions == null) ? 0 : versions.hashCode());
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
    VersionsRepresentation other = (VersionsRepresentation) obj;
    if (versions == null) {
      if (other.versions != null)
        return false;
    } else if (!versions.equals(other.versions))
      return false;
    return true;
  }
}
