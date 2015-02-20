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

package monasca.api.app;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import monasca.api.ApiConfig;
import monasca.api.app.AlarmDefinitionService.SubExpressions;
import monasca.api.app.command.UpdateAlarmDefinitionCommand;
import monasca.common.model.alarm.AlarmExpression;
import monasca.common.model.alarm.AlarmSubExpression;
import monasca.common.model.event.AlarmDefinitionUpdatedEvent;
import monasca.common.util.Serialization;
import monasca.api.domain.model.alarm.AlarmRepo;
import monasca.api.domain.model.alarmdefinition.AlarmDefinition;
import monasca.api.domain.model.alarmdefinition.AlarmDefinitionRepo;
import monasca.api.domain.model.notificationmethod.NotificationMethodRepo;
import monasca.api.domain.exception.EntityExistsException;

@Test
public class AlarmDefinitionServiceTest {
  private static final String EXPR2 = "avg(bar{instance_id=777}) > 80";

  private static final String EXPR1 = "avg(foo{instance_id=123}) > 90";

  final static String TENANT_ID = "bob";

  AlarmDefinitionService service;
  ApiConfig config;
  Producer<String, String> producer;
  AlarmDefinitionRepo repo;
  NotificationMethodRepo notificationMethodRepo;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  protected void beforeMethod() {
    config = new ApiConfig();
    producer = mock(Producer.class);
    repo = mock(AlarmDefinitionRepo.class);
    notificationMethodRepo = mock(NotificationMethodRepo.class);
    AlarmRepo alarmRepo = mock(AlarmRepo.class);
    service = new AlarmDefinitionService(config, producer, repo, alarmRepo, notificationMethodRepo);

    when(
        repo.create(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            any(Map.class), any(List.class), any(List.class), any(List.class), any(List.class)))
        .thenAnswer(new Answer<AlarmDefinition>() {
          @Override
          public AlarmDefinition answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            return new AlarmDefinition((String) args[0], (String) args[2], (String) args[3],
                (String) args[4], (String) args[5], (List<String>) args[7], true,
                (List<String>) args[8], (List<String>) args[9], (List<String>) args[10]);
          }
        });
  }

  @SuppressWarnings("unchecked")
  public void shouldCreate() {
    String exprStr = "avg(cpu_utilization{service=hpcs.compute, instance_id=123}) > 90";
    List<String> matchBy = Arrays.asList("service", "instance_id");
    List<String> alarmActions = Arrays.asList("1", "2", "3");
    List<String> okActions = Arrays.asList("2", "3");
    List<String> undeterminedActions = Arrays.asList("3");

    when(notificationMethodRepo.exists(eq(TENANT_ID), anyString())).thenReturn(true);

    AlarmDefinition alarm =
        service.create(TENANT_ID, "90% CPU", "foo", "LOW", exprStr, AlarmExpression.of(exprStr),
            matchBy, alarmActions, okActions, undeterminedActions);

    AlarmDefinition expected =
        new AlarmDefinition(alarm.getId(), "90% CPU", "foo", "LOW", exprStr, matchBy, true,
            alarmActions, okActions, undeterminedActions);
    assertEquals(expected, alarm);
    verify(repo).create(eq(TENANT_ID), anyString(), eq("90% CPU"), eq("foo"), eq("LOW"), eq(exprStr),
        any(Map.class), eq(matchBy), eq(alarmActions), eq(okActions), eq(undeterminedActions));
    verify(producer).send(any(KeyedMessage.class));
  }

  public void updateFailsDueToMatchBy() {
    final List<String> matchBy = Arrays.asList("hostname", "service");
    final AlarmDefinition oldAlarmDef = setupInitialAlarmDefinition(matchBy);

    final List<String> newMatchBy = Arrays.asList("service");
    verifyChangeFails(oldAlarmDef.getId(), oldAlarmDef.getName(), oldAlarmDef.getDescription(),
        oldAlarmDef.getExpression(), newMatchBy, oldAlarmDef.getSeverity(), oldAlarmDef.isActionsEnabled(),
        oldAlarmDef.getAlarmActions(), oldAlarmDef.getOkActions(),
        oldAlarmDef.getUndeterminedActions(), "match_by");
  }

  public void changeFailsDueToDeletedExpression() {
    final List<String> matchBy = Arrays.asList("hostname", "service");
    final AlarmDefinition oldAlarmDef = setupInitialAlarmDefinition(matchBy);

    verifyChangeFails(oldAlarmDef.getId(), oldAlarmDef.getName(), oldAlarmDef.getDescription(),
        EXPR1, matchBy, oldAlarmDef.getSeverity(), oldAlarmDef.isActionsEnabled(),
        oldAlarmDef.getAlarmActions(), oldAlarmDef.getOkActions(),
        oldAlarmDef.getUndeterminedActions(), "subexpressions");
  }

  public void changeFailsDueToAddedExpression() {
    final List<String> matchBy = Arrays.asList("hostname", "service");
    final AlarmDefinition oldAlarmDef = setupInitialAlarmDefinition(matchBy);

    final String newExpression = EXPR1 + " or " + EXPR2 + " or " + "min(cpu.idle) < 10";
    verifyChangeFails(oldAlarmDef.getId(), oldAlarmDef.getName(), oldAlarmDef.getDescription(),
        newExpression, matchBy, oldAlarmDef.getSeverity(), oldAlarmDef.isActionsEnabled(),
        oldAlarmDef.getAlarmActions(), oldAlarmDef.getOkActions(),
        oldAlarmDef.getUndeterminedActions(), "subexpressions");
  }

  public void changeFailsDueToChangedMetricName() {
    final List<String> matchBy = Arrays.asList("hostname", "service");
    final AlarmDefinition oldAlarmDef = setupInitialAlarmDefinition(matchBy);

    final String newExpression = EXPR1 + " or " + EXPR2.replace("bar", "barz");
    verifyChangeFails(oldAlarmDef.getId(), oldAlarmDef.getName(), oldAlarmDef.getDescription(),
        newExpression, matchBy, oldAlarmDef.getSeverity(), oldAlarmDef.isActionsEnabled(),
        oldAlarmDef.getAlarmActions(), oldAlarmDef.getOkActions(),
        oldAlarmDef.getUndeterminedActions(), "metric");
  }

  public void changeFailsDueToChangedMetricDimension() {
    final List<String> matchBy = Arrays.asList("hostname", "service");
    final AlarmDefinition oldAlarmDef = setupInitialAlarmDefinition(matchBy);

    final String newExpression = EXPR1 + " or " + EXPR2.replace("777", "888");
    verifyChangeFails(oldAlarmDef.getId(), oldAlarmDef.getName(), oldAlarmDef.getDescription(),
        newExpression, matchBy, oldAlarmDef.getSeverity(), oldAlarmDef.isActionsEnabled(),
        oldAlarmDef.getAlarmActions(), oldAlarmDef.getOkActions(),
        oldAlarmDef.getUndeterminedActions(), "metric");
  }

  private void verifyChangeFails(final String id, String name, String description,
      final String expression, List<String> matchBy, String severity, boolean actionsEnabled, List<String> alarmActions,
      List<String> okActions, List<String> undeterminedActions, String expected) {
    UpdateAlarmDefinitionCommand command =
        new UpdateAlarmDefinitionCommand(name, description, expression, matchBy, severity,
            actionsEnabled, alarmActions, okActions, undeterminedActions);
    try {
      service.update(TENANT_ID, id, AlarmExpression.of(command.expression), command);
      fail("Update of AlarmDefinition succeeded when it should have failed");
    }
    catch(WebApplicationException e) {
      assertEquals(e.getResponse().getStatus(), 422);
      assertTrue(e.getResponse().getEntity().toString().contains(expected));
    }

    try {
      service.patch(TENANT_ID, id, name, description, severity, expression,
          AlarmExpression.of(expression), matchBy, actionsEnabled, alarmActions, okActions,
          undeterminedActions);
      fail("Patch of AlarmDefinition succeeded when it should have failed");
    }
    catch(WebApplicationException e) {
      assertEquals(e.getResponse().getStatus(), 422);
      assertTrue(e.getResponse().getEntity().toString().contains(expected));
    }
  }

  public void shouldChange() {
    final List<String> matchBy = Arrays.asList("hostname", "service");
    final AlarmDefinition oldAlarmDef = setupInitialAlarmDefinition(matchBy);

    final String newExprStr = oldAlarmDef.getExpression().replace("90", "75").replace(" or ", " and ");
    final List<String> newAlarmActions = Arrays.asList("5", "6", "7");
    final List<String> newOkActions = Arrays.asList("6", "7");
    final List<String> newUndeterminedActions = Arrays.asList("7");
    final String newSeverity = "HIGH";
    final String newName = "foo bar baz";
    final String newDescription = "foo bar baz";
    final boolean newEnabled = false;
    UpdateAlarmDefinitionCommand command =
        new UpdateAlarmDefinitionCommand(newName, newDescription, newExprStr, matchBy, newSeverity,
            newEnabled, newAlarmActions, newOkActions, newUndeterminedActions);

    final AlarmDefinition expected =
        new AlarmDefinition(oldAlarmDef.getId(), newName, newDescription, newSeverity, newExprStr, matchBy,
            newEnabled, newAlarmActions, newOkActions, newUndeterminedActions);

    final AlarmDefinition updatedAlarmDef =
        service.update(TENANT_ID, oldAlarmDef.getId(), AlarmExpression.of(command.expression),
            command);
    assertEquals(updatedAlarmDef, expected);

    final AlarmDefinition patchedAlarmDef =
        service.patch(TENANT_ID, oldAlarmDef.getId(), newName, newDescription, newSeverity,
            newExprStr, AlarmExpression.of(newExprStr), matchBy, newEnabled, newAlarmActions,
            newOkActions, newUndeterminedActions);
    assertEquals(patchedAlarmDef, expected);

    final Map<String, AlarmSubExpression> emptyMap = new HashMap<>();
    final Map<String, AlarmSubExpression> changedSubExpressions = new HashMap<>();
    final Map<String, AlarmSubExpression> unchangedSubExpressions = new HashMap<>();
    changedSubExpressions.put("444", AlarmSubExpression.of("avg(foo{instance_id=123}) > 75"));
    unchangedSubExpressions.put("555", AlarmSubExpression.of(EXPR2));
    final AlarmDefinitionUpdatedEvent event =
        new AlarmDefinitionUpdatedEvent(TENANT_ID, oldAlarmDef.getId(), newName, newDescription,
            newExprStr, matchBy, newEnabled, newSeverity, emptyMap, changedSubExpressions,
            unchangedSubExpressions, emptyMap);
    verify(producer).send(
        new KeyedMessage<String, String>(config.eventsTopic,
            String.valueOf(service.eventCount - 1), Serialization.toJson(event)));
  }

  public void shouldPatchExpression() {
    final List<String> matchBy = Arrays.asList("hostname", "service");
    final AlarmDefinition oldAlarmDef = setupInitialAlarmDefinition(matchBy);

    final String newExprStr = oldAlarmDef.getExpression().replace("90", "75").replace(" or ", " and ");
    assertNotEquals(newExprStr, oldAlarmDef.getExpression());

    final Map<String, AlarmSubExpression> changedSubExpressions = new HashMap<>();
    final Map<String, AlarmSubExpression> unchangedSubExpressions = new HashMap<>();
    changedSubExpressions.put("444", AlarmSubExpression.of(EXPR1.replace("90", "75")));
    unchangedSubExpressions.put("555", AlarmSubExpression.of(EXPR2));

    patchExpression(newExprStr, oldAlarmDef, changedSubExpressions, unchangedSubExpressions);

    final String newExprStr2 = EXPR2.replace("avg", "min") + " and " + EXPR1.replace("avg", "max");
    assertNotEquals(newExprStr2, oldAlarmDef.getExpression());
    changedSubExpressions.clear();
    unchangedSubExpressions.clear();
    changedSubExpressions.put("444", AlarmSubExpression.of(EXPR1.replace("avg", "max")));
    changedSubExpressions.put("555", AlarmSubExpression.of(EXPR2.replace("avg", "min")));

    patchExpression(newExprStr2, oldAlarmDef, changedSubExpressions, unchangedSubExpressions);
  }

  private void patchExpression(final String newExprStr, final AlarmDefinition oldAlarmDef,
      final Map<String, AlarmSubExpression> changedSubExpressions,
      final Map<String, AlarmSubExpression> unchangedSubExpressions) {
    BiMap<String, AlarmSubExpression> oldExpressions =
        HashBiMap.create(new HashMap<String, AlarmSubExpression>());
    final Set<String> oldSubAlarmIds = oldExpressions.keySet();
    Map<String, AlarmSubExpression> newSubAlarms = new HashMap<>();
    final AlarmDefinition patchedAlarmDef =
        service.patch(TENANT_ID, oldAlarmDef.getId(), null, null, null,
            newExprStr, AlarmExpression.of(newExprStr), null, null, null,
            null, null);

    final AlarmDefinition expected =
        new AlarmDefinition(oldAlarmDef.getId(), oldAlarmDef.getName(), oldAlarmDef.getDescription(),
            oldAlarmDef.getSeverity(), newExprStr, oldAlarmDef.getMatchBy(),
            oldAlarmDef.isActionsEnabled(), oldAlarmDef.getAlarmActions(),
            oldAlarmDef.getOkActions(), oldAlarmDef.getUndeterminedActions());
    assertEquals(patchedAlarmDef, expected);

    final Map<String, AlarmSubExpression> emptyMap = new HashMap<>();
    final AlarmDefinitionUpdatedEvent event =
        new AlarmDefinitionUpdatedEvent(TENANT_ID, oldAlarmDef.getId(), oldAlarmDef.getName(),
            oldAlarmDef.getDescription(), newExprStr, oldAlarmDef.getMatchBy(),
            oldAlarmDef.isActionsEnabled(), oldAlarmDef.getSeverity(), emptyMap,
            changedSubExpressions, unchangedSubExpressions, emptyMap);
    verify(producer).send(
        new KeyedMessage<String, String>(config.eventsTopic,
            String.valueOf(service.eventCount - 1), Serialization.toJson(event)));
    verify(repo).update(TENANT_ID, oldAlarmDef.getId(), true, oldAlarmDef.getName(),
        oldAlarmDef.getDescription(), newExprStr, oldAlarmDef.getMatchBy(),
        oldAlarmDef.getSeverity(), oldAlarmDef.isActionsEnabled(), oldSubAlarmIds,
        changedSubExpressions, newSubAlarms, null, null, null);
  }

  public void shouldPatchIndividual() {
    final List<String> matchBy = Arrays.asList("hostname", "service");
    final AlarmDefinition oldAlarmDef = setupInitialAlarmDefinition(matchBy);

    final List<String> newAlarmActions = Arrays.asList("5", "6", "7");
    final List<String> newOkActions = Arrays.asList("6", "7");
    final List<String> newUndeterminedActions = Arrays.asList("7");
    final String newSeverity = "HIGH";
    final String newName = "foo bar baz";
    final String newDescription = "foo bar baz";
    final boolean newEnabled = false;

    doPatch(matchBy, oldAlarmDef, newName, newName, null, oldAlarmDef.getDescription(), null,
        oldAlarmDef.getSeverity(), null, oldAlarmDef.isActionsEnabled(), null,
        oldAlarmDef.getAlarmActions(), null,
        oldAlarmDef.getOkActions(), null,
        oldAlarmDef.getUndeterminedActions());

    doPatch(matchBy, oldAlarmDef, null, oldAlarmDef.getName(), newDescription, newDescription,
        null, oldAlarmDef.getSeverity(), null, oldAlarmDef.isActionsEnabled(), null,
        oldAlarmDef.getAlarmActions(), null,
        oldAlarmDef.getOkActions(), null,
        oldAlarmDef.getUndeterminedActions());

    doPatch(matchBy, oldAlarmDef, null, oldAlarmDef.getName(), null, oldAlarmDef.getDescription(),
        newSeverity, newSeverity, null, oldAlarmDef.isActionsEnabled(), null,
        oldAlarmDef.getAlarmActions(), null,
        oldAlarmDef.getOkActions(), null,
        oldAlarmDef.getUndeterminedActions());

    doPatch(matchBy, oldAlarmDef, null, oldAlarmDef.getName(), null, oldAlarmDef.getDescription(),
        null, oldAlarmDef.getSeverity(), newEnabled, newEnabled, null,
        oldAlarmDef.getAlarmActions(), null, oldAlarmDef.getOkActions(), null,
        oldAlarmDef.getUndeterminedActions());

    doPatch(matchBy, oldAlarmDef, null, oldAlarmDef.getName(), null, oldAlarmDef.getDescription(),
        null, oldAlarmDef.getSeverity(), null, oldAlarmDef.isActionsEnabled(),
        newAlarmActions, newAlarmActions,
        null, oldAlarmDef.getOkActions(), null,
        oldAlarmDef.getUndeterminedActions());

    doPatch(matchBy, oldAlarmDef, null, oldAlarmDef.getName(), null, oldAlarmDef.getDescription(),
        null, oldAlarmDef.getSeverity(), newEnabled, newEnabled, null,
        oldAlarmDef.getAlarmActions(), newOkActions, newOkActions, null,
        oldAlarmDef.getUndeterminedActions());

    doPatch(matchBy, oldAlarmDef, null, oldAlarmDef.getName(), null, oldAlarmDef.getDescription(),
        null, oldAlarmDef.getSeverity(), newEnabled, newEnabled, null,
        oldAlarmDef.getAlarmActions(), null, oldAlarmDef.getOkActions(), newUndeterminedActions,
        newUndeterminedActions);

    final List<String> emptyActionList = new ArrayList<>();
    doPatch(matchBy, oldAlarmDef, null, oldAlarmDef.getName(), null, oldAlarmDef.getDescription(),
        null, oldAlarmDef.getSeverity(), newEnabled, newEnabled, emptyActionList,
        emptyActionList, null, oldAlarmDef.getOkActions(), null,
        oldAlarmDef.getUndeterminedActions());

    doPatch(matchBy, oldAlarmDef, null, oldAlarmDef.getName(), null, oldAlarmDef.getDescription(),
        null, oldAlarmDef.getSeverity(), newEnabled, newEnabled, null,
        oldAlarmDef.getAlarmActions(), emptyActionList, emptyActionList, null,
        oldAlarmDef.getUndeterminedActions());

    doPatch(matchBy, oldAlarmDef, null, oldAlarmDef.getName(), null, oldAlarmDef.getDescription(),
        null, oldAlarmDef.getSeverity(), newEnabled, newEnabled, null,
        oldAlarmDef.getAlarmActions(), null, oldAlarmDef.getOkActions(), emptyActionList,
        emptyActionList);
  }

  private void doPatch(final List<String> matchBy, final AlarmDefinition oldAlarmDef,
      final String newName, final String expectedName, String newDescription,
      String expectedDescription, final String newSeverity, final String expectedSeverity,
      final Boolean actionsEnabled, final boolean expectedActionsEnabled,
      final List<String> newAlarmActions, final List<String> expectedAlarmActions,
      final List<String> newOkActions, final List<String> expectedOkActions,
      final List<String> newUndeterminedActions, final List<String> expectedUndeterminedActions) {
    final Map<String, AlarmSubExpression> emptyMap = new HashMap<>();
    final Map<String, AlarmSubExpression> changedSubExpressions = new HashMap<>();
    final Map<String, AlarmSubExpression> unchangedSubExpressions = new HashMap<>();
    unchangedSubExpressions.put("444", AlarmSubExpression.of(EXPR1));
    unchangedSubExpressions.put("555", AlarmSubExpression.of(EXPR2));

    BiMap<String, AlarmSubExpression> oldExpressions =
        HashBiMap.create(new HashMap<String, AlarmSubExpression>());
    final Set<String> oldSubAlarmIds = oldExpressions.keySet();
    Map<String, AlarmSubExpression> changedSubAlarms = new HashMap<>();
    Map<String, AlarmSubExpression> newSubAlarms = new HashMap<>();
    final AlarmDefinition patchedAlarmDef =
        service.patch(TENANT_ID, oldAlarmDef.getId(), newName, newDescription, newSeverity,
            null, null, null, actionsEnabled, newAlarmActions,
            newOkActions, newUndeterminedActions);

    final AlarmDefinition expected =
        new AlarmDefinition(oldAlarmDef.getId(), expectedName, expectedDescription,
            expectedSeverity, oldAlarmDef.getExpression(), matchBy,
            expectedActionsEnabled, expectedAlarmActions,
            expectedOkActions, expectedUndeterminedActions);
    assertEquals(patchedAlarmDef, expected);

    final AlarmDefinitionUpdatedEvent event =
        new AlarmDefinitionUpdatedEvent(TENANT_ID, oldAlarmDef.getId(), expectedName,
            expectedDescription, oldAlarmDef.getExpression(), matchBy,
            expectedActionsEnabled, expectedSeverity, emptyMap,
            changedSubExpressions, unchangedSubExpressions, emptyMap);
    verify(producer).send(
        new KeyedMessage<String, String>(config.eventsTopic,
            String.valueOf(service.eventCount - 1), Serialization.toJson(event)));
    verify(repo).update(TENANT_ID, oldAlarmDef.getId(), true, expectedName,
        expectedDescription, oldAlarmDef.getExpression(), matchBy,
        expectedSeverity, expectedActionsEnabled, oldSubAlarmIds,
        changedSubAlarms, newSubAlarms, newAlarmActions, newOkActions, newUndeterminedActions);
  }

  private AlarmDefinition setupInitialAlarmDefinition(final List<String> matchBy) {
    final String alarmDefId = "123";
    String exprStr = EXPR1 + " or " + EXPR2;
    List<String> alarmActions = Arrays.asList("1", "2", "3");
    List<String> okActions = Arrays.asList("2", "3");
    List<String> undeterminedActions = Arrays.asList("3");
    AlarmDefinition oldAlarmDef =
        new AlarmDefinition(alarmDefId, "foo bar", "foo bar", "LOW", exprStr, matchBy, true, alarmActions,
            okActions, undeterminedActions);
    Map<String, AlarmSubExpression> oldSubExpressions = new HashMap<>();
    oldSubExpressions.put("444", AlarmSubExpression.of(EXPR1));
    oldSubExpressions.put("555", AlarmSubExpression.of(EXPR2));

    when(repo.findById(eq(TENANT_ID), eq(alarmDefId))).thenReturn(oldAlarmDef);
    when(repo.findSubExpressions(eq(alarmDefId))).thenReturn(oldSubExpressions);
    when(notificationMethodRepo.exists(eq(TENANT_ID), anyString())).thenReturn(true);
    return oldAlarmDef;
  }

  public void testOldAndNewSubExpressionsFor() {
    Map<String, AlarmSubExpression> oldSubExpressions = new HashMap<>();
    oldSubExpressions.put("111", AlarmSubExpression.of("avg(foo{instance_id=123}) > 1"));
    oldSubExpressions.put("222", AlarmSubExpression.of("avg(foo{instance_id=456}) > 2"));
    oldSubExpressions.put("333", AlarmSubExpression.of("avg(foo{instance_id=789}) > 3"));

    String newExprStr =
        "avg(foo{instance_id=123}) > 1 or avg(foo{instance_id=456}) <= 22 or avg(foo{instance_id=444}) > 4";
    AlarmExpression newExpr = AlarmExpression.of(newExprStr);

    SubExpressions expressions = service.subExpressionsFor(oldSubExpressions, newExpr);

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
    assertTrue(expressions.newAlarmSubExpressions.containsValue(AlarmSubExpression
        .of("avg(foo{instance_id=444}) > 4")));
  }

  public void testSubExpressionsForUnchanged() {
    Map<String, AlarmSubExpression> oldSubExpressions = new HashMap<>();
    final String expr1 = "avg(foo{instance_id=123}) > 1";
    oldSubExpressions.put("111", AlarmSubExpression.of(expr1));
    final String expr2 = "avg(foo{instance_id=123}) < 4";
    oldSubExpressions.put("222", AlarmSubExpression.of(expr2));

    String newExprStr = expr2 + " and " + expr1;
    AlarmExpression newExpr = AlarmExpression.of(newExprStr);

    SubExpressions expressions = service.subExpressionsFor(oldSubExpressions, newExpr);

    // Assert old expressions
    assertTrue(expressions.oldAlarmSubExpressions.isEmpty());

    // Assert changed expressions
    assertTrue(expressions.changedSubExpressions.isEmpty());

    // Assert unchanged expressions
    assertEquals(expressions.unchangedSubExpressions.size(), 2);
    assertEquals(expressions.unchangedSubExpressions.get("111"), AlarmSubExpression.of(expr1));
    assertEquals(expressions.unchangedSubExpressions.get("222"), AlarmSubExpression.of(expr2));

    // Assert new expressions
    assertTrue(expressions.newAlarmSubExpressions.isEmpty());
  }

  @Test(expectedExceptions = EntityExistsException.class)
  public void testPatchSameName() {
    String exprStr = EXPR1 + " or " + EXPR2;
    List<String> alarmActions = Arrays.asList("1", "2", "3");
    List<String> okActions = Arrays.asList("2", "3");
    List<String> undeterminedActions = Arrays.asList("3");
    List<String> matchBy = Arrays.asList("service", "instance_id");
    AlarmDefinition firstAlarmDef =
        new AlarmDefinition("123", "91% CPU", "description1", "LOW", exprStr, matchBy, true, alarmActions,
            okActions, undeterminedActions);

    AlarmDefinition secondAlarmDef =
        new AlarmDefinition("234", "92% CPU", "description2", "LOW", exprStr, matchBy, true, alarmActions,
            okActions, undeterminedActions);

    when(repo.findById(TENANT_ID, secondAlarmDef.getId())).thenReturn(secondAlarmDef);
    when(repo.findById(TENANT_ID, firstAlarmDef.getId())).thenReturn(firstAlarmDef);
    when(repo.exists(TENANT_ID, "91% CPU")).thenReturn("123");
    when(notificationMethodRepo.exists(eq(TENANT_ID), anyString())).thenReturn(true);
    service.patch(TENANT_ID, secondAlarmDef.getId(), firstAlarmDef.getName(), "foo", "LOW", exprStr, null,
        matchBy, true, alarmActions, okActions, undeterminedActions);

  }

  public void testPatchExistingAlarmName() {
    String exprStr = EXPR1 + " or " + EXPR2;
    List<String> alarmActions = Arrays.asList("1", "2", "3");
    List<String> okActions = Arrays.asList("2", "3");
    List<String> undeterminedActions = Arrays.asList("3");
    List<String> matchBy = Arrays.asList("service", "instance_id");
    AlarmDefinition firstAlarmDef =
        new AlarmDefinition("123", "91% CPU", "description1", "LOW", exprStr, matchBy, true, alarmActions,
            okActions, undeterminedActions);

    AlarmDefinition secondAlarmDef =
        new AlarmDefinition("234", "92% CPU", "description2", "LOW", exprStr, matchBy, true, alarmActions,
            okActions, undeterminedActions);

    when(repo.findById(TENANT_ID, secondAlarmDef.getId())).thenReturn(secondAlarmDef);
    when(repo.findById(TENANT_ID, firstAlarmDef.getId())).thenReturn(firstAlarmDef);
    when(repo.exists(TENANT_ID, "92% CPU")).thenReturn(secondAlarmDef.getId());

    Map<String, AlarmSubExpression> oldSubExpressions = new HashMap<>();
    oldSubExpressions.put("444", AlarmSubExpression.of(EXPR1));
    oldSubExpressions.put("555", AlarmSubExpression.of(EXPR2));
    when(repo.findSubExpressions(eq("234"))).thenReturn(oldSubExpressions);

    when(notificationMethodRepo.exists(eq(TENANT_ID), anyString())).thenReturn(true);
    AlarmDefinition alarmPatched = service.patch(TENANT_ID, secondAlarmDef.getId(), "92% CPU", "foo", "LOW", exprStr, null,
        matchBy, true, alarmActions, okActions, undeterminedActions);
    assertEquals(alarmPatched.getName(), "92% CPU");

  }

  public void testUpdateExistingAlarmName() {
    String exprStr = EXPR1 + " or " + EXPR2;
    List<String> alarmActions = Arrays.asList("1", "2", "3");
    List<String> okActions = Arrays.asList("2", "3");
    List<String> undeterminedActions = Arrays.asList("3");
    List<String> matchBy = Arrays.asList("service", "instance_id");
    AlarmDefinition firstAlarmDef =
        new AlarmDefinition("123", "91% CPU", "description1", "LOW", exprStr, matchBy, true,
            alarmActions, okActions, undeterminedActions);

    AlarmDefinition secondAlarmDef =
        new AlarmDefinition("234", "92% CPU", "description2", "LOW", exprStr, matchBy, true,
            alarmActions, okActions, undeterminedActions);

    UpdateAlarmDefinitionCommand updateCommand =
        new UpdateAlarmDefinitionCommand("92% CPU", "Description1", exprStr, matchBy, "LOW", true,
            alarmActions, okActions, undeterminedActions);

    when(repo.findById(TENANT_ID, secondAlarmDef.getId())).thenReturn(secondAlarmDef);
    when(repo.findById(TENANT_ID, firstAlarmDef.getId())).thenReturn(firstAlarmDef);
    when(repo.exists(TENANT_ID, "92% CPU")).thenReturn(secondAlarmDef.getId());

    Map<String, AlarmSubExpression> oldSubExpressions = new HashMap<>();
    oldSubExpressions.put("444", AlarmSubExpression.of(EXPR1));
    oldSubExpressions.put("555", AlarmSubExpression.of(EXPR2));
    when(repo.findSubExpressions(eq("234"))).thenReturn(oldSubExpressions);
    AlarmExpression alarmExpression = new AlarmExpression(exprStr);

    when(notificationMethodRepo.exists(eq(TENANT_ID), anyString())).thenReturn(true);
    AlarmDefinition alarmPatched =
        service.update(TENANT_ID, secondAlarmDef.getId(), alarmExpression, updateCommand);
    assertEquals(alarmPatched.getName(), "92% CPU");
  }

  @Test(expectedExceptions = EntityExistsException.class)
  public void testUpdateSameName() {
    String exprStr = EXPR1 + " or " + EXPR2;
    List<String> alarmActions = Arrays.asList("1", "2", "3");
    List<String> okActions = Arrays.asList("2", "3");
    List<String> undeterminedActions = Arrays.asList("3");
    List<String> matchBy = Arrays.asList("service", "instance_id");

    AlarmDefinition firstAlarmDef =
        new AlarmDefinition("123", "91% CPU", "description1", "LOW", exprStr, matchBy, true, alarmActions,
            okActions, undeterminedActions);

    AlarmDefinition secondAlarmDef =
        new AlarmDefinition("234", "92% CPU", "description2", "LOW", exprStr, matchBy, true, alarmActions,
            okActions, undeterminedActions);

    UpdateAlarmDefinitionCommand updateCommand = new UpdateAlarmDefinitionCommand(
            "91% CPU", "Description1", exprStr, matchBy, "LOW", true,
            alarmActions, okActions, undeterminedActions);

    when(repo.findById(TENANT_ID, secondAlarmDef.getId())).thenReturn(secondAlarmDef);
    when(repo.findById(TENANT_ID, firstAlarmDef.getId())).thenReturn(firstAlarmDef);
    when(repo.exists(TENANT_ID, "91% CPU")).thenReturn("123");
    when(notificationMethodRepo.exists(eq(TENANT_ID), anyString())).thenReturn(true);

    AlarmExpression alarmExpression = new AlarmExpression(exprStr);
    service.update(TENANT_ID, secondAlarmDef.getId(),alarmExpression,updateCommand);

  }
}
