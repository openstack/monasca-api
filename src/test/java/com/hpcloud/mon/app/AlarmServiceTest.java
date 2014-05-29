package com.hpcloud.mon.app;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
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
import com.hpcloud.mon.app.AlarmService.SubExpressions;
import com.hpcloud.mon.app.command.UpdateAlarmCommand;
import com.hpcloud.mon.common.model.alarm.AlarmExpression;
import com.hpcloud.mon.common.model.alarm.AlarmState;
import com.hpcloud.mon.common.model.alarm.AlarmSubExpression;
import com.hpcloud.mon.domain.model.alarm.Alarm;
import com.hpcloud.mon.domain.model.alarm.AlarmRepository;
import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethodRepository;

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

    when(
        repo.create(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            any(Map.class), any(List.class), any(List.class), any(List.class))).thenAnswer(
        new Answer<Alarm>() {
          @Override
          public Alarm answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            return new Alarm((String) args[0], (String) args[2], (String) args[3], (String) args[4],
                (String) args[5], AlarmState.UNDETERMINED, true, (List<String>) args[7],
                (List<String>) args[8], (List<String>) args[9]);
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

    Alarm alarm = service.create("bob", "90% CPU", "foo", "LOW", exprStr, AlarmExpression.of(exprStr),
        alarmActions, okActions, undeterminedActions);

    Alarm expected = new Alarm(alarm.getId(), "90% CPU", "foo", "LOW", exprStr, AlarmState.UNDETERMINED,
        true, alarmActions, okActions, undeterminedActions);
    assertEquals(expected, alarm);
    verify(repo).create(eq("bob"), anyString(), eq("90% CPU"), eq("foo"), eq("LOW"), eq(exprStr),
        any(Map.class), eq(alarmActions), eq(okActions), eq(undeterminedActions));
    verify(producer).send(any(KeyedMessage.class));
  }

  @SuppressWarnings("unchecked")
  public void shouldUpdate() {
    String exprStr = "avg(foo{instance_id=123}) > 90 or avg(bar{instance_id=777}) > 80";
    List<String> alarmActions = Arrays.asList("1", "2", "3");
    List<String> okActions = Arrays.asList("2", "3");
    List<String> undeterminedActions = Arrays.asList("3");

    Alarm oldAlarm = new Alarm("123", "foo bar", "foo bar", "LOW", exprStr, AlarmState.OK, true,
        alarmActions, okActions, undeterminedActions);
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
         "LOW", AlarmState.ALARM, false, newAlarmActions, newOkActions, newUndeterminedActions);

    Alarm alarm = service.update("bob", "123", AlarmExpression.of(newExprStr), command);

    Alarm expected = new Alarm(alarm.getId(), "foo bar baz", "foo bar baz", "LOW", newExprStr,
        AlarmState.ALARM, false, newAlarmActions, newOkActions, newUndeterminedActions);
    assertEquals(expected, alarm);
    verify(producer, times(2)).send(any(KeyedMessage.class));
  }

  public void testOldAndNewSubExpressionsFor() {
    Map<String, AlarmSubExpression> oldSubExpressions = new HashMap<>();
    oldSubExpressions.put("111", AlarmSubExpression.of("avg(foo{instance_id=123}) > 1"));
    oldSubExpressions.put("222", AlarmSubExpression.of("avg(foo{instance_id=456}) > 2"));
    oldSubExpressions.put("333", AlarmSubExpression.of("avg(foo{instance_id=789}) > 3"));
    when(repo.findSubExpressions(eq("123"))).thenReturn(oldSubExpressions);

    String newExprStr = "avg(foo{instance_id=123}) > 1 or avg(foo{instance_id=456}) <= 22 or avg(foo{instance_id=444}) > 4";
    AlarmExpression newExpr = AlarmExpression.of(newExprStr);

    SubExpressions expressions = service.subExpressionsFor("123", newExpr);

    // Assert old expressions
    assertEquals(expressions.oldAlarmSubExpressions,
        Collections.singletonMap("333", AlarmSubExpression.of("avg(foo{instance_id=789}) > 3")));

    // Assert changed expressions
    assertEquals(expressions.changedSubExpressions,
        Collections.singletonMap("222", AlarmSubExpression.of("avg(foo{instance_id=456}) <= 22")));

    // Assert unchanged expressions
    assertEquals(expressions.unchangedSubExpressions,
        Collections.singletonMap("111", AlarmSubExpression.of("avg(foo{instance_id=123}) > 1")));

    // Assert new expressions
    assertTrue(expressions.newAlarmSubExpressions.containsValue(AlarmSubExpression.of("avg(foo{instance_id=444}) > 4")));
  }
}
