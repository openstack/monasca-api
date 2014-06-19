package com.hpcloud.mon.infrastructure.persistence;

import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.model.metric.MetricDefinitionRepository;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

@Test(groups = "database")
public class MetricDefinitionVerticaRepositoryImplTest {
  private DBI db;
  private Handle handle;
  private MetricDefinitionRepository repo;

  @BeforeClass
  protected void setupClass() throws Exception {
    Class.forName("com.vertica.jdbc.Driver");
    db = new DBI("jdbc:vertica://192.168.10.8/mon", "dbadmin", "password");
    handle = db.open();
    repo = new MetricDefinitionVerticaRepositoryImpl(db);
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

    handle.execute("insert into MonMetrics.Definitions values ('/1', 'cpu_utilization', 'bob', '1')");

    handle.execute("insert into MonMetrics.Dimensions values ('/5', 'service', 'compute')");
    handle.execute("insert into MonMetrics.Dimensions values ('/5', 'instance_id', '123')");
    handle.execute("insert into MonMetrics.Dimensions values ('/5', 'flavor_id', '1')");
    handle.execute("insert into MonMetrics.DefinitionDimensions values ('/1', '/1', '/5')");
    handle.execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/1', '2014-01-01 00:00:00', 10)");
    handle.execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/1', '2014-01-01 00:01:00', 15)");

    handle.execute("insert into MonMetrics.Dimensions values ('/8', 'service', 'compute')");
    handle.execute("insert into MonMetrics.Dimensions values ('/8', 'instance_id', '123')");
    handle.execute("insert into MonMetrics.Dimensions values ('/8', 'flavor_id', '2')");
    handle.execute("insert into MonMetrics.DefinitionDimensions values ('/2', '/1', '/8')");
    handle.execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/2', '2014-01-01 00:00:00', 12)");
    handle.execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/2', '2014-01-01 00:01:00', 13)");

    handle.execute("insert into MonMetrics.DefinitionDimensions values ('/3', '/1', '')");
    handle.execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/3', '2014-01-01 00:00:00', 4)");
    handle.execute("insert into MonMetrics.Measurements (definition_dimensions_id, time_stamp, value) values ('/3', '2014-01-01 00:01:00', 8)");
  }

  public void shouldFindWithoutDimensions() throws Exception {
    List<MetricDefinition> defs = repo.find("bob", "cpu_utilization", null);
    assertEquals(defs.size(), 3);
  }

  public void shouldFindWithDimensions() throws Exception {
    Map<String, String> dims = new HashMap<>();
    dims.put("service", "compute");
    dims.put("instance_id", "123");

    List<MetricDefinition> defs = repo.find("bob", "cpu_utilization", dims);
    assertEquals(defs.size(), 2);

    dims.put("flavor_id", "2");
    defs = repo.find("bob", "cpu_utilization", dims);
    assertEquals(defs.size(), 1);
  }
}
