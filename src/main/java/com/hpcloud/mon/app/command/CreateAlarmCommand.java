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
package com.hpcloud.mon.app.command;

import java.util.List;

import javax.annotation.Nullable;

import org.hibernate.validator.constraints.NotEmpty;

import com.hpcloud.mon.app.validation.AlarmValidation;

public class CreateAlarmCommand {
  @NotEmpty public String name;
  public String description;
  @NotEmpty public String expression;
  public String severity;
  public List<String> alarmActions;
  public List<String> okActions;
  public List<String> undeterminedActions;


  public CreateAlarmCommand() {
    this.severity = "LOW";
  }

  public CreateAlarmCommand(String name, @Nullable String description, String expression, String severity,
      List<String> alarmActions, List<String> okActions, List<String> undeterminedActions) {
    this.name = name;
    this.description = description;
    this.expression = expression;
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
    if (getClass() != obj.getClass())
      return false;
    CreateAlarmCommand other = (CreateAlarmCommand) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  public void validate() {
    AlarmValidation.validate(name, description, severity, alarmActions, okActions, undeterminedActions);
  }
}
