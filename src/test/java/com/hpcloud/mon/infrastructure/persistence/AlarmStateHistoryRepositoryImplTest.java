package com.hpcloud.mon.infrastructure.persistence;

import java.sql.Timestamp;

import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistoryRepository;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test(groups = "database")
public class AlarmStateHistoryRepositoryImplTest {
  private DBI db;
  private Handle handle;
  private AlarmStateHistoryRepository repo;

  @BeforeClass
  protected void setupClass() throws Exception {
    Class.forName("com.vertica.jdbc.Driver");
    db = new DBI("jdbc:vertica://192.168.10.8/mon", "dbadmin", "password");
    handle = db.open();
    repo = new AlarmStateHistoryRepositoryImpl(null, db);
  }

  @AfterClass
  protected void afterClass() {
    handle.close();
  }

  @BeforeMethod
  protected void beforeMethod() {
    handle.execute("truncate table MonAlarms.StateHistory");
  }

  public void create(String tenantId, String alarmId, AlarmState oldState, AlarmState newState,
      String reason, String reasonData, DateTime timestamp) {
    try (Handle h = db.open()) {
      h.insert(
          "insert into MonAlarms.StateHistory (tenant_id, alarm_id, old_state, new_state, reason, reason_data, time_stamp) values (?, ?, ?, ?, ?, ?, ?)",
          tenantId, alarmId, oldState.name(), newState.name(), reason, reasonData, new Timestamp(
              timestamp.getMillis()));
    }
  }

  public void shouldCreateAndFind() {
    create("bob", "123", AlarmState.UNDETERMINED, AlarmState.ALARM, "foo", "bar",
        new DateTime());
    assertEquals(repo.findById("bob", "123").size(), 1);
  }
}
