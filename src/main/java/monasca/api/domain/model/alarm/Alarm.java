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
package monasca.api.domain.model.alarm;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.common.AbstractEntity;
import monasca.api.domain.model.common.Link;
import monasca.api.domain.model.common.Linked;
import com.wordnik.swagger.annotations.ApiModelProperty;

@XmlRootElement(name = "Alarm")
public class Alarm extends AbstractEntity implements Linked {
  private List<Link> links;
  private String alarmDefinitionId;
  private List<MetricDefinition> metrics;
  private AlarmState state;

  public Alarm() {}

  public Alarm(String id, String alarmDefinitionId, String metricName,
      List<MetricDefinition> metrics, AlarmState state) {
    this.id = id;
    setMetrics(metrics);
    setState(state);
  }

  public String getAlarmDefinitionId() {
    return alarmDefinitionId;
  }

  public String getId() {
    return id;
  }

  public List<Link> getLinks() {
    return links;
  }

  public AlarmState getState() {
    return state;
  }

  public void setAlarmDefinitionId(String alarmDefinitionId) {
    this.alarmDefinitionId = alarmDefinitionId;
  }

  @XmlElement(name = "id")
  @ApiModelProperty(value = "Alarm ID")
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public void setLinks(List<Link> links) {
    this.links = links;
  }

  public void setState(AlarmState state) {
    this.state = state;
  }

  public List<MetricDefinition> getMetrics() {
    return metrics;
  }

  public void setMetrics(List<MetricDefinition> metrics) {
    this.metrics = metrics;
  }
}
