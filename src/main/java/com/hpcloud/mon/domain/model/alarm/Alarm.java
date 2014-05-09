/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpcloud.mon.domain.model.alarm;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.common.AbstractEntity;
import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.domain.model.common.Linked;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "An alarm is a devops's best friend")
@XmlRootElement(name = "Alarm")
public class Alarm extends AbstractEntity implements Linked {
  private List<Link> links;
  private String name;
  private String description = "";
  private String expression;
  private Object expressionData;
  private AlarmState state;
  private String severity;
  private boolean actionsEnabled;
  private List<String> alarmActions;
  private List<String> okActions;
  private List<String> undeterminedActions;

  public Alarm() {
  }

  public Alarm(String id, String name, String description, String severity, String expression,
      AlarmState state, boolean actionsEnabled, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions) {
    this.id = id;
    this.name = name;
    setDescription(description);
    setSeverity(severity);
    setExpression(expression);
    setState(state);
    setActionsEnabled(actionsEnabled);
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
    if (severity == null) {
      if (other.severity != null)
        return false;
    } else if (!severity.equals(other.severity))
      return false;
    if (actionsEnabled != other.actionsEnabled)
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

  public String getSeverity() { return severity; }

  public String getExpression() {
    return expression;
  }

  public Object getExpressionData() {
    return expressionData;
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
    result = prime * result + ((severity == null) ? 0 : severity.hashCode());
    result = prime * result + (actionsEnabled ? 1231 : 1237);
    result = prime * result + ((expression == null) ? 0 : expression.hashCode());
    result = prime * result + ((links == null) ? 0 : links.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((okActions == null) ? 0 : okActions.hashCode());
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    result = prime * result + ((undeterminedActions == null) ? 0 : undeterminedActions.hashCode());
    return result;
  }

  public boolean isActionsEnabled() {
    return actionsEnabled;
  }

  public void setActionsEnabled(boolean actionsEnabled) {
    this.actionsEnabled = actionsEnabled;
  }

  public void setAlarmActions(List<String> alarmActions) {
    this.alarmActions = alarmActions;
  }

  public void setDescription(String description) {
    this.description = description == null ? "" : description;
  }

  public void setExpression(String expression) {
    this.expression = expression;
    setExpressionData(AlarmExpression.of(expression).getExpressionTree());
  }

  public void setExpressionData(Object expressionData) {
    this.expressionData = expressionData;
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

  public void setSeverity(String severity) { this.severity = severity;}

  public void setUndeterminedActions(List<String> undeterminedActions) {
    this.undeterminedActions = undeterminedActions;
  }

  @Override
  public String toString() {
    return String.format("Alarm [name=%s]", name);
  }
}
