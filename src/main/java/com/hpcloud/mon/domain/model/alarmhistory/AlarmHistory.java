package com.hpcloud.mon.domain.model.alarmhistory;

import java.util.List;

import com.hpcloud.mon.domain.common.AbstractEntity;
import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.common.Linked;

public class AlarmHistory extends AbstractEntity implements Linked {
  private List<Link> links;
  private AlarmHistoryType type;

  public AlarmHistory() {
  }

  @Override
  public List<Link> getLinks() {
    return links;
  }

  @Override
  public void setLinks(List<Link> links) {
    this.links = links;
  }

  public AlarmHistoryType getType() {
    return type;
  }

  public void setType(AlarmHistoryType type) {
    this.type = type;
  }
}
