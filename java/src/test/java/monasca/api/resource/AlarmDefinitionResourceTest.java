/*
 * Copyright (c) 2014,2016 Hewlett Packard Enterprise Development Company, L.P.
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

package monasca.api.resource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.testng.annotations.Test;

import monasca.api.app.AlarmDefinitionService;
import monasca.api.app.command.CreateAlarmDefinitionCommand;
import monasca.api.app.command.UpdateAlarmDefinitionCommand;
import monasca.api.domain.model.common.Paged;
import monasca.api.infrastructure.persistence.PersistUtils;
import monasca.common.model.alarm.AlarmExpression;
import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepo;
import monasca.api.domain.model.common.Link;
import monasca.api.resource.exception.ErrorMessages;
import monasca.common.model.alarm.AlarmSeverity;

import com.sun.jersey.api.client.ClientResponse;

@Test
public class AlarmDefinitionResourceTest extends AbstractMonApiResourceTest {
  private String expression;
  private String detExpression;
  private AlarmDefinition alarm;
  private AlarmDefinition detAlarm;
  private AlarmDefinition alarmItem;
  private AlarmDefinitionService service;
  private AlarmDefinitionRepo repo;
  private List<String> alarmActions;

  @Override
  @SuppressWarnings("unchecked")
  protected void setupResources() throws Exception {
    super.setupResources();

    expression = "avg(disk_read_ops{service=hpcs.compute, instance_id=937}) >= 90";
    detExpression = "count(log.error{service=test,instance_id=2},deterministic) >= 10 times 10";
    List<String> matchBy = Arrays.asList("service", "instance_id");
    alarmItem =
        new AlarmDefinition("123", "Disk Exceeds 1k Operations", null, "LOW", expression,
            Arrays.asList("service", "instance_id"), true, null, null, null);
    alarmActions = new ArrayList<String>();
    alarmActions.add("29387234");
    alarmActions.add("77778687");

    alarm =
        new AlarmDefinition("123", "Disk Exceeds 1k Operations", null, "LOW", expression, matchBy,
            true, alarmActions, null, null);
    detAlarm =
        new AlarmDefinition("456", "log.error", null, "LOW", detExpression, matchBy,
            true, alarmActions, null, null);

    service = mock(AlarmDefinitionService.class);

    when(
        service.create(eq("abc"), eq("Disk Exceeds 1k Operations"), any(String.class), eq("LOW"),
            eq(expression), eq(AlarmExpression.of(expression)), eq(matchBy), any(List.class),
            any(List.class), any(List.class))).thenReturn(alarm);
    when(
        service.create(eq("abc"), eq("log.error"), any(String.class), eq("LOW"),
            eq(detExpression), eq(AlarmExpression.of(detExpression)), eq(matchBy), any(List.class),
            any(List.class), any(List.class))).thenReturn(detAlarm);

    repo = mock(AlarmDefinitionRepo.class);
    when(repo.findById(eq("abc"), eq("123"))).thenReturn(alarm);
    when(repo.findById(eq("abc"), eq("456"))).thenReturn(detAlarm);
    when(repo.find(anyString(), anyString(), (Map<String, String>) anyMap(), anyListOf(
        AlarmSeverity.class), (List<String>) anyList(), anyString(), anyInt())).thenReturn(
        Arrays.asList(alarmItem));

    addResources(new AlarmDefinitionResource(service, repo, new PersistUtils()));
  }

  @SuppressWarnings("unchecked")
  public void shouldCreate() {
    ClientResponse response =
        createResponseFor(new CreateAlarmDefinitionCommand("Disk Exceeds 1k Operations", null,
            expression, Arrays.asList("service", "instance_id"), "LOW", alarmActions, null, null));

    assertEquals(response.getStatus(), 201);
    AlarmDefinition newAlarm = response.getEntity(AlarmDefinition.class);
    String location = response.getHeaders().get("Location").get(0);
    assertEquals(location, "/v2.0/alarm-definitions/" + newAlarm.getId());
    assertEquals(newAlarm, alarm);
    verify(service).create(eq("abc"), eq("Disk Exceeds 1k Operations"), any(String.class),
                           eq("LOW"), eq(expression), eq(AlarmExpression.of(expression)),
                           eq(Arrays.asList("service", "instance_id")), any(List.class),
                           any(List.class), any(List.class));
  }

  public void shouldCreateDeterministic() {
    final CreateAlarmDefinitionCommand request = new CreateAlarmDefinitionCommand(
        "log.error",
        null,
        detExpression,
        Arrays.asList("service", "instance_id"),
        "LOW",
        alarmActions,
        null,
        null
    );
    final ClientResponse response = this.createResponseFor(request);

    assertEquals(response.getStatus(), 201);
    AlarmDefinition newAlarm = response.getEntity(AlarmDefinition.class);
    String location = response.getHeaders().get("Location").get(0);
    assertEquals(location, "/v2.0/alarm-definitions/" + newAlarm.getId());
    assertEquals(newAlarm, detAlarm);

    verify(service).create(eq("abc"), eq("log.error"), any(String.class),
        eq("LOW"), eq(detExpression), eq(AlarmExpression.of(detExpression)),
        eq(Arrays.asList("service", "instance_id")), any(List.class),
        any(List.class), any(List.class));
  }

  public void shouldUpdate() {
    when(
        service.update(eq("abc"), eq("123"), any(AlarmExpression.class),
            any(UpdateAlarmDefinitionCommand.class))).thenReturn(alarm);
    ClientResponse response =
        client()
            .resource("/v2.0/alarm-definitions/123")
            .header("X-Tenant-Id", "abc")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .put(ClientResponse.class,
                 new UpdateAlarmDefinitionCommand("Disk Exceeds 1k Operations", "", expression,
                                                  Arrays.asList("service", "instance_id"), "LOW",
                                                  true, alarmActions, new ArrayList<String>(),
                                                  new ArrayList<String>()));

    assertEquals(response.getStatus(), 200);
    verify(service).update(eq("abc"), eq("123"), any(AlarmExpression.class),
        any(UpdateAlarmDefinitionCommand.class));
  }

  public void shouldErrorOnCreateWithDuplicateDimensions() {
    String expression =
        "avg(hpcs.compute{instance_id=937, instance_id=123, az=2, instance_uuid=abc123, metric_name=disk_read_ops}) >= 90";
    ClientResponse response =
        createResponseFor(new CreateAlarmDefinitionCommand("Disk Exceeds 1k Operations", null,
            expression, Arrays.asList("service", "instance_id"), "LOW", alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
                                                                       "The alarm expression is invalid",
                                                                       "More than one value was given for dimension instance_id");
  }

  @SuppressWarnings("unchecked")
  public void shouldNotRequireDimensionsForCustomNamespace() {
    String expression = "avg(foo{metric_name=bar}) >= 90";
    when(
        service.create(eq("abc"), eq("Disk Exceeds 1k Operations"), any(String.class), eq("LOW"),
            eq(expression), eq(AlarmExpression.of(expression)), any(List.class), any(List.class),
            any(List.class), any(List.class))).thenReturn(alarm);
    ClientResponse response =
        createResponseFor(new CreateAlarmDefinitionCommand("Disk Exceeds 1k Operations", null,
            expression, Arrays.asList("service", "instance_id"), "LOW", alarmActions, null, null));
    assertEquals(response.getStatus(), 201);
  }

  public void shouldErrorOnCreateWithInvalidJson() {
    ClientResponse response =
        createResponseFor("{\"alarmasdf\"::{\"name\":\"Disk Exceeds 1k Operations\"}}");

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("bad_request", 400,
                                                                       "Unable to process the provided JSON",
                                                                       "Unexpected character (':'");
  }

  public void shouldErrorOnCreateWithInvalidOperator() {
    String expression =
        "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops}) ^ 90";
    ClientResponse response =
        createResponseFor(new CreateAlarmDefinitionCommand("Disk Exceeds 1k Operations", null,
            expression, Arrays.asList("service", "instance_id"), "LOW", alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
                                                                       "The alarm expression is invalid",
                                                                       "Syntax Error");
  }

  public void shouldErrorOnCreateWith0Period() {
    String expression =
        "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops},0) >= 90";
    ClientResponse response =
        createResponseFor(new CreateAlarmDefinitionCommand("Disk Exceeds 1k Operations", null,
            expression, Arrays.asList("service", "instance_id"), "LOW", alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
                                                                       "Period must not be 0");
  }

  public void shouldErrorOnCreateWithNonMod60Period() {
    String expression =
        "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops},61) >= 90";
    ClientResponse response =
        createResponseFor(new CreateAlarmDefinitionCommand("Disk Exceeds 1k Operations", null,
            expression, Arrays.asList("service", "instance_id"), "LOW", alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
                                                                       "Period 61 must be a multiple of 60");
  }

  public void shouldErrorOnCreateWithPeriodsLessThan1() {
    String expression =
        "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops}) >= 90 times 0";
    ClientResponse response =
        createResponseFor(new CreateAlarmDefinitionCommand("Disk Exceeds 1k Operations", null,
            expression, Arrays.asList("service", "instance_id"), "LOW", alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
                                                                       "Periods 0 must be greater than or equal to 1");
  }

  public void shouldErrorOnCreateWithPeriodTimesPeriodsGT2Weeks() {
    String expression =
        "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops},60) >= 90 times 20161";
    ClientResponse response =
        createResponseFor(new CreateAlarmDefinitionCommand("Disk Exceeds 1k Operations", null,
            expression, Arrays.asList("service", "instance_id"), "LOW", alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Period 60 times 20161 must total less than 2 weeks in seconds (1209600)");
  }

  public void shouldErrorOnCreateWithTooLongName() {
    String expression =
        "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops}) >= 90";
    ClientResponse response =
        createResponseFor(new CreateAlarmDefinitionCommand(
            "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
                + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
                + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789",
            null, expression, Arrays.asList("service", "instance_id"), "LOW", alarmActions, null,
            null));

    ErrorMessages
        .assertThat(response.getEntity(String.class))
        .matches("unprocessable_entity", 422,
                 "Name 012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789 must be 255 characters or less");
  }

  public void shouldErrorOnCreateWithTooLongAlarmAction() {
    alarmActions = new ArrayList<String>();
    alarmActions.add("012345678901234567890123456789012345678901234567890");
    ClientResponse response =
        createResponseFor(new CreateAlarmDefinitionCommand("Disk Exceeds 1k Operations", null,
            expression, Arrays.asList("service", "instance_id"), "LOW", alarmActions, null, null));

    ErrorMessages
        .assertThat(response.getEntity(String.class))
        .matches("unprocessable_entity", 422,
                 "Alarm action 012345678901234567890123456789012345678901234567890 must be 50 characters or less");
  }

  @SuppressWarnings("unchecked")
  public void shouldList() {


    Map<String, Object> lhm = (Map<String, Object>) client().resource("/v2.0/alarm-definitions").header("X-Tenant-Id", "abc")
        .get(Paged.class).elements.get(0);

    AlarmDefinition ad = new AlarmDefinition((String) lhm.get("id"), (String) lhm.get("name"),
                                             (String) lhm.get("description"),
                                             (String) lhm.get("severity"),
                                             (String) lhm.get("expression"),
                                             (List<String>) lhm.get("match_by"),
                                             (boolean) lhm.get("actions_enabled"),
                                             (List<String>) lhm.get("alarm_actions"),
                                             (List<String>) lhm.get("ok_actions"),
                                             (List<String>) lhm.get("undetermined_actions"));


    List<Map<String, String>> links = (List<Map<String, String>>) lhm.get("links");
    List<Link> linksList = Arrays.asList(new Link(links.get(0).get("rel"), links.get(0).get("href")));

    ad.setLinks(linksList);

    List<AlarmDefinition> alarms = Arrays.asList(ad);

    assertEquals(alarms, Arrays.asList(alarmItem));

    verify(repo).find(eq("abc"), anyString(), (Map<String, String>) anyMap(), anyListOf(AlarmSeverity.class),
                      (List<String>) anyList(),
                      anyString(), anyInt());
  }

  @SuppressWarnings("unchecked")
  public void shouldListByName() throws Exception {
    Map<String, Object>
        lhm =
        (Map<String, Object>) client()
            .resource("/v2.0/alarm-definitions?name=" + URLEncoder.encode("foo bar baz", "UTF-8"))
            .header("X-Tenant-Id", "abc").get(Paged.class).elements.get(0);

    AlarmDefinition
        ad =
        new AlarmDefinition((String) lhm.get("id"), (String) lhm.get("name"),
                            (String) lhm.get("description"), (String) lhm.get("severity"),
                            (String) lhm.get("expression"), (List<String>) lhm.get("match_by"),
                            (boolean) lhm.get("actions_enabled"),
                            (List<String>) lhm.get("alarm_actions"),
                            (List<String>) lhm.get("ok_actions"),
                            (List<String>) lhm.get("undetermined_actions"));

    List<Map<String, String>> links = (List<Map<String, String>>) lhm.get("links");
    List<Link>
        linksList =
        Arrays.asList(new Link(links.get(0).get("rel"), links.get(0).get("href")));

    ad.setLinks(linksList);

    List<AlarmDefinition> alarms = Arrays.asList(ad);

    assertEquals(alarms, Arrays.asList(alarmItem));
    verify(repo).find(eq("abc"), eq("foo bar baz"), (Map<String, String>) anyMap(), anyListOf(AlarmSeverity.class), (List<String>) anyList(),
                      anyString(), anyInt());
  }

  public void shouldGet() {
    assertEquals(
        client().resource("/v2.0/alarm-definitions/123").header("X-Tenant-Id", "abc")
            .get(AlarmDefinition.class), alarm);
    verify(repo).findById(eq("abc"), eq("123"));
  }

  public void should404OnGetInvalid() {
    doThrow(new EntityNotFoundException(null)).when(repo).findById(eq("abc"), eq("999"));

    try {
      client().resource("/v2.0/alarm-definitions/999").header("X-Tenant-Id", "abc").get(
          AlarmDefinition.class);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("404"));
    }
  }

  public void shouldDelete() {
    ClientResponse response =
        client().resource("/v2.0/alarm-definitions/123").header("X-Tenant-Id", "abc")
            .delete(ClientResponse.class);
    assertEquals(response.getStatus(), 204);
    verify(service).delete(eq("abc"), eq("123"));
  }

  public void should404OnDeleteInvalid() {
    doThrow(new EntityNotFoundException(null)).when(service).delete(eq("abc"), eq("999"));

    try {
      client().resource("/v2.0/alarm-definitions/999").header("X-Tenant-Id", "abc").delete();
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("404"));
    }
  }

  @SuppressWarnings("unchecked")
  public void should500OnInternalException() {
    doThrow(new RuntimeException("")).when(repo).find(anyString(), anyString(),

        (Map<String, String>) anyObject(), anyListOf(AlarmSeverity.class), (List<String>) anyList(), anyString(), anyInt());

    try {
      client().resource("/v2.0/alarm-definitions").header("X-Tenant-Id", "abc").get(List.class);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("500"), e.getMessage());
    }
  }

  public void shouldHydateLinksOnList() {
    List<Link> expected = Arrays.asList(new Link("self", "/v2.0/alarm-definitions/123"));

    Map<String, Object>
        lhm =
        (Map<String, Object>) client().resource("/v2.0/alarm-definitions").header("X-Tenant-Id", "abc")
            .get(Paged.class).elements.get(0);

    List<Map<String, String>> links = (List<Map<String, String>>) lhm.get("links");

    List<Link> actual = Arrays.asList(new Link(links.get(0).get("rel"), links.get(0).get("href")));
    assertEquals(actual, expected);
  }

  public void shouldHydateLinksOnGet() {
    List<Link> links =
        Arrays.asList(new Link("self", "/v2.0/alarm-definitions/123"));
    assertEquals(
        client().resource("/v2.0/alarm-definitions/123").header("X-Tenant-Id", "abc")
            .get(AlarmDefinition.class).getLinks(), links);
  }

  private ClientResponse createResponseFor(Object request) {
    return client().resource("/v2.0/alarm-definitions").header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON).post(ClientResponse.class, request);
  }
}
