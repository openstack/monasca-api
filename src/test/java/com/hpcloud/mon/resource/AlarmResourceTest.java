package com.hpcloud.mon.resource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.testng.annotations.Test;

import com.hpcloud.mon.app.AlarmService;
import com.hpcloud.mon.app.command.CreateAlarmCommand;
import com.hpcloud.mon.app.command.UpdateAlarmCommand;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.domain.exception.EntityNotFoundException;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistory;
import com.hpcloud.mon.domain.model.alarmstatehistory.AlarmStateHistoryRepository;
import com.hpcloud.mon.domain.model.common.Link;
import com.hpcloud.mon.resource.exception.ErrorMessages;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

/**
 * @author Jonathan Halterman
 */
@Test
public class AlarmResourceTest extends AbstractMonApiResourceTest {
  private String expression;
  private Alarm alarm;
  private Alarm alarmItem;
  private AlarmService service;
  private AlarmRepository repo;
  private AlarmStateHistoryRepository stateHistoryRepo;
  private List<String> alarmActions;

  @Override
  @SuppressWarnings("unchecked")
  protected void setupResources() throws Exception {
    super.setupResources();

    expression = "avg(disk_read_ops{service=hpcs.compute, instance_id=937}) >= 90";
    alarmItem = new Alarm("123", "Disk Exceeds 1k Operations", null, expression, AlarmState.OK,
        true, null, null, null);
    alarmActions = new ArrayList<String>();
    alarmActions.add("29387234");
    alarmActions.add("77778687");
    alarm = new Alarm("123", "Disk Exceeds 1k Operations", null, expression, AlarmState.OK, true,
        alarmActions, null, null);

    service = mock(AlarmService.class);
    when(
        service.create(eq("abc"), eq("Disk Exceeds 1k Operations"), any(String.class),
            eq(expression), eq(AlarmExpression.of(expression)), any(List.class), any(List.class),
            any(List.class))).thenReturn(alarm);

    repo = mock(AlarmRepository.class);
    when(repo.findById(eq("abc"), eq("123"))).thenReturn(alarm);
    when(repo.find(anyString())).thenReturn(Arrays.asList(alarmItem));

    stateHistoryRepo = mock(AlarmStateHistoryRepository.class);

    addResources(new AlarmResource(service, repo, stateHistoryRepo));
  }

  @SuppressWarnings("unchecked")
  public void shouldCreate() {
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "Disk Exceeds 1k Operations", null, expression, alarmActions, null, null));

    assertEquals(response.getStatus(), 201);
    Alarm newAlarm = response.getEntity(Alarm.class);
    String location = response.getHeaders().get("Location").get(0);
    assertEquals(location, "/v2.0/alarms/" + newAlarm.getId());
    assertEquals(newAlarm, alarm);
    verify(service).create(eq("abc"), eq("Disk Exceeds 1k Operations"), any(String.class),
        eq(expression), eq(AlarmExpression.of(expression)), any(List.class), any(List.class),
        any(List.class));
  }

  public void shouldUpdate() {
    when(
        service.update(eq("abc"), eq("123"), any(AlarmExpression.class),
            any(UpdateAlarmCommand.class))).thenReturn(alarm);
    ClientResponse response = client().resource("/v2.0/alarms/123")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .put(
            ClientResponse.class,
            new UpdateAlarmCommand("Disk Exceeds 1k Operations", null, expression,
                AlarmState.ALARM, true, alarmActions, null, null));

    assertEquals(response.getStatus(), 200);
    verify(service).update(eq("abc"), eq("123"), any(AlarmExpression.class),
        any(UpdateAlarmCommand.class));
  }

  public void shouldErrorOnCreateWithInvalidMetricName() {
    String expression = "avg(foo{service=hpcs.compute, instance_id=937}) >= 90";
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "Disk Exceeds 1k Operations", null, expression, alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "foo is not a valid metric name for namespace hpcs.compute");
  }

  public void shouldErrorOnCreateWithInvalidDimensions() {
    String expression = "avg(disk_read_ops{service=hpcs.compute, instance_id=937, foo=bar}) >= 90";
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "Disk Exceeds 1k Operations", null, expression, alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "foo is not a valid dimension name for service hpcs.compute");
  }

  public void shouldErrorOnCreateWithDuplicateDimensions() {
    String expression = "avg(hpcs.compute{instance_id=937, instance_id=123, az=2, instance_uuid=abc123, metric_name=disk_read_ops}) >= 90";
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "Disk Exceeds 1k Operations", null, expression, alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "The alarm expression is invalid",
        "More than one value was given for dimension instance_id");
  }

  @SuppressWarnings("unchecked")
  public void shouldNotRequireDimensionsForCustomNamespace() {
    String expression = "avg(foo{metric_name=bar}) >= 90";
    when(
        service.create(eq("abc"), eq("Disk Exceeds 1k Operations"), any(String.class),
            eq(expression), eq(AlarmExpression.of(expression)), any(List.class), any(List.class),
            any(List.class))).thenReturn(alarm);
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "Disk Exceeds 1k Operations", null, expression, alarmActions, null, null));
    assertEquals(response.getStatus(), 201);
  }

  public void shouldErrorOnCreateWithInvalidJson() {
    ClientResponse response = createResponseFor("{\"alarmasdf\"::{\"name\":\"Disk Exceeds 1k Operations\"}}");

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("bad_request", 400,
        "Unable to process the provided JSON", "Unexpected character (':'");
  }

  public void shouldErrorOnCreateWithInvalidOperator() {
    String expression = "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops}) & 90";
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "Disk Exceeds 1k Operations", null, expression, alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "The alarm expression is invalid", "Syntax Error");
  }

  public void shouldErrorOnCreateWith0Period() {
    String expression = "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops},0) >= 90";
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "Disk Exceeds 1k Operations", null, expression, alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Period must not be 0");
  }

  public void shouldErrorOnCreateWithNonMod60Period() {
    String expression = "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops},61) >= 90";
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "Disk Exceeds 1k Operations", null, expression, alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Period 61 must be a multiple of 60");
  }

  public void shouldErrorOnCreateWithPeriodsLessThan1() {
    String expression = "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops}) >= 90 times 0";
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "Disk Exceeds 1k Operations", null, expression, alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Periods 0 must be greater than or equal to 1");
  }

  public void shouldErrorOnCreateWithPeriodTimesPeriodsGT2Weeks() {
    String expression = "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops},60) >= 90 times 20161";
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "Disk Exceeds 1k Operations", null, expression, alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class)).matches("unprocessable_entity", 422,
        "Period 60 times 20161 must total less than 2 weeks in seconds (1209600)");
  }

  public void shouldErrorOnCreateWithTooLongName() {
    String expression = "avg(hpcs.compute{instance_id=937, az=2, instance_uuid=0ff588fc-d298-482f-bb11-4b52d56801a4, metric_name=disk_read_ops}) >= 90";
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789"
            + "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789",
        null, expression, alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class))
        .matches(
            "unprocessable_entity",
            422,
            "Name 012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789 must be 255 characters or less");
  }

  public void shouldErrorOnCreateWithTooLongAlarmAction() {
    alarmActions = new ArrayList<String>();
    alarmActions.add("012345678901234567890123456789012345678901234567890");
    ClientResponse response = createResponseFor(new CreateAlarmCommand(
        "Disk Exceeds 1k Operations", null, expression, alarmActions, null, null));

    ErrorMessages.assertThat(response.getEntity(String.class))
        .matches("unprocessable_entity", 422,
            "Alarm action 012345678901234567890123456789012345678901234567890 must be 50 characters or less");
  }

  public void shouldList() {
    List<Alarm> alarms = client().resource("/v2.0/alarms")
        .header("X-Tenant-Id", "abc")
        .get(new GenericType<List<Alarm>>() {
        });

    assertEquals(alarms, Arrays.asList(alarmItem));
    verify(repo).find(eq("abc"));
  }

  public void shouldGet() {
    assertEquals(client().resource("/v2.0/alarms/123")
        .header("X-Tenant-Id", "abc")
        .get(Alarm.class), alarm);
    verify(repo).findById(eq("abc"), eq("123"));
  }

  public void should404OnGetInvalid() {
    doThrow(new EntityNotFoundException(null)).when(repo).findById(eq("abc"), eq("999"));

    try {
      client().resource("/v2.0/alarms/999").header("X-Tenant-Id", "abc").get(Alarm.class);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("404"));
    }
  }

  public void shouldDelete() {
    ClientResponse response = client().resource("/v2.0/alarms/123")
        .header("X-Tenant-Id", "abc")
        .delete(ClientResponse.class);
    assertEquals(response.getStatus(), 204);
    verify(service).delete(eq("abc"), eq("123"));
  }

  public void should404OnDeleteInvalid() {
    doThrow(new EntityNotFoundException(null)).when(service).delete(eq("abc"), eq("999"));

    try {
      client().resource("/v2.0/alarms/999").header("X-Tenant-Id", "abc").delete();
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("404"));
    }
  }

  public void should500OnInternalException() {
    doThrow(new RuntimeException("")).when(repo).find(anyString());

    try {
      client().resource("/v2.0/alarms").header("X-Tenant-Id", "abc").get(List.class);
      fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("500"));
    }
  }

  public void shouldHydateLinksOnList() {
    List<Link> expected = Arrays.asList(new Link("self", "/v2.0/alarms/123"), new Link("history",
        "/v2.0/alarms/123/history"));
    List<Link> links = client().resource("/v2.0/alarms")
        .header("X-Tenant-Id", "abc")
        .get(new GenericType<List<Alarm>>() {
        })
        .get(0)
        .getLinks();
    assertEquals(links, expected);
  }

  public void shouldHydateLinksOnGet() {
    List<Link> links = Arrays.asList(new Link("self", "/v2.0/alarms/123"), new Link("history",
        "/v2.0/alarms/123/history"));
    assertEquals(client().resource("/v2.0/alarms/123")
        .header("X-Tenant-Id", "abc")
        .get(Alarm.class)
        .getLinks(), links);
  }

  public void shouldGetAlarmStateHistory() {
    AlarmStateHistory history1 = new AlarmStateHistory("123", AlarmState.OK, AlarmState.ALARM,
        "foo", "foobar", System.currentTimeMillis() / 1000);
    AlarmStateHistory history2 = new AlarmStateHistory("123", AlarmState.ALARM, AlarmState.OK,
        "foo", "foobar", System.currentTimeMillis() / 1000);
    List<AlarmStateHistory> expected = Arrays.asList(history1, history2);

    when(stateHistoryRepo.findById(eq("abc"), eq("123"))).thenReturn(expected);

    assertEquals(client().resource("/v2.0/alarms/123/state-history")
        .header("X-Tenant-Id", "abc")
        .get(new GenericType<List<AlarmStateHistory>>() {
        }), expected);
  }

  private ClientResponse createResponseFor(Object request) {
    return client().resource("/v2.0/alarms")
        .header("X-Tenant-Id", "abc")
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .post(ClientResponse.class, request);
  }
}