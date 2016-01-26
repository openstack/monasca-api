/*
 * Copyright (c) 2014,2016 Hewlett Packard Enterprise Development Company, L.P.
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
package monasca.api.infrastructure.persistence;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.skife.jdbi.v2.Query;

/**
 * Utilities for building dimension queries.
 *
 * This class has issues with testing with mockito because bind method on Query class is final.
 */
public final class DimensionQueries {

  private DimensionQueries() {}

  public static void bindDimensionsToQuery(
      Query<?> query,
      Map<String, String> dimensions) {

    if (dimensions != null) {
      int i = 0;
      for (Iterator<Map.Entry<String, String>> it = dimensions.entrySet().iterator(); it.hasNext(); i++) {
        Map.Entry<String, String> entry = it.next();
        query.bind("dname" + i, entry.getKey());
        if (!Strings.isNullOrEmpty(entry.getValue())) {
          List<String> values = Splitter.on('|').splitToList(entry.getValue());
          int j = 0;
          for (String value : values) {
            query.bind("dvalue" + i + '_' + j, value);
            j++;
          }
        }
      }
    }
  }

  public static Map<String, String> dimensionsFor(String dimensionSet) {

    Map<String, String> dimensions = Collections.emptyMap();

    if (dimensionSet != null) {
      dimensions = new HashMap<>();
      for (String kvStr : dimensionSet.split(",")) {
        String[] kv = kvStr.split("=");
        if (kv.length > 1)
          dimensions.put(kv[0], kv[1]);
      }
    }

    return dimensions;
  }
}
