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
package monasca.api.app.command;

import java.util.List;

import javax.annotation.Nullable;

import org.hibernate.validator.constraints.NotEmpty;

import monasca.api.app.validation.AlarmValidation;

public class CreateAlarmDefinitionCommand {
  @NotEmpty
  public String name;
  public String description;
  @NotEmpty
  public String expression;
  public List<String> matchBy;
  public String severity;
  public List<String> alarmActions;
  public List<String> okActions;
  public List<String> undeterminedActions;

  public CreateAlarmDefinitionCommand() {
    this.severity = "LOW";
  }

  public CreateAlarmDefinitionCommand(String name, @Nullable String description, String expression,
      List<String> matchBy, String severity, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions) {
    this.name = name;
    this.description = description;
    this.expression = expression;
    this.matchBy = matchBy;
    this.alarmActions = alarmActions;
    this.okActions = okActions;
    this.undeterminedActions = undeterminedActions;
    this.severity = severity == null ? "LOW" : severity;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof CreateAlarmDefinitionCommand))
      return false;
    CreateAlarmDefinitionCommand other = (CreateAlarmDefinitionCommand) obj;
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((alarmActions == null) ? 0 : alarmActions.hashCode());
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((expression == null) ? 0 : expression.hashCode());
    result = prime * result + ((matchBy == null) ? 0 : matchBy.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((okActions == null) ? 0 : okActions.hashCode());
    result = prime * result + ((severity == null) ? 0 : severity.hashCode());
    result = prime * result + ((undeterminedActions == null) ? 0 : undeterminedActions.hashCode());
    return result;
  }

  public void validate() {
    AlarmValidation.validate(name, description, severity, alarmActions, okActions,
        undeterminedActions);
  }
}
