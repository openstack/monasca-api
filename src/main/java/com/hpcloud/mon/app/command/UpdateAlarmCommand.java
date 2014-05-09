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
import javax.validation.constraints.NotNull;

import com.hpcloud.mon.common.model.alarm.AlarmState;

public class UpdateAlarmCommand extends CreateAlarmCommand {
  @NotNull public AlarmState state;
  @NotNull public Boolean actionsEnabled;

  public UpdateAlarmCommand() {
  }

  public UpdateAlarmCommand(String name, @Nullable String description, String severity,
      String expression, AlarmState state, boolean enabled, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    super(name, description, severity, expression, alarmActions, okActions, undeterminedActions);
    this.state = state;
    this.actionsEnabled = enabled;
  }
}
