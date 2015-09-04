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

import java.util.Random;

import org.joda.time.DateTime;

class TestHelper {
  private static final int SLEEP_TIME_RANDOM_BYTE_ARRAY = 30;

  private TestHelper() {
  }

  static byte[] randomByteArray(final int length) {
    return randomByteArray(length, true);
  }

  static byte[] randomByteArray(final int length, final boolean sleep) {
    if (sleep) {
      try {
        Thread.sleep(SLEEP_TIME_RANDOM_BYTE_ARRAY);
      } catch (InterruptedException e) {
        System.err.println(e.getLocalizedMessage());
        throw new RuntimeException(e);
      }
    }
    byte[] b = new byte[length];
    new Random(DateTime.now().getMillis()).nextBytes(b);
    return b;
  }

}
