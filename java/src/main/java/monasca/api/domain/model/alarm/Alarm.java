/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P.
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

import org.joda.time.DateTime;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import monasca.common.model.alarm.AlarmState;
import monasca.common.model.metric.MetricDefinition;
import monasca.common.model.domain.common.AbstractEntity;
import monasca.api.domain.model.common.Link;
import monasca.api.domain.model.common.Linked;

@XmlRootElement(name = "Alarm")
public class Alarm extends AbstractEntity implements Linked {
  private List<Link> links;
  private List<MetricDefinition> metrics;
  private AlarmState state;
  private String lifecycleState;
  private String link;
  private AlarmDefinitionShort alarmDefinition;
  private DateTime stateUpdatedTimestamp;
  private DateTime updatedTimestamp;
  private DateTime createdTimestamp;

  public Alarm() {}

  public Alarm(String id, String alarmDefinitionId, String alarmDefinitionName,
      String alarmDefinitionSeverity, List<MetricDefinition> metrics, AlarmState state,
      String lifecycleState, String link, DateTime stateUpdatedTimestamp,
      DateTime updatedTimestamp, DateTime createdTimestamp) {
    this.id = id;
    setMetrics(metrics);
    setState(state);
    setLifecycleState(lifecycleState);
    setLink(link);
    setStateUpdatedTimestamp(stateUpdatedTimestamp);
    setUpdatedTimestamp(updatedTimestamp);
    setCreatedTimestamp(createdTimestamp);
    this.alarmDefinition = new AlarmDefinitionShort(alarmDefinitionId, alarmDefinitionName, alarmDefinitionSeverity);
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

  public String getLifecycleState() {
    return lifecycleState;
  }

  public String getLink() {
    return link;
  }

  public DateTime getStateUpdatedTimestamp() {
    return stateUpdatedTimestamp;
  }

  public DateTime getUpdatedTimestamp() {
    return updatedTimestamp;
  }

  public DateTime getCreatedTimestamp() {
    return createdTimestamp;
  }

  @XmlElement(name = "id")
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

  public void setLifecycleState(String lifecycleState) {
    this.lifecycleState = lifecycleState;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public void setStateUpdatedTimestamp(DateTime stateUpdatedTimestamp) {
    this.stateUpdatedTimestamp = stateUpdatedTimestamp;
  }

  public void setUpdatedTimestamp(DateTime updatedTimestamp) {
    this.updatedTimestamp = updatedTimestamp;
  }

  public void setCreatedTimestamp(DateTime createdTimestamp) {
    this.createdTimestamp = createdTimestamp;
  }

  public List<MetricDefinition> getMetrics() {
    return metrics;
  }

  public void setMetrics(List<MetricDefinition> metrics) {
    this.metrics = metrics;
  }

  public AlarmDefinitionShort getAlarmDefinition() {
    return alarmDefinition;
  }

  public void setAlarmDefinition(AlarmDefinitionShort alarmDefinition) {
    this.alarmDefinition = alarmDefinition;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((alarmDefinition == null) ? 0 : alarmDefinition.hashCode());
    result = prime * result + ((links == null) ? 0 : links.hashCode());
    result = prime * result + ((metrics == null) ? 0 : metrics.hashCode());
    result = prime * result + ((state == null) ? 0 : state.hashCode());
    result = prime * result + ((lifecycleState == null) ? 0 : lifecycleState.hashCode());
    result = prime * result + ((link == null) ? 0 : link.hashCode());
    result = prime * result + ((stateUpdatedTimestamp == null) ? 0 : stateUpdatedTimestamp.hashCode());
    result = prime * result + ((updatedTimestamp == null) ? 0 : updatedTimestamp.hashCode());
    result = prime * result + ((createdTimestamp == null) ? 0 : createdTimestamp.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    Alarm other = (Alarm) obj;
    if (alarmDefinition == null) {
      if (other.alarmDefinition != null)
        return false;
    } else if (!alarmDefinition.equals(other.alarmDefinition))
      return false;
    if (links == null) {
      if (other.links != null)
        return false;
    } else if (!links.equals(other.links))
      return false;
    if (metrics == null) {
      if (other.metrics != null)
        return false;
    } else if (!CollectionUtils.isEqualCollection(metrics, other.metrics))
      // order agnostic collection equality check
      return false;
    if (state != other.state)
      return false;
    if (lifecycleState == null) {
      if (other.lifecycleState != null)
        return false;
    } else if (!lifecycleState.equals(other.lifecycleState))
      return false;
    if (link == null) {
      if (other.link != null)
        return false;
    } else if (!link.equals(other.link))
      return false;
    // Ignore timezones, only check milliseconds since epoch
    if (stateUpdatedTimestamp != other.stateUpdatedTimestamp) {
      if (stateUpdatedTimestamp == null || other.stateUpdatedTimestamp == null) {
        return false;
      } else if (stateUpdatedTimestamp.getMillis() != other.stateUpdatedTimestamp.getMillis()) {
        return false;
      }
    }
    if (updatedTimestamp != other.updatedTimestamp) {
      if (updatedTimestamp == null || other.updatedTimestamp == null) {
        return false;
      } else if (updatedTimestamp.getMillis() != other.updatedTimestamp.getMillis()) {
        return false;
      }
    }
    if (createdTimestamp != other.createdTimestamp) {
      if (createdTimestamp == null || other.createdTimestamp == null) {
        return false;
      } else if (createdTimestamp.getMillis() != other.createdTimestamp.getMillis()) {
        return false;
      }
    }
    return true;
  }

  /**
   * This class holds the parts of AlarmDefinition that are returned through the API with
   * an Alarm
   * @author craigbr
   *
   */
  public static class AlarmDefinitionShort extends AbstractEntity implements Linked {
    private String name;
    private String severity;
    private List<Link> links;

    public AlarmDefinitionShort() {
    }

    public AlarmDefinitionShort(String id, String name, String severity) {
      this.id = id;
      this.name = name;
      this.severity = severity;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getSeverity() {
      return severity;
    }

    public void setSeverity(String severity) {
      this.severity = severity;
    }

    @Override
    public List<Link> getLinks() {
      return links;
    }

    @Override
    public void setLinks(List<Link> links) {
      this.links = links;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((links == null) ? 0 : links.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((severity == null) ? 0 : severity.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      AlarmDefinitionShort other = (AlarmDefinitionShort) obj;
      if (links == null) {
        if (other.links != null)
          return false;
      } else if (!links.equals(other.links))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      if (severity == null) {
        if (other.severity != null)
          return false;
      } else if (!severity.equals(other.severity))
        return false;
      return true;
    }
  }
}
