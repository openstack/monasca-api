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
package monasca.api.domain.model.version;

import java.util.List;

import org.joda.time.DateTime;

import monasca.common.model.domain.common.AbstractEntity;
import monasca.api.domain.model.common.Link;
import monasca.api.domain.model.common.Linked;

public class Version extends AbstractEntity implements Linked {
  private List<Link> links;
  public VersionStatus status;
  public DateTime updated;

  public enum VersionStatus {
    CURRENT, DEPRECATED, OBSOLETE;
  }

  public Version() {}

  public Version(String id, VersionStatus status, DateTime updated) {
    this.id = id;
    this.status = status;
    this.updated = updated;
  }

  public String getId() {
    return id;
  }

  @Override
  public List<Link> getLinks() {
    return links;
  }

  @Override
  public void setLinks(List<Link> links) {
    this.links = links;
  }
}
