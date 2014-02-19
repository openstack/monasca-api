package com.hpcloud.mon.app.command;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * Request for a new alarm.
 * 
 * @author Jonathan Halterman
 */
public class CreateAlarmCommand {
  @Valid @NotNull public CreateAlarmInner alarm;

  /** Actual resource values are wrapped since that's how OpenStack does it. */
  public static class CreateAlarmInner {
    @NotEmpty public String name;
    @NotEmpty public String expression;
    @NotEmpty public List<String> alarmActions;

    public CreateAlarmInner() {
    }

    public CreateAlarmInner(String name, String expression, List<String> alarmActions) {
      this.name = name;
      this.expression = expression;
      this.alarmActions = alarmActions;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      CreateAlarmInner other = (CreateAlarmInner) obj;
      if (alarmActions == null) {
        if (other.alarmActions != null)
          return false;
      } else if (!alarmActions.equals(other.alarmActions))
        return false;
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
      return true;
    }

    public void validate() {
      if (name.length() > 250)
        throw Exceptions.unprocessableEntity("Name %s must be 250 characters or less", name);
      for (String action : alarmActions)
        if (action.length() > 50)
          throw Exceptions.unprocessableEntity("Alarm action %s must be 50 characters or less",
              action);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((alarmActions == null) ? 0 : alarmActions.hashCode());
      result = prime * result + ((expression == null) ? 0 : expression.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }
  }

  public CreateAlarmCommand() {
  }

  public CreateAlarmCommand(String name, String expression, List<String> alarmActions) {
    alarm = new CreateAlarmInner(name, expression, alarmActions);
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
    if (alarm == null) {
      if (other.alarm != null)
        return false;
    } else if (!alarm.equals(other.alarm))
      return false;
    return true;
  }
}
