package com.hpcloud.mon.infrastructure.persistence;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.model.metric.MetricDefinitionRepository;

/**
 * @author Jonathan Halterman
 */
@Test(groups = "database")
public class MetricDefinitionRepositoryImplTest {
  private DBI db;
  private Handle handle;
  private MetricDefinitionRepository repo;

  @BeforeClass
  protected void setupClass() throws Exception {
    Class.forName("com.vertica.jdbc.Driver");
    db = new DBI("jdbc:vertica://192.168.10.8/mon", "dbadmin", "password");
    handle = db.open();
    repo = new MetricDefinitionRepositoryImpl(db);
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

    handle.execute("insert into MonMetrics.Definitions values ('/1', 'cpu_utilization', 'bob', '1')");
    handle.execute("insert into MonMetrics.Dimensions values ('/1', 'service', 'compute')");
    handle.execute("insert into MonMetrics.Dimensions values ('/1', 'instance_id', '123')");
    handle.execute("insert into MonMetrics.Dimensions values ('/1', 'flavor_id', '1')");
    handle.execute("insert into MonMetrics.Measurements (definition_id, time_stamp, value) values ('/1', '2014-01-01 00:00:00', 10)");
    handle.execute("insert into MonMetrics.Measurements (definition_id, time_stamp, value) values ('/1', '2014-01-01 00:01:00', 15)");

    handle.execute("insert into MonMetrics.Definitions values ('/2', 'cpu_utilization', 'bob', '1')");
    handle.execute("insert into MonMetrics.Dimensions values ('/2', 'service', 'compute')");
    handle.execute("insert into MonMetrics.Dimensions values ('/2', 'instance_id', '123')");
    handle.execute("insert into MonMetrics.Dimensions values ('/2', 'flavor_id', '2')");
    handle.execute("insert into MonMetrics.Measurements (definition_id, time_stamp, value) values ('/2', '2014-01-01 00:00:00', 12)");
    handle.execute("insert into MonMetrics.Measurements (definition_id, time_stamp, value) values ('/2', '2014-01-01 00:01:00', 13)");
  }

  public void shouldFind() {
    Map<String, String> dims = new HashMap<>();
    dims.put("service", "compute");
    dims.put("instance_id", "123");

    List<MetricDefinition> defs = repo.find("1234", "cpu_utilization", dims);
    assertEquals(defs.size(), 2);
  }
}
