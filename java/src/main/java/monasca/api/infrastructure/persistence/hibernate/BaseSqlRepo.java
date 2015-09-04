/*
 * Copyright 2015 FUJITSU LIMITED
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
 *
 */

package monasca.api.infrastructure.persistence.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Abstract foundation for ORM repositories.
 */
abstract class BaseSqlRepo {
  protected static final Marker ORM_LOG_MARKER = MarkerFactory.getMarker("ORM");
  private static final Logger LOG = LoggerFactory.getLogger(BaseSqlRepo.class);
  protected final SessionFactory sessionFactory;

  protected BaseSqlRepo(final SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /**
   * Rollbacks passed {@code tx} transaction if such is not null.
   * Assumption is being made that {@code tx} being null means transaction
   * has been successfully committed.
   *
   * @param tx {@link Transaction} object
   */
  protected void rollbackIfNotNull(final Transaction tx) {
    if (tx != null) {
      try {
        tx.rollback();
      } catch (RuntimeException rbe) {
        LOG.error(ORM_LOG_MARKER, "Couldn’t roll back transaction", rbe);
      }
    }
  }

  /**
   * Returns <b>UTC</b> based {@link DateTime#now()}
   *
   * @return current date/time in UTC
   *
   * @see DateTimeZone#UTC
   * @see DateTime#now()
   */
  protected DateTime getUTCNow() {
    return DateTime.now(DateTimeZone.UTC);
  }

}
