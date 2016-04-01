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

package monasca.api.app.validation;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

@Test
public class DimensionsTest {
  @SuppressWarnings("serial")
  public void shouldNormalize() {
    Map<String, String> dimensions = new HashMap<String, String>();
    dimensions.put(" abc ", " 1 2 3     ");
    dimensions.put(" ezaz", "do re mi   ");
    dimensions.put("  ", "   ");

    assertEquals(DimensionValidation.normalize(dimensions), new HashMap<String, String>() {
      {
        put("abc", "1 2 3");
        put("ezaz", "do re mi");
        put(null, null);
      }
    });
  }

  @Test(expectedExceptions = WebApplicationException.class)
  @SuppressWarnings("serial")
  public void shouldThrowOnEmptyDimensionValue() {
    DimensionValidation.validate(new HashMap<String, String>() {
      {
        put("abc", "1 2 3");
        put("ezaz", "do re mi");
        put("abc", null);
      }
    });
  }

  public void shouldValidateKey() {
    DimensionValidation.validateName("this.is_a.valid-key");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldErrorOnValidateKeyWithEmptyKey() {
    DimensionValidation.validateName("");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldErrorOnValidateKeyWithLongKey() {
    String key = StringUtils.repeat("A", 256);
    DimensionValidation.validateName(key);
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldErrorOnValidateKeyWithStartingUnderscore() {
    DimensionValidation.validateName("_key");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldErrorOnValidateKeyWithInvalidCharKey() {
    DimensionValidation.validateName("this{}that");
  }

  public void shouldValidateValue() {
    DimensionValidation.validateValue("this.is_a.valid-value", "valid_name");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldErrorOnValidateValueWithEmptyValue() {
    DimensionValidation.validateValue("", "valid_name");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldErrorOnValidateValueWithLongValue() {
    String value = StringUtils.repeat("A", 256);
    DimensionValidation.validateValue(value, "valid_name");
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldErrorOnValidateValueWithInvalidCharValue() {
    DimensionValidation.validateValue("this{}that", "valid_name");
  }
}
