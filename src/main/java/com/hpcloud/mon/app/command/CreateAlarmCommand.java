package com.hpcloud.mon.app.command;

import java.util.List;

import javax.annotation.Nullable;

import org.hibernate.validator.constraints.NotEmpty;

import com.hpcloud.mon.resource.exception.Exceptions;

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
    if (name.length() > 250)
      throw Exceptions.unprocessableEntity("Name %s must be 250 characters or less", name);
    if (description != null && description.length() > 250)
      throw Exceptions.unprocessableEntity("Description %s must be 250 characters or less",
          description);
    for (String action : alarmActions)
      if (action.length() > 50)
        throw Exceptions.unprocessableEntity("Alarm action %s must be 50 characters or less",
            action);
    if (okActions != null)
      for (String action : okActions)
        if (action.length() > 50)
          throw Exceptions.unprocessableEntity("Ok action %s must be 50 characters or less", action);
    if (undeterminedActions != null)
      for (String action : undeterminedActions)
        if (action.length() > 50)
          throw Exceptions.unprocessableEntity(
              "Undetermined action %s must be 50 characters or less", action);
  }
}
