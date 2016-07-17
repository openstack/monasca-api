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

package monasca.api.integration.docker;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.DockerException;
import com.github.dockerjava.client.NotFoundException;
import com.github.dockerjava.client.model.ContainerCreateResponse;
import com.github.dockerjava.client.model.ExposedPort;
import com.github.dockerjava.client.model.Ports;
import com.sun.jersey.api.client.ClientResponse;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.from;

@Test(groups = "integration", enabled = true)
public class ITInfluxDBTest {

  private final static String INFLUXDB_IMAGE_NAME = "monasca/api-integ-tests-influxdb";
  private static final String MYSQL_IMAGE_NAME = "monasca/api-integ-tests-mysql";
  private static final String MYSQL_CONTAINER_RUN_CMD = "/usr/bin/mysqld_safe";
  private static final String KAFKA_IMAGE_NAME = "monasca/api-integ-tests-kafka";
  private static final String KAFKA_CONTAINER_RUN_CMD = "/run.sh";
  private static final String DOCKER_IP = "192.168.59.103";
  private static final String DOCKER_PORT = "2375";
  private static final String DOCKER_URL = "http://" + DOCKER_IP + ":" + DOCKER_PORT;
  private static final int MAX_CONNECT_PORT_TRIES = 10000;

  private final static DockerClient dockerClient = new DockerClient(DOCKER_URL);
  private Process apiProcess = null;
  private ContainerCreateResponse influxDBContainer = null;
  private ContainerCreateResponse mysqlContainer = null;
  private ContainerCreateResponse kafkaContainer = null;

  @BeforeClass
  public void setup() throws DockerException, IOException {

    try {

      runKafka();

      runInfluxDB();

      runMYSQL();

      runAPI();

    } catch (Exception e) {

      System.err.println("Failed to setup environment");
      System.err.println(e);
      tearDown();
      System.exit(-1);
    }
  }

  private void runAPI() throws Exception {

    if (!isPortFree(8070)) {
      throw new Exception("port 8070 is not free. Unable to start instance" + " of monasca api");
    }

    String latestShadedJarFileName = getLatestShadedJarFileName();
    System.out.println("Running " + latestShadedJarFileName);

    ProcessBuilder pb = new ProcessBuilder("java", "-cp", "./target/" + latestShadedJarFileName,
                                           "monasca.api.MonApiApplication", "server",
                                           "src/test/resources/mon-api-config.yml");
    File log = new File("mon-api-integration-test.log");
    pb.redirectErrorStream(true);
    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
    apiProcess = pb.start();

    System.out.println("Started " + latestShadedJarFileName);

    waitForPortReady("localhost", 8070);
  }

  private String getLatestShadedJarFileName() {

    File dir = new File("./target");
    FileFilter fileFilter = new RegexFileFilter("^mon-api-0\\.1\\.0-(\\d|\\w)+-(\\d|\\w)+\\.jar");
    File[] files = dir.listFiles(fileFilter);
    if (files.length == 0) {
      System.err.println("Failed to find shaded jar. You must build mon-api before running this "
                         + "test. Try 'mvn clean package'");
      tearDown();
      System.exit(-1);
    }
    System.out.println("Found " + files.length + " jar files");
    File latestFile = files[0];
    for (File file : files) {
      if (file.lastModified() > latestFile.lastModified()) {
        latestFile = file;
      }
    }

    System.out.println(latestFile.getName() + " is the latest jar file");
    return latestFile.getName();

  }

  boolean isPortFree(int port) {

    try (Socket s = new Socket("localhost", port)) {
      return false;
    } catch (Exception e) {
      return true;
    }

  }

  private void waitForPortReady(String host, int port) {

    System.out.println("waiting to connect to host [" + host + "] on port [" + port + "]");

    Socket s = null;
    boolean isPortReady = false;
    int tryCount = 0;
    while (!isPortReady) {

      if (tryCount >= MAX_CONNECT_PORT_TRIES) {
        System.err.println("Failed to connect to host [" + host + "] on port [" + port + "] in " +
                           "[" + tryCount + "] tries");
        tearDown();
        System.exit(-1);
      }

      try {
        s = new Socket();
        s.setReuseAddress(true);
        SocketAddress sa = new InetSocketAddress(host, port);
        s.connect(sa, 50000);
        isPortReady = true;
        System.out.println("Took " + tryCount + " tries to connect to host [" + host + "] on port" +
                           "[" + port + "]");
      } catch (Exception e) {
        tryCount++;
      }
    }

    if (s != null) {
      try {
        s.close();
      } catch (Exception e) {
        System.err.print(e);
      }
    }
  }

  private void runKafka() {

    ClientResponse response = dockerClient.pullImageCmd(KAFKA_IMAGE_NAME).exec();

    final ExposedPort tcp2181 = ExposedPort.tcp(2181);
    final ExposedPort tcp9092 = ExposedPort.tcp(9092);

    waitForCreateContainer(new CreateContainer(KAFKA_IMAGE_NAME) {
      @Override
      void createContainer() {
        kafkaContainer = dockerClient.createContainerCmd(KAFKA_IMAGE_NAME).withCmd(new
                                                                                       String[]{
            KAFKA_CONTAINER_RUN_CMD, DOCKER_IP}).withExposedPorts(tcp2181, tcp9092).exec();
      }
    });

    Ports portBindings2 = new Ports();
    portBindings2.bind(tcp2181, Ports.Binding(2181));
    portBindings2.bind(tcp9092, Ports.Binding(9092));

    dockerClient.startContainerCmd(kafkaContainer.getId()).withPortBindings(portBindings2).exec();

    waitForPortReady(DOCKER_IP, 2181);
    waitForPortReady(DOCKER_IP, 9092);
  }

  private void runMYSQL() {

    ClientResponse response = dockerClient.pullImageCmd(MYSQL_IMAGE_NAME).exec();

    final ExposedPort tcp3306 = ExposedPort.tcp(3306);

    waitForCreateContainer(new CreateContainer(MYSQL_IMAGE_NAME) {
      @Override
      void createContainer() {

        mysqlContainer = dockerClient.createContainerCmd(MYSQL_IMAGE_NAME).withCmd(new
                                                                                       String[]{
            MYSQL_CONTAINER_RUN_CMD}).withExposedPorts(tcp3306).exec();
      }
    });

    Ports portBindings1 = new Ports();
    portBindings1.bind(tcp3306, Ports.Binding(3306));

    dockerClient.startContainerCmd(mysqlContainer.getId()).withPortBindings(portBindings1).exec();

    waitForPortReady(DOCKER_IP, 3306);
  }

  private void runInfluxDB() {

    ClientResponse response = dockerClient.pullImageCmd(INFLUXDB_IMAGE_NAME).exec();

    final ExposedPort tcp8083 = ExposedPort.tcp(8083);
    final ExposedPort tcp8086 = ExposedPort.tcp(8086);
    final ExposedPort tcp8090 = ExposedPort.tcp(8090);
    final ExposedPort tcp8099 = ExposedPort.tcp(8099);

    waitForCreateContainer(new CreateContainer(INFLUXDB_IMAGE_NAME) {
      @Override
      void createContainer() {
        influxDBContainer = dockerClient.createContainerCmd(INFLUXDB_IMAGE_NAME).withExposedPorts
            (tcp8083, tcp8086, tcp8090, tcp8099).exec();
      }
    });

    Ports portBindings = new Ports();
    portBindings.bind(tcp8083, Ports.Binding(8083));
    portBindings.bind(tcp8086, Ports.Binding(8086));
    portBindings.bind(tcp8090, Ports.Binding(8090));
    portBindings.bind(tcp8099, Ports.Binding(8099));

    dockerClient.startContainerCmd(influxDBContainer.getId()).withPortBindings(portBindings).exec();

    waitForPortReady(DOCKER_IP, 8086);
  }

  @Test
  public void alarmCreateTest() {

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").body("{\"alarm_actions\": " +
                                                           "[\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], "
                                                           +
                                                           ""
                                                           + "\"ok_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], "
                                                           +
                                                           "\"name\": \"test-alarm-1\", \"description\": \"test-alarm-description\", "
                                                           +
                                                           "\"undetermined_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], "
                                                           +
                                                           "\"expression\": \"max(cpu_system_perc) > 0 and max(load_avg_1_min{hostname=mini-mon}) > "
                                                           +
                                                           "0\", \"severity\": \"low\"}")
        .post("/v2.0/alarms").then().assertThat().statusCode(201);

  }

  @Test
  public void alarmDeleteTest() {

    String json = given().headers("Accept", "application/json", "Content-Type",
                                  "application/json", "X-Auth-Token", "82510970543135")
        .body("{\"alarm_actions\": " +
              "[\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "" + "\"ok_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "\"name\": \"test-alarm-2\", \"description\": \"test-alarm-description\", " +
              "\"undetermined_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "\"expression\": \"max(cpu_system_perc) > 0 and max(load_avg_1_min{hostname=mini-mon}) > "
              +
              "0\", \"severity\": \"low\"}").post("/v2.0/alarms").asString();

    String alarmId = from(json).get("id");

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").delete("/v2.0/alarms/" + alarmId).then()
        .assertThat()
        .statusCode(204);

  }

  @Test
  public void alarmHistoryTest() {

    String json = given().headers("Accept", "application/json", "Content-Type",
                                  "application/json", "X-Auth-Token", "82510970543135")
        .body("{\"alarm_actions\": " +
              "[\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "" + "\"ok_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "\"name\": \"test-alarm-3\", \"description\": \"test-alarm-description\", " +
              "\"undetermined_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "\"expression\": \"max(cpu_system_perc) > 0 and max(load_avg_1_min{hostname=mini-mon}) > "
              +
              "0\", \"severity\": \"low\"}").post("/v2.0/alarms").asString();

    String alarmId = from(json).get("id");

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135")
        .get("v2.0/alarms/" + alarmId + "/state-history").then()
        .assertThat().statusCode(200);

  }

  @Test
  public void alarmListTest() {

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").get("/v2.0/alarms").then().assertThat()
        .statusCode(200);

  }

  @Test
  public void alarmPatchTest() {

    String json = given().headers("Accept", "application/json", "Content-Type",
                                  "application/json", "X-Auth-Token", "82510970543135")
        .body("{\"alarm_actions\": " +
              "[\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "" + "\"ok_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "\"name\": \"test-alarm-4\", \"description\": \"test-alarm-description\", " +
              "\"undetermined_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "\"expression\": \"max(cpu_system_perc) > 0 and max(load_avg_1_min{hostname=mini-mon}) > "
              +
              "0\", \"severity\": \"low\"}").post("/v2.0/alarms").asString();

    String alarmId = from(json).get("id");

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").body("{}").patch("v2.0/alarms/" + alarmId)
        .then()
        .assertThat().statusCode(200);

  }

  @Test
  public void alarmShowTest() {

    String json = given().headers("Accept", "application/json", "Content-Type",
                                  "application/json", "X-Auth-Token", "82510970543135")
        .body("{\"alarm_actions\": " +
              "[\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "" + "\"ok_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "\"name\": \"test-alarm-5\", \"description\": \"test-alarm-description\", " +
              "\"undetermined_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "\"expression\": \"max(cpu_system_perc) > 0 and max(load_avg_1_min{hostname=mini-mon}) > "
              +
              "0\", \"severity\": \"low\"}").post("/v2.0/alarms").asString();

    String alarmId = from(json).get("id");

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").get("v2.0/alarms/" + alarmId).then()
        .assertThat()
        .statusCode(200);

  }

  @Test
  public void alarmUpdateTest() {

    String json = given().headers("Accept", "application/json", "Content-Type",
                                  "application/json", "X-Auth-Token", "82510970543135")
        .body("{\"alarm_actions\": " +
              "[\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "" + "\"ok_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "\"name\": \"test-alarm-6\", \"description\": \"test-alarm-description\", " +
              "\"undetermined_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], " +
              "\"expression\": \"max(cpu_system_perc) > 0 and max(load_avg_1_min{hostname=mini-mon}) > "
              +
              "0\", \"severity\": \"low\"}").post("/v2.0/alarms").asString();

    String alarmId = from(json).get("id");

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").body("{\"alarm_actions\": " +
                                                           "[\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], "
                                                           +
                                                           ""
                                                           + "\"ok_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], "
                                                           +
                                                           "\"name\": \"test-alarm-6\", \"description\": \"test-alarm-description\", "
                                                           +
                                                           "\"undetermined_actions\": [\"044fa9be-36ef-4e51-a1d9-67ec31734908\"], "
                                                           +
                                                           "\"expression\": \"max(cpu_system_perc) > 0 and max(load_avg_1_min{hostname=mini-mon}) > "
                                                           +
                                                           "0\", \"severity\": \"low\", \"actions_enabled\":\"true\", "
                                                           +
                                                           "\"state\": \"alarm\"}").put("/v2" +
                                                                                        ".0/alarms/"
                                                                                        + alarmId)
        .then().assertThat().statusCode(200);

  }

  @Test
  public void measurementListTest() {

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").param("start_time", "1970-01-01T00:00:00Z")
        .param
            ("name", "cpu_system_perc").get("v2.0/metrics/measurements").then().assertThat()
        .statusCode(200);

  }

  @Test
  public void metricCreateTest() {

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135")
        .body("{\"timestamp\": 0, \"name\": \"test-metric-1\", " +
              "\"value\": 1234.5678, \"dimensions\": {\"foo\": \"bar\", " +
              "\"biz\": \"baz\"}}").post("/v2.0/metrics ").then().assertThat().statusCode(204);

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").param("start_time", "1970-01-01T00:00:00Z")
        .param
            ("name", "test-metric-1").get("v2.0/metrics/measurements").then().assertThat()
        .statusCode
            (200);


  }

  @Test
  public void metricCreateRawTest() {

    long unixTime = System.currentTimeMillis();

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135")
        .body("{\"timestamp\":\"" + unixTime + "\" , " +
              "\"name\": \"test-metric-2\", " +
              "\"value\": 1234.5678, \"dimensions\": {\"foo\": \"bar\", " +
              "\"biz\": \"baz\"}}").post("/v2.0/metrics ").then().assertThat().statusCode(204);

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").param("start_time", "1970-01-01T00:00:00Z")
        .param
            ("name", "test-metric-2").get("v2.0/metrics/measurements").then().assertThat()
        .statusCode
            (200);

  }

  @Test
  public void metricList() {

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").get("/v2.0/metrics").then().assertThat()
        .statusCode(500);

  }

  @Test
  public void metricStatisticsTest() {

    String[] stats = new String[]{"avg", "min", "max", "count", "sum"};

    for (String stat : stats) {
      given().headers("Accept", "application/json", "Content-Type", "application/json",
                      "X-Auth-Token", "82510970543135").param("start_time", "1970-01-01T00:00:00Z")
          .param
              ("statistics", stat).param("name", "cpu_system_perc").get("/v2.0/metrics/statistics")
          .then().assertThat().statusCode(200);
    }

  }

  @Test
  public void notificationCreateTest() {

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").body("{\"type\": \"email\", " +
                                                           ""
                                                           + "\"name\": \"test-notification-1\", \"address\": \"jdoe@gmail.com\"}")
        .post("/v2" +
              ".0/notification-methods").then().assertThat().statusCode(201);
  }

  @Test
  public void notificationDeleteTest() {

    String json = given().headers("Accept", "application/json", "Content-Type",
                                  "application/json", "X-Auth-Token", "82510970543135")
        .body("{\"type\": \"email\", " +
              "" + "\"name\": \"test-notification-2\", \"address\": \"jdoe@gmail.com\"}")
        .post("/v2" +
              ".0/notification-methods").asString();

    String notificationId = from(json).get("id");

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135")
        .delete("/v2.0/notification-methods/" + notificationId)
        .then().assertThat().statusCode(204);


  }

  @Test
  public void notificationList() {

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").get("/v2.0/notification-methods").then()
        .assertThat()
        .statusCode(200);

  }

  @Test
  public void notificationShowTest() {

    String json = given().headers("Accept", "application/json", "Content-Type",
                                  "application/json", "X-Auth-Token", "82510970543135")
        .body("{\"type\": \"email\", " +
              "" + "\"name\": \"test-notification-3\", \"address\": \"jdoe@gmail.com\"}")
        .post("/v2" +
              ".0/notification-methods").asString();

    String notificationId = from(json).get("id");

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135")
        .get("/v2.0/notification-methods/" + notificationId)
        .then().assertThat().statusCode(200);

  }

  @Test
  public void notificationUpdateTest() {

    String json = given().headers("Accept", "application/json", "Content-Type",
                                  "application/json", "X-Auth-Token", "82510970543135")
        .body("{\"type\": \"email\", " +
              "" + "\"name\": \"test-notification-4\", \"address\": \"jdoe@gmail.com\"}")
        .post("/v2" +
              ".0/notification-methods").asString();

    String notificationId = from(json).get("id");

    given().headers("Accept", "application/json", "Content-Type", "application/json",
                    "X-Auth-Token", "82510970543135").body("{\"type\": \"email\", " +
                                                           ""
                                                           + "\"name\": \"test-notification-4\", \"address\": \"jsmith@gmail.com\"}")
        .put("/v2" +
             ".0/notification-methods/" + notificationId).then().assertThat().statusCode(200);

    json = given().headers("Accept", "application/json", "Content-Type", "application/json",
                           "X-Auth-Token", "82510970543135")
        .get("/v2.0/notification-methods/" + notificationId)
        .asString();

    String address = from(json).get("address");

    assert (address.equals("jsmith@gmail.com"));


  }

  @AfterClass
  public void tearDown() {

    stopAPI();

    stopMYSQL();

    stopInfluxDB();

    stopKafka();


  }

  private void stopAPI() {
    if (apiProcess != null) {
      apiProcess.destroy();
    }
  }

  private void stopKafka() {
    if (kafkaContainer != null) {
      dockerClient.stopContainerCmd(kafkaContainer.getId()).withTimeout(2).exec();
    }

  }

  private void stopMYSQL() {
    if (mysqlContainer != null) {
      dockerClient.stopContainerCmd(mysqlContainer.getId()).withTimeout(2).exec();
    }


  }

  private void stopInfluxDB() {
    if (influxDBContainer != null) {
      dockerClient.stopContainerCmd(influxDBContainer.getId()).withTimeout(2).exec();
    }

  }

  private static abstract class CreateContainer {

    private String imageName;

    private CreateContainer(String imageName) {
      this.imageName = imageName;
    }

    abstract void createContainer();

    String getImageName() {
      return imageName;
    }

  }

  private void waitForCreateContainer(CreateContainer createContainer) {

    boolean isContainerCreated = false;
    while (!isContainerCreated) {
      try {
        createContainer.createContainer();
        isContainerCreated = true;
      } catch (NotFoundException e) {
        System.out.println("Waiting for image " + createContainer.getImageName() + " to be pulled");
      }
    }
  }
}
