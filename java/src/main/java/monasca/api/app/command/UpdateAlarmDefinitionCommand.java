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
import javax.validation.constraints.NotNull;

public class UpdateAlarmDefinitionCommand extends CreateAlarmDefinitionCommand {
  @NotNull
  public Boolean actionsEnabled;

  public UpdateAlarmDefinitionCommand() {}

  public UpdateAlarmDefinitionCommand(String name, @Nullable String description, String expression,
      List<String> matchBy, String severity, boolean enabled, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions) {
    super(name, description, expression, matchBy, severity, alarmActions, okActions,
        undeterminedActions);
    this.actionsEnabled = enabled;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (!(obj instanceof UpdateAlarmDefinitionCommand))
      return false;
    UpdateAlarmDefinitionCommand other = (UpdateAlarmDefinitionCommand) obj;
    if (actionsEnabled == null) {
      if (other.actionsEnabled != null)
        return false;
    } else if (!actionsEnabled.equals(other.actionsEnabled))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((actionsEnabled == null) ? 0 : actionsEnabled.hashCode());
    return result;
  }
}
