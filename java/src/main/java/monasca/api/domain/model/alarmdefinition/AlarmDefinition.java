/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package monasca.api.domain.model.alarmdefinition;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import monasca.api.domain.model.common.Link;
import monasca.api.domain.model.common.Linked;
import monasca.common.model.alarm.AlarmExpression;
import monasca.common.model.domain.common.AbstractEntity;

@XmlRootElement(name = "Alarm definition")
public class AlarmDefinition extends AbstractEntity implements Linked {
  private List<Link> links;
  private String name;
  private String description = "";
  private String expression;
  private boolean deterministic;
  private Object expressionData;
  private List<String> matchBy;
  private String severity;
  private boolean actionsEnabled;
  private List<String> alarmActions;
  private List<String> okActions;
  private List<String> undeterminedActions;

  public AlarmDefinition() {}

  public AlarmDefinition(String id, String name, String description, String severity,
      String expression, List<String> matchBy, boolean actionsEnabled, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    this.id = id;
    this.name = name;
    setDescription(description);
    setSeverity(severity);
    setExpression(expression);
    setMatchBy(matchBy);
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
    if (!(obj instanceof AlarmDefinition))
      return false;
    AlarmDefinition other = (AlarmDefinition) obj;
    if (actionsEnabled != other.actionsEnabled)
      return false;
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
    if (expression == null) {
      if (other.expression != null)
        return false;
    } else if (!expression.equals(other.expression))
      return false;
    if (expressionData == null) {
      if (other.expressionData != null)
        return false;
    } else if (!expressionData.equals(other.expressionData))
      return false;
    if (this.deterministic != other.deterministic) {
      return false;
    }
    if (links == null) {
      if (other.links != null)
        return false;
    } else if (!links.equals(other.links))
      return false;
    if (matchBy == null) {
      if (other.matchBy != null)
        return false;
    } else if (!matchBy.equals(other.matchBy))
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
    if (severity == null) {
      if (other.severity != null)
        return false;
    } else if (!severity.equals(other.severity))
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

  public Object getExpressionData() {
    return expressionData;
  }

  public String getId() {
    return id;
  }

  public List<Link> getLinks() {
    return links;
  }

  public List<String> getMatchBy() {
    return matchBy;
  }

  public String getName() {
    return name;
  }

  public List<String> getOkActions() {
    return okActions;
  }

  public String getSeverity() {
    return severity;
  }

  public List<String> getUndeterminedActions() {
    return undeterminedActions;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (actionsEnabled ? 1231 : 1237);
    result = prime * result + ((alarmActions == null) ? 0 : alarmActions.hashCode());
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((expression == null) ? 0 : expression.hashCode());
    result = prime * result + ((expressionData == null) ? 0 : expressionData.hashCode());
    result = prime * result + Boolean.valueOf(this.deterministic).hashCode();
    result = prime * result + ((links == null) ? 0 : links.hashCode());
    result = prime * result + ((matchBy == null) ? 0 : matchBy.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((okActions == null) ? 0 : okActions.hashCode());
    result = prime * result + ((severity == null) ? 0 : severity.hashCode());
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

    final AlarmExpression alarmExpression = AlarmExpression.of(expression);
    this.setExpressionData(alarmExpression.getExpressionTree());
    this.deterministic = alarmExpression.isDeterministic();
  }

  @JsonIgnore
  public void setExpressionData(Object expressionData) {
    this.expressionData = expressionData;
  }

  @XmlElement(name = "id")
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public void setLinks(List<Link> links) {
    this.links = links;
  }

  public void setMatchBy(List<String> matchBy) {
    this.matchBy = matchBy == null ? Collections.<String>emptyList() : matchBy;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setOkActions(List<String> okActions) {
    this.okActions = okActions;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public void setUndeterminedActions(List<String> undeterminedActions) {
    this.undeterminedActions = undeterminedActions;
  }

  public boolean isDeterministic() {
    return this.deterministic;
  }

  @Override
  public String toString() {
    return String.format("AlarmDefinition [name=%s]", name);
  }
}
