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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import monasca.api.ApiConfig;

public class PersistUtils {

  private static final Logger logger = LoggerFactory.getLogger(PersistUtils.class);

  private int maxQueryLimit;

  private final int DEFAULT_MAX_QUERY_LIMIT = 10000;

  private final ThreadLocal<SimpleDateFormat> simpleDateFormatSpace = new ThreadLocal<>();
  private static final String FORMAT_WITH_SPACE = "yyyy-MM-dd HH:mm:ss.SSSX";

  private final ThreadLocal<SimpleDateFormat> simpleDateFormatSpaceOneDigitMilli = new ThreadLocal<>();
  private static final String FORMAT_WITH_SPACE_ONE_DIGIT_MILLI = "yyyy-MM-dd HH:mm:ss.SX";

  private final ThreadLocal<SimpleDateFormat> simpleDateFormatT = new ThreadLocal<>();
  private static final String FORMAT_WITH_T = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

  private final ThreadLocal<SimpleDateFormat> simpleDateFormatTOneDigitMilli = new ThreadLocal<>();
  private static final String FORMAT_WITH_T_ONE_DIGIT_MILLI = "yyyy-MM-dd'T'HH:mm:ss.SX";

  @Inject
  public PersistUtils(ApiConfig config) {

    setMaxQueryLimit(config.maxQueryLimit);

  }

  private void setMaxQueryLimit(int maxQueryLimit) {

    // maxQueryLimit could be 0 if maxQueryLimit is not specified in the config file.
    if (maxQueryLimit <= 0) {

      logger.warn(String.format("Found invalid maxQueryLimit: [%1d]. maxQueryLimit must be a positive integer.", maxQueryLimit));
      logger.warn(String.format("Setting maxQueryLimit to default: [%1d]", DEFAULT_MAX_QUERY_LIMIT));
      logger.warn("Please check your config file for a valid maxQueryLimit property");

      this.maxQueryLimit = DEFAULT_MAX_QUERY_LIMIT;

    } else {

      this.maxQueryLimit = maxQueryLimit;
    }
  }

  public PersistUtils(int maxQueryLimit) {

    setMaxQueryLimit(maxQueryLimit);

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
      throw new IllegalArgumentException(
          String.format("Found invalid Limit: '%1$s'. Limit must be a positive integer.", limit));
    }

    if (limitInt <= 0) {
      throw new IllegalArgumentException(
          String.format("Found invalid Limit: '%1$s'. Limit must be a positive integer.", limit));
    }

    if (limitInt <= this.maxQueryLimit) {

      return limitInt;

    } else {

      return this.maxQueryLimit;
    }
  }

  private Date parseForFormat(final String timeStamp,
                              final ThreadLocal<SimpleDateFormat> formatter,
                              final String format) throws ParseException {
    if (formatter.get() == null) {
      formatter.set(new SimpleDateFormat(format));
    }
    return formatter.get().parse(timeStamp);
  }

  public Date parseTimestamp(String timestampString) throws ParseException {

    try {

      // Handles 2 and 3 digit millis. '2016-01-01 01:01:01.12Z' or '2016-01-01 01:01:01.123Z'
      return parseForFormat(timestampString, this.simpleDateFormatSpace, FORMAT_WITH_SPACE);

    } catch (ParseException pe0) {

      try {

        // Handles 1 digit millis. '2016-01-01 01:01:01.1Z'
        return parseForFormat(timestampString, this.simpleDateFormatSpaceOneDigitMilli,
            FORMAT_WITH_SPACE_ONE_DIGIT_MILLI);

      } catch (ParseException pe1) {

        try {

          // Handles 2 and 3 digit millis with 'T'. Comes from the Python Persister.
          //  '2016-01-01T01:01:01.12Z' or '2016-01-01T01:01:01.123Z'
          return parseForFormat(timestampString, this.simpleDateFormatT, FORMAT_WITH_T);

        } catch (ParseException pe2) {

          // Handles 1 digit millis with 'T'. Comes from the Python Persister.
          // '2016-01-01T01:01:01.1Z'
          return parseForFormat(timestampString, this.simpleDateFormatTOneDigitMilli,
              FORMAT_WITH_T_ONE_DIGIT_MILLI);
        }
      }
    }
  }
}
