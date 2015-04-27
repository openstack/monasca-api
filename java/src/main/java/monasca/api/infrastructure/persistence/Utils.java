/*
 * Copyright 2015 FUJITSU LIMITED
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
 *
 */

package monasca.api.infrastructure.persistence;

import java.util.List;
import java.util.Map;


abstract public class Utils {

  public abstract List<String> findAlarmIds(String tenantId,
                                            Map<String, String> dimensions);

  protected String buildJoinClauseFor(Map<String, String> dimensions) {

    if ((dimensions == null) || dimensions.isEmpty()) {
      return "";
    }

    final StringBuilder sb =
        new StringBuilder(
            "join alarm_metric as am on a.id=am.alarm_id "
                + "join metric_definition_dimensions as mdd on am.metric_definition_dimensions_id=mdd.id ");

    for (int i = 0; i < dimensions.size(); i++) {

      final String tableAlias = "md" + i;

      sb.append(" inner join metric_dimension ")
          .append(tableAlias)
          .append(" on ")
          .append(tableAlias)
          .append(".name = :dname")
          .append(i)
          .append(" and ")
          .append(tableAlias)
          .append(".value = :dvalue")
          .append(i)
          .append(" and mdd.metric_dimension_set_id = ")
          .append(tableAlias)
          .append(".dimension_set_id");
    }

    return sb.toString();
  }

}
