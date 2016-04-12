/*
 * (C) Copyright 2015-2016 Hewlett Packard Enterprise Development Company LP
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
package monasca.api.domain.model.common;

import java.util.ArrayList;
import java.util.List;

public class Paged {

  public static final int LIMIT = 10000;

  public List<Link> links = new ArrayList<>();

  public List<?> elements;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((elements == null) ? 0 : elements.hashCode());
    result = prime * result + ((links == null) ? 0 : links.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Paged other = (Paged) obj;
    if (elements == null) {
      if (other.elements != null)
        return false;
    } else if (!elements.equals(other.elements))
      return false;
    if (links == null) {
      if (other.links != null)
        return false;
    } else if (!links.equals(other.links))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Paged [links=" + links + ", elements=" + elements + "]";
  }
}
