package com.hpcloud.mon.domain.model.alarmstatehistory;

import java.util.List;

import com.hpcloud.mon.domain.common.AbstractEntity;
import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.common.Linked;

public class AlarmStateHistory extends AbstractEntity implements Linked {
  private List<Link> links;


  public AlarmStateHistory() {
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
