/*
 * Copyright (c) 2016 Hewlett Packard Enterprise Development Company, L.P.
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
package monasca.api.domain.model.alarm;

import java.util.ArrayList;
import java.util.List;

import monasca.api.domain.model.common.Link;
import monasca.api.domain.model.common.Linked;
import monasca.common.model.domain.common.AbstractEntity;

public class AlarmCount extends AbstractEntity implements Linked {
  private List<Link> links;
  private List<String> columns;
  private List<List<Object>> counts;

  public AlarmCount() {}

  public AlarmCount(List<String> columns, List<List<Object>> counts) {
    this.columns = new ArrayList<>();
    this.columns.add("count");
    if (columns != null) {
      this.columns.addAll(columns);
    }
    this.counts = new ArrayList<>();
    this.counts.addAll(counts);
  }

  public void setColumns(List<String> columns) {
    this.columns = columns;
  }

  public List<String> getColumns() {
    return this.columns;
  }

  public void setCounts(List<List<Object>> counts) {
    this.counts = counts;
  }

  public List<List<Object>> getCounts() {
    return this.counts;
  }

  public void setLinks(List<Link> links) {
    this.links = links;
  }

  public List<Link> getLinks() {
    return this.links;
  }
}
