package com.hpcloud.mon.app.command;

import java.util.List;

import javax.annotation.Nullable;

import org.hibernate.validator.constraints.NotEmpty;

import com.hpcloud.mon.app.validation.AlarmValidation;

/**
 * @author Jonathan Halterman
 */
public class CreateAlarmCommand {
  @NotEmpty public String name;
  public String description;
  @NotEmpty public String expression;
  @NotEmpty public List<String> alarmActions;
  public List<String> okActions;
  public List<String> undeterminedActions;

  public CreateAlarmCommand() {
  }

  public CreateAlarmCommand(String name, @Nullable String description, String expression,
      List<String> alarmActions, List<String> okActions, List<String> undeterminedActions) {
    this.name = name;
    this.description = description;
    this.expression = expression;
    this.alarmActions = alarmActions;
    this.okActions = okActions;
    this.undeterminedActions = undeterminedActions;
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
    AlarmValidation.validate(name, description, alarmActions, okActions, undeterminedActions);
  }
}
