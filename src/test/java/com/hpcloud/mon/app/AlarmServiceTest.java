package com.hpcloud.mon.app;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.app.command.UpdateAlarmCommand;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.domain.model.alarm.AlarmDetail;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;

/**
 * @author Jonathan Halterman
 */
@Test
public class AlarmServiceTest {
  AlarmService service;
  MonApiConfiguration config;
  Producer<String, String> producer;
  AlarmRepository repo;
  NotificationMethodRepository notificationMethodRepo;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  protected void beforeMethod() {
    config = new MonApiConfiguration();
    producer = mock(Producer.class);
    repo = mock(AlarmRepository.class);
    notificationMethodRepo = mock(NotificationMethodRepository.class);
    service = new AlarmService(config, producer, repo, notificationMethodRepo);

    // Mock answers
    when(
        repo.create(anyString(), anyString(), anyString(), anyString(), anyString(),
            any(Map.class), any(List.class), any(List.class), any(List.class))).thenAnswer(
        new Answer<AlarmDetail>() {
          @Override
          public AlarmDetail answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            return new AlarmDetail((String) args[0], (String) args[2], (String) args[3],
                (String) args[4], AlarmState.UNDETERMINED, true, (List<String>) args[6],
                (List<String>) args[7], (List<String>) args[8]);
          }
        });
    when(
        repo.update(anyString(), anyString(), anyString(), anyString(), anyString(),
            any(AlarmState.class), anyBoolean(), any(List.class), any(Map.class), any(List.class),
            any(List.class), any(List.class))).thenAnswer(new Answer<AlarmDetail>() {
      @Override
      public AlarmDetail answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return new AlarmDetail((String) args[0], (String) args[2], (String) args[3],
            (String) args[4], (AlarmState) args[5], (Boolean) args[6], (List<String>) args[9],
            (List<String>) args[10], (List<String>) args[11]);
      }
    });
  }

  @SuppressWarnings("unchecked")
  public void shouldCreate() {
    String exprStr = "avg(cpu_utilization{service=hpcs.compute, instance_id=123}) > 90";
    List<String> alarmActions = Arrays.asList("1", "2", "3");
    List<String> okActions = Arrays.asList("2", "3");
    List<String> undeterminedActions = Arrays.asList("3");

    when(notificationMethodRepo.exists(eq("bob"), anyString())).thenReturn(true);

    AlarmDetail alarm = service.create("bob", "90% CPU", "foo", exprStr,
        AlarmExpression.of(exprStr), alarmActions, okActions, undeterminedActions);

    AlarmDetail expected = new AlarmDetail(alarm.getId(), "90% CPU", "foo", exprStr,
        AlarmState.UNDETERMINED, true, alarmActions, okActions, undeterminedActions);
    assertEquals(expected, alarm);
    verify(repo).create(anyString(), eq("bob"), eq("90% CPU"), eq("foo"), eq(exprStr),
        any(Map.class), eq(alarmActions), eq(okActions), eq(undeterminedActions));
    verify(producer).send(any(KeyedMessage.class));
  }

  @SuppressWarnings("unchecked")
  public void shouldUpdateForCommand() {
    String exprStr = "avg(foo{instance_id=123}) > 90 or avg(bar{instance_id=777}) > 80";
    List<String> alarmActions = Arrays.asList("1", "2", "3");
    List<String> okActions = Arrays.asList("2", "3");
    List<String> undeterminedActions = Arrays.asList("3");

    AlarmDetail oldAlarm = new AlarmDetail("123", "foo bar", "foo bar", exprStr, AlarmState.OK,
        true, alarmActions, okActions, undeterminedActions);
    Map<String, AlarmSubExpression> oldSubExpressions = new HashMap<>();
    oldSubExpressions.put("444", AlarmSubExpression.of("avg(foo{instance_id=123}) > 90"));
    oldSubExpressions.put("555", AlarmSubExpression.of("avg(bar{instance_id=777}) > 80"));

    when(repo.findById(eq("bob"), eq("123"))).thenReturn(oldAlarm);
    when(repo.findSubExpressions(eq("123"))).thenReturn(oldSubExpressions);
    when(notificationMethodRepo.exists(eq("bob"), anyString())).thenReturn(true);

    String newExprStr = "avg(foo{instance_id=123}) > 90 or avg(bar{instance_id=xxxx}) > 10 or avg(baz{instance_id=654}) > 123";
    List<String> newAlarmActions = Arrays.asList("5", "6", "7");
    List<String> newOkActions = Arrays.asList("6", "7");
    List<String> newUndeterminedActions = Arrays.asList("7");
    UpdateAlarmCommand command = new UpdateAlarmCommand("foo bar baz", "foo bar baz", newExprStr,
        AlarmState.ALARM, false, newAlarmActions, newOkActions, newUndeterminedActions);

    AlarmDetail alarm = service.update("bob", "123", AlarmExpression.of(newExprStr), command);

    AlarmDetail expected = new AlarmDetail(alarm.getId(), "foo bar baz", "foo bar baz", newExprStr,
        AlarmState.ALARM, false, newAlarmActions, newOkActions, newUndeterminedActions);
    assertEquals(expected, alarm);
    verify(producer).send(any(KeyedMessage.class));
  }
}
