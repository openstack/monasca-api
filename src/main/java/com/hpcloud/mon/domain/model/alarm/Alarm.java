package com.hpcloud.mon.domain.model.alarm;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.common.AbstractEntity;
import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.common.Linked;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@ApiModel(value = "An alarm is a devops's best friend")
@XmlRootElement(name = "Alarm")
public class Alarm extends AbstractEntity implements Linked {
  private List<Link> links;
  private String name;
  private String description = "";
  private String expression;
  private AlarmState state;
  private boolean enabled;
  private List<String> alarmActions;
  private List<String> okActions;
  private List<String> undeterminedActions;

  public Alarm() {
  }

  public Alarm(String id, String name, String description, String expression, AlarmState state,
      boolean enabled, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions) {
    this.id = id;
    this.name = name;
    setDescription(description);
    setExpression(expression);
    setState(state);
    setEnabled(enabled);
    setAlarmActions(alarmActions);
    setOkActions(okActions);
    setUndeterminedActions(undeterminedActions);
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
    if (alarmActions == null) {
      if (other.alarmActions != null)
        return false;
    } else if (!alarmActions.equals(other.alarmActions))
      return false;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    if (enabled != other.enabled)
      return false;
    if (expression == null) {
      if (other.expression != null)
        return false;
    } else if (!expression.equals(other.expression))
      return false;
    if (links == null) {
      if (other.links != null)
        return false;
    } else if (!links.equals(other.links))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (okActions == null) {
      if (other.okActions != null)
        return false;
    } else if (!okActions.equals(other.okActions))
      return false;
    if (state != other.state)
      return false;
    if (undeterminedActions == null) {
      if (other.undeterminedActions != null)
        return false;
    } else if (!undeterminedActions.equals(other.undeterminedActions))
      return false;
    return true;
  }

  public List<String> getAlarmActions() {
    return alarmActions;
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

  public List<String> getOkActions() {
    return okActions;
  }

  public AlarmState getState() {
    return state;
  }

  public List<String> getUndeterminedActions() {
    return undeterminedActions;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((alarmActions == null) ? 0 : alarmActions.hashCode());
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + (enabled ? 1231 : 1237);
    result = prime * result + ((expression == null) ? 0 : expression.hashCode());
    result = prime * result + ((links == null) ? 0 : links.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((okActions == null) ? 0 : okActions.hashCode());
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    result = prime * result + ((undeterminedActions == null) ? 0 : undeterminedActions.hashCode());
    return result;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setAlarmActions(List<String> alarmActions) {
    this.alarmActions = alarmActions;
  }

  public void setDescription(String description) {
    this.description = description == null ? "" : description;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  @XmlElement(name = "id")
  @ApiModelProperty(value = "Alarm ID")
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

  public void setOkActions(List<String> okActions) {
    this.okActions = okActions;
  }

  public void setState(AlarmState state) {
    this.state = state;
  }

  public void setUndeterminedActions(List<String> undeterminedActions) {
    this.undeterminedActions = undeterminedActions;
  }

  @Override
  public String toString() {
    return String.format("Alarm [name=%s]", name);
  }
}
