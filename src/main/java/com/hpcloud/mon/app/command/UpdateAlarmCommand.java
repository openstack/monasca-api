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
  @NotNull public Boolean enabled;

  public UpdateAlarmCommand() {
  }

  public UpdateAlarmCommand(String name, @Nullable String description, String expression,
      AlarmState state, boolean enabled, List<String> alarmActions) {
    super(name, description, expression, alarmActions);
    this.state = state;
    this.enabled = enabled;
  }
}
