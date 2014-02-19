package com.hpcloud.mon.app.representation;

import com.hpcloud.mon.domain.model.version.Version;

/**
 * Version response.
 * 
 * @author Jonathan Halterman
 */
public class VersionRepresentation {
  public Version version;

  public VersionRepresentation() {
  }

  public VersionRepresentation(Version version) {
    this.version = version;
  }
}
