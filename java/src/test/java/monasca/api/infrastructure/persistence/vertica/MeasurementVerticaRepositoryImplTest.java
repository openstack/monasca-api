/*
 * Copyright (c) 2014, 2016 Hewlett-Packard Development Company, L.P.
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

import monasca.api.ApiConfig;
import monasca.api.domain.model.measurement.MeasurementRepo;
import monasca.api.domain.model.measurement.Measurements;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

@Test(groups = "database")
public class MeasurementVerticaRepositoryImplTest {
  private ApiConfig config;
  private DBI db;
  private Handle handle;
  private MeasurementRepo repo;

  @BeforeClass
  protected void setupClass() throws Exception {
    Class.forName("com.vertica.jdbc.Driver");
    db = new DBI("jdbc:vertica://192.168.10.4/mon", "dbadmin", "password");
    handle = db.open();
    config = new ApiConfig();
    repo = new MeasurementVerticaRepoImpl(db, config);
  }

  @AfterClass
  protected void afterClass() {
    handle.close();
  }

  @BeforeMethod
  protected void beforeMethod() {
    handle.execute("truncate table MonMetrics.Definitions");
    handle.execute("truncate table MonMetrics.Dimensions");
    handle.execute("truncate table MonMetrics.Measurements");
    handle.execute("truncate table MonMetrics.DefinitionDimensions");

    handle
        .execute("insert into MonMetrics.Definitions values ('/1', 'cpu_utilization', 'bob', '1')");

    handle.execute("insert into MonMetrics.Dimensions values ('/5', 'service', 'compute')");
    handle.execute("insert into MonMetrics.Dimensions values ('/5', 'instance_id', '123')");
    handle.execute("insert into MonMetrics.Dimensions values ('/5', 'flavor_id', '1')");
    handle.execute("insert into MonMetrics.DefinitionDimensions values ('/1', '/1', '/5')");
    handle
        .execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/1', '2014-01-01 00:00:00', 10)");
    handle
        .execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/1', '2014-01-01 00:01:00', 15)");

    handle.execute("insert into MonMetrics.Dimensions values ('/8', 'service', 'compute')");
    handle.execute("insert into MonMetrics.Dimensions values ('/8', 'instance_id', '123')");
    handle.execute("insert into MonMetrics.Dimensions values ('/8', 'flavor_id', '2')");
    handle.execute("insert into MonMetrics.DefinitionDimensions values ('/2', '/1', '/8')");
    handle
        .execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/2', '2014-01-01 00:00:00', 12)");
    handle
        .execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/2', '2014-01-01 00:01:00', 13)");

    handle.execute("insert into MonMetrics.DefinitionDimensions values ('/3', '/1', '')");
    handle
        .execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/3', '2014-01-01 00:00:00', 4)");
    handle
        .execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/3', '2014-01-01 00:01:00', 8)");
  }

  public void shouldFindWithoutDimensions() throws Exception {
    Collection<Measurements> measurements =
        repo.find("bob", "cpu_utilization", null, new DateTime(2014, 1, 1, 0, 0, 0), null, null, 1,
                  false, null);
    assertEquals(measurements.size(), 3);
  }

  public void shouldFindWithDimensions() throws Exception {
    Map<String, String> dims = new HashMap<>();
    dims.put("service", "compute");
    dims.put("instance_id", "123");

    Collection<Measurements> measurements =
        repo.find("bob", "cpu_utilization", dims, new DateTime(2014, 1, 1, 0, 0), null, null, 1,
                  false, null);
    assertEquals(measurements.size(), 2);

    dims.put("flavor_id", "2");
    measurements = repo.find("bob", "cpu_utilization", dims, new DateTime(2014, 1, 1, 0, 0), null, null, 1,
                             false, null);
    assertEquals(measurements.size(), 1);
  }
}
