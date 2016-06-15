/*
 * (C) Copyright 2014-2016 Hewlett Packard Enterprise Development Company LP
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

package monasca.api.app.command;

import static monasca.common.dropwizard.JsonHelpers.jsonFixture;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonMappingException;

import monasca.api.app.command.CreateNotificationMethodCommand;
import monasca.api.domain.model.AbstractModelTest;
import monasca.api.domain.model.notificationmethod.NotificationMethodType;

@Test
public class CreateNotificationMethodTest extends AbstractModelTest {

  private static Validator validator;
  private List<Integer> validPeriods = Arrays.asList(0, 60);

  @BeforeClass
  public static void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  public void shouldDeserializeFromJson() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "a@b", "0");

    String json = jsonFixture("fixtures/newNotificationMethod.json");
    CreateNotificationMethodCommand other = fromJson(json, CreateNotificationMethodCommand.class);
    assertEquals(other, newNotificationMethod);
  }

  public void shouldDeserializeFromJsonLowerCaseEnum() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "a@b", "0");

    String json = jsonFixture("fixtures/newNotificationMethodWithLowercaseEnum.json");
    CreateNotificationMethodCommand other = fromJson(json, CreateNotificationMethodCommand.class);
    assertEquals(other, newNotificationMethod);
  }

  public void shouldDeserializeFromJsonDefinedPeriod() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod =
            new CreateNotificationMethodCommand("MyWebhook", NotificationMethodType.WEBHOOK, "http://somedomain.com", "60");

    String json = jsonFixture("fixtures/newNotificationMethodWithPeriod.json");
    CreateNotificationMethodCommand other = fromJson(json, CreateNotificationMethodCommand.class);
    assertEquals(other, newNotificationMethod);
  }

  @Test(expectedExceptions = JsonMappingException.class)
  public void shouldDeserializeFromJsonEnumError() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "a@b", "0");

    String json = jsonFixture("fixtures/newNotificationMethodWithInvalidEnum.json");
    CreateNotificationMethodCommand other = fromJson(json, CreateNotificationMethodCommand.class);
    assertEquals(other, newNotificationMethod);
  }

  public void testValidationForEmail() {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "name@domain.com", "0");
      newNotificationMethod.validate(validPeriods);
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void testValidationExceptionForEmail() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "name@domain.", "0");
    newNotificationMethod.validate(validPeriods);
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void testValidationExceptionForNonZeroPeriodForEmail() {
    CreateNotificationMethodCommand newNotificationMethod =
            new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "name@domain.", "60");
    newNotificationMethod.validate(validPeriods);
  }

  public void testValidationForWebhook() {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyWebhook", NotificationMethodType.WEBHOOK, "http://somedomain.com", "0");
      newNotificationMethod.validate(validPeriods);
  }

  public void testValidationNonZeroPeriodForWebhook() {
    CreateNotificationMethodCommand newNotificationMethod =
            new CreateNotificationMethodCommand("MyWebhook", NotificationMethodType.WEBHOOK, "http://somedomain.com", "60");
    newNotificationMethod.validate(validPeriods);
  }

  public void testValidationTestDomainForWebhook() {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyWebhook", NotificationMethodType.WEBHOOK, "http://test.test", "60");
      newNotificationMethod.validate(validPeriods);
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void testValidationInvalidDomainForWebhook() {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyWebhook", NotificationMethodType.WEBHOOK, "http://test.fred", "60");
      newNotificationMethod.validate(validPeriods);
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void testValidationExceptionForWebhook() throws Exception {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyWebhook", NotificationMethodType.WEBHOOK, "ftp://localhost", "0");
    newNotificationMethod.validate(validPeriods);
  }

  public void testValidationForPagerduty() {
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyPagerduty", NotificationMethodType.PAGERDUTY, "nzH2LVRdMzun11HNC2oD", "0");
      newNotificationMethod.validate(validPeriods);
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void testValidationExceptionForNonZeroPeriodForPagerDuty() {
    CreateNotificationMethodCommand newNotificationMethod =
            new CreateNotificationMethodCommand("MyPagerduty", NotificationMethodType.PAGERDUTY, "nzH2LVRdMzun11HNC2oD", "60");
    newNotificationMethod.validate(validPeriods);
  }

  public void testValidationForMaxNameAddress() {
    String name = StringUtils.repeat("A", 250);
    assertEquals(name.length(), 250);
    String address = "http://" + StringUtils.repeat("A", 502) + ".io";
    assertEquals(address.length(), 512);
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand(name, NotificationMethodType.WEBHOOK, address, "0");
    Set<ConstraintViolation<CreateNotificationMethodCommand>> constraintViolations =
        validator.validate(newNotificationMethod);

    assertEquals(constraintViolations.size(), 0);
  }

  public void testValidationExceptionForExceededNameLength() {
    String name = StringUtils.repeat("A", 251);
    assertEquals(name.length(), 251);
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand(name, NotificationMethodType.WEBHOOK, "http://somedomain.com", "0");
    Set<ConstraintViolation<CreateNotificationMethodCommand>> constraintViolations =
        validator.validate(newNotificationMethod);

    assertEquals(constraintViolations.size(), 1);
    assertEquals(constraintViolations.iterator().next().getMessage(),
        "size must be between 1 and 250");
  }

  public void testValidationExceptionForExceededAddressLength() {
    String address = "http://" + StringUtils.repeat("A", 503) + ".io";
    assertEquals(address.length(), 513);
    CreateNotificationMethodCommand newNotificationMethod =
        new CreateNotificationMethodCommand("MyWebhook", NotificationMethodType.WEBHOOK, address, "0");
    Set<ConstraintViolation<CreateNotificationMethodCommand>> constraintViolations =
        validator.validate(newNotificationMethod);

    assertEquals(constraintViolations.size(), 1);
    assertEquals(constraintViolations.iterator().next().getMessage(),
        "size must be between 1 and 512");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void testValidationExceptionForNonIntPeriod() {
    CreateNotificationMethodCommand newNotificationMethod =
            new CreateNotificationMethodCommand("MyEmail", NotificationMethodType.EMAIL, "name@domain.com", "interval");
    newNotificationMethod.validate(validPeriods);
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void testValidationExceptionForInvalidPeriod() {
    CreateNotificationMethodCommand newNotificationMethod =
            new CreateNotificationMethodCommand("MyWebhook", NotificationMethodType.WEBHOOK, "http://somedomain.com", "10");
    newNotificationMethod.validate(validPeriods);
  }
}
