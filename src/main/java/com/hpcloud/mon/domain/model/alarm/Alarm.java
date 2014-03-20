package com.hpcloud.mon.domain.model.alarm;

import java.util.List;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.common.AbstractEntity;
import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.common.Linked;

public class Alarm extends AbstractEntity implements Linked {
  private List<Link> links;
  private String name;
  private String description = "";
  private String expression;
  private AlarmState state;

  public Alarm() {
  }

  public Alarm(String id, String name, String description, String expression, AlarmState state) {
    this.id = id;
    this.name = name;
    this.setDescription(description);
    this.setExpression(expression);
    this.state = state;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    Alarm other = (Alarm) obj;
    if (expression == null) {
      if (other.expression != null)
        return false;
    } else if (!expression.equals(other.expression))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (state != other.state)
      return false;
    return true;
  }

  public String getDescription() {
    return description;
  }

  public String getExpression() {
    return expression;
  }

  public String getId() {
    return id;
  }

  public List<Link> getLinks() {
    return links;
  }

  public String getName() {
    return name;
  }

  public AlarmState getState() {
    return state;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((expression == null) ? 0 : expression.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    return result;
  }

  public void setDescription(String description) {
    this.description = description == null ? "" : description;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public void setLinks(List<Link> links) {
    this.links = links;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setState(AlarmState state) {
    this.state = state;
  }
}
