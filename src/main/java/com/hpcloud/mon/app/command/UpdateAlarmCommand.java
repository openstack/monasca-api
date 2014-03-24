package com.hpcloud.mon.app.command;

import javax.validation.constraints.NotNull;

import com.hpcloud.mon.common.model.alarm.AlarmState;

/**
 * @author Jonathan Halterman
 */
public class UpdateAlarmCommand extends CreateAlarmCommand {
  @NotNull public AlarmState state;
  @NotNull public Boolean enabled;
}
