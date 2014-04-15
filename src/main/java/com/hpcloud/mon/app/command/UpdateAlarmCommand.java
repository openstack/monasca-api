package com.hpcloud.mon.app.command;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.hpcloud.mon.common.model.alarm.AlarmState;

/**
 * @author Jonathan Halterman
 */
public class UpdateAlarmCommand extends CreateAlarmCommand {
  @NotNull public AlarmState state;
  @NotNull public Boolean actionsEnabled;

  public UpdateAlarmCommand() {
  }

  public UpdateAlarmCommand(String name, @Nullable String description, String expression,
      AlarmState state, boolean enabled, List<String> alarmActions, List<String> okActions,
      List<String> undeterminedActions) {
    super(name, description, expression, alarmActions, okActions, undeterminedActions);
    this.state = state;
    this.actionsEnabled = enabled;
  }
}
