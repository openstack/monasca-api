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

package monasca.api.infrastructure.persistence.vertica;

import monasca.common.model.alarm.AlarmState;
import monasca.api.domain.model.alarmstatehistory.AlarmStateHistoryRepo;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Timestamp;

import static org.testng.Assert.assertEquals;

@Test(groups = "database")
public class AlarmStateHistoryVerticaRepositoryImplTest {
  private DBI db;
  private Handle handle;
  private AlarmStateHistoryRepo repo;

  @BeforeClass
  protected void setupClass() throws Exception {
    Class.forName("com.vertica.jdbc.Driver");
    db = new DBI("jdbc:vertica://192.168.10.4/mon", "dbadmin", "password");
    handle = db.open();
    repo = new AlarmStateHistoryVerticaRepoImpl(db, null, null);
  }

  @AfterClass
  protected void afterClass() {
    handle.close();
  }

  @BeforeMethod
  protected void beforeMethod() {
    handle.execute("truncate table MonAlarms.StateHistory");
  }

  private void create(String tenantId, String alarmId, AlarmState oldState, AlarmState newState,
      String reason, String reasonData, DateTime timestamp) {
    try (Handle h = db.open()) {
      h.insert("insert into MonAlarms.StateHistory (tenant_id, alarm_id, old_state, new_state, "
          + "reason, reason_data, time_stamp) values (?, ?, ?, ?, ?, ?, ?)", tenantId, alarmId,
          oldState.name(), newState.name(), reason, reasonData,
          new Timestamp(timestamp.getMillis()));
    }
  }

  @Test
  public void shouldCreateAndFind() throws Exception {
    create("bob", "123", AlarmState.UNDETERMINED, AlarmState.ALARM, "foo", "bar", new DateTime());
    assertEquals(repo.findById("bob", "123", null, 1).size(), 1);
  }
}
