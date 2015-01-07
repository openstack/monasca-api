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

import monasca.api.MonApiConfiguration;
import org.influxdb.InfluxDB;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.skife.jdbi.v2.DBI;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import scala.actors.threadpool.Arrays;

@Test
public class AlarmStateHistoryInfluxDbRepositoryImplTest {

  @Mock(name = "mysql")
  private DBI mysql;

  @Mock
  private MonApiConfiguration monApiConfiguration;

  @Mock
  private InfluxDB influxDB;

  @InjectMocks
  private AlarmStateHistoryInfluxDbRepositoryImpl alarmStateHistoryInfluxDBRepository;

  @BeforeMethod(alwaysRun = true)
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  public void buildQueryForFindByIdTest() throws Exception {

    String er = "select alarm_id, metrics, old_state, new_state, reason, " +
        "" + "reason_data from alarm_state_history where tenant_id = 'tenant-id' and alarm_id = "
        + "'alarm-id' ";
    String r = this.alarmStateHistoryInfluxDBRepository.buildQueryForFindById("tenant-id",
        "alarm-id", null);

    assert (er.equals(r));

  }

  public void buildTimePartTest() {
    String er = " and time > 1388534400s and time < 1388534401s";
    String r = this.alarmStateHistoryInfluxDBRepository.buildTimePart(new DateTime(2014, 01, 01,
        0, 0, 0, DateTimeZone.UTC), new DateTime(2014, 01, 01, 0, 0, 1, DateTimeZone.UTC));
    assert (er.equals(r));

  }

  @SuppressWarnings("unchecked")
  public void buildAlarmsPartTest() {
    String er = " and ( alarm_id = 'id-1'  or  alarm_id = 'id-2' )";
    String r = this.alarmStateHistoryInfluxDBRepository.buildAlarmsPart(Arrays.asList(new
        String[]{"id-1", "id-2"}));
    assert (er.equals(r));
  }

  public void buildQueryForFindTest() throws Exception {
    String er = "select alarm_id, metrics, old_state, new_state, reason, " +
        "" + "reason_data from alarm_state_history where tenant_id = 'tenant-id'  and time > " +
        "1388559600s and time < 1388559601s  and ( alarm_id = 'id-1'  or  alarm_id = 'id-2' ) ";
    String r = this.alarmStateHistoryInfluxDBRepository.buildQueryForFind("tenant-id",
        " and time > 1388559600s and time < 1388559601s", " and ( alarm_id = 'id-1'  or  " +
            "alarm_id" + " = 'id-2' )", "");

    assert (er.equals(r));
  }

}
