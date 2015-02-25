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
package monasca.api.infrastructure.persistence.influxdb;

import java.util.HashMap;
import java.util.Map;

public class Series {

  public SeriesElement[] results;
  public String error;

  boolean isEmpty() {

    return this.results[0].series == null;
  }

  int getSeriesLength() {

    if (!isEmpty()) {
      return this.results[0].series.length;
    } else {
      return 0;
    }
  }

  Serie[] getSeries() {

    return this.results[0].series;
  }

  public String getError() {

    return this.error;

  }
}

class SeriesElement {

  public Serie[] series;
  public String error;

}

class Serie {

  // Initialize to defaults to avoid NPE.
  public String name = "";
  Map<String, String> tags = new HashMap();
  public String[] columns = new String[0];
  public String[][] values = new String[0][0];

  public String getName() {
    return name;
  }

  public Map getTags() {
    return tags;
  }

  public String[] getColumns() {
    return columns;
  }

  public String[][] getValues() {
    return values;
  }
}
