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
package monasca.api.infrastructure.persistence;

import com.google.inject.Inject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import monasca.api.ApiConfig;

public class PersistUtils {

  private final int maxQueryLimit;

  private final int DEFAULT_MAX_QUERY_LIMIT = 10000;

  private final SimpleDateFormat simpleDateFormatSpace =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSX");

  private final SimpleDateFormat simpleDateFormatSpaceOneDigitMilli =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SX");

  private final SimpleDateFormat simpleDateFormatT =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

  private final SimpleDateFormat simpleDateFormatTOneDigitMilli =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SX");

  @Inject
  public PersistUtils(ApiConfig config) {
    this.maxQueryLimit = config.maxQueryLimit;
  }

  public PersistUtils(int maxQueryLimit) {

    // maxQueryLimit could be 0 if binding for Google Guice is not setup.
    if (maxQueryLimit <= 0) {

      this.maxQueryLimit = DEFAULT_MAX_QUERY_LIMIT;

    } else {

      this.maxQueryLimit = maxQueryLimit;

    }
  }

  public PersistUtils() {

    this.maxQueryLimit = DEFAULT_MAX_QUERY_LIMIT;
  }

  public int getLimit(String limit)  {

    if (limit == null || limit.isEmpty()) {
      return this.maxQueryLimit;
    }

    int limitInt;
    try {
      limitInt = Integer.parseInt(limit);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(String.format("Found invalid Limit: '%1$s'. Limit must be a positive integer.", limit));
    }

    if (limitInt <= 0) {
      throw new IllegalArgumentException(String.format("Found invalid Limit: '%1$s'. Limit must be a positive integer.", limit));
    }

    if (limitInt <= this.maxQueryLimit) {

      return limitInt;

    } else {

      return this.maxQueryLimit;
    }

  }

  public Date parseTimestamp(String timestampString) throws ParseException {

    try {

      // Handles 2 and 3 digit millis. '2016-01-01 01:01:01.12Z' or '2016-01-01 01:01:01.123Z'
      return this.simpleDateFormatSpace.parse(timestampString);

    } catch (ParseException pe0) {

      try {

        // Handles 1 digit millis. '2016-01-01 01:01:01.1Z'
        return this.simpleDateFormatSpaceOneDigitMilli.parse(timestampString);

      } catch (ParseException pe1) {

        try {

          // Handles 2 and 3 digit millis with 'T'. Comes from the Python Persister.
          //  '2016-01-01T01:01:01.12Z' or '2016-01-01T01:01:01.123Z'
          return this.simpleDateFormatT.parse(timestampString);

        } catch (ParseException pe2) {

          // Handles 1 digit millis with 'T'. Comes from the Python Persister.
          // '2016-01-01T01:01:01.1Z'
          return this.simpleDateFormatTOneDigitMilli.parse(timestampString);

        }
      }
    }
  }

}
