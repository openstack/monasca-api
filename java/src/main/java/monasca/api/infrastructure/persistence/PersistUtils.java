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

import monasca.api.ApiConfig;

import com.google.inject.Inject;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;

public class PersistUtils {

  private static final Logger logger = LoggerFactory.getLogger(PersistUtils.class);

  private int maxQueryLimit;

  private final int DEFAULT_MAX_QUERY_LIMIT = 10000;

  private DateTimeFormatter isoFormat = ISODateTimeFormat.dateTime();

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

  public Date parseTimestamp(String timestampString) throws ParseException {
    return isoFormat.parseDateTime(timestampString.trim().replace(' ', 'T')).toDate();
  }
}
