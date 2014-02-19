package com.hpcloud.mon.domain.model.version;

import java.util.Date;
import java.util.List;

import com.hpcloud.mon.domain.common.AbstractEntity;
import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.common.Linked;

public class Version extends AbstractEntity implements Linked {
  private List<Link> links;
  public VersionStatus status;
  // TODO use a date that serializes better
  public Date updated;

  public enum VersionStatus {
    CURRENT, DEPRECATED, OBSOLETE;
  }

  public Version() {
  }

  public Version(String id, VersionStatus status, Date updated) {
    this.id = id;
    this.status = status;
    this.updated = updated;
  }

  public String getId() {
    return id;
  }

  @Override
  public List<Link> getLinks() {
    return links;
  }

  @Override
  public void setLinks(List<Link> links) {
    this.links = links;
  }
}
