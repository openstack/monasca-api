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

package monasca.api.app.validation;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.testng.annotations.Test;

@Test
public class ValueMetaValidationTest {
  @Test
  @SuppressWarnings("serial")
  public void shouldNormalize() {
    Map<String, String> valueMeta = new HashMap<String, String>();
    final String value1_with_spaces = " 1 2 3     ";
    valueMeta.put(" abc ", value1_with_spaces);
    final String value2_with_spaces = "do re mi   ";
    valueMeta.put(" ezaz", value2_with_spaces);

    assertEquals(ValueMetaValidation.normalize(valueMeta), new HashMap<String, String>() {
      {
        put("abc", value1_with_spaces);
        put("ezaz", value2_with_spaces);
      }
    });
  }

  @SuppressWarnings("serial")
  public void emptyOk() {
    ValueMetaValidation.validate(new HashMap<String, String>() {
      {
      }
    });
  }

  public void maxOk() {
    final Map<String, String> valueMeta = new HashMap<String, String>();
    for (int i = 0; i < 16; i++) {
      //
      // All 16 name/value pairs (converted to json) must fit in 2048
      // chars.  Test that we can fit 1/16th of 2048 in each pair (128 chars):
      //
      // {"name":"value"}, ...
      // ^^    ^^^     ^^^     <-- extra chars (8 per pair)
      //
      valueMeta.put(makeString(i, 10), makeString(i, (128 - (10+8))));
    }
    ValueMetaValidation.validate(valueMeta);
  }

  @SuppressWarnings("serial")
  public void emptyValueOk() {
    final String key = "noValue";
    HashMap<String, String> emptyValue = new HashMap<String, String>() {
      {
        put(key, "");
      }
    };
    ValueMetaValidation.validate(emptyValue);
    assertEquals(ValueMetaValidation.normalize(emptyValue), new HashMap<String, String>() {
      {
        put(key, "");
      }
    });
  }

  private String makeString(int num, int len) {
    final StringBuilder builder = new StringBuilder(len);
    while (builder.length() < len) {
      builder.append(num);
      builder.append('-');
    }
    builder.setLength(len);
    return builder.toString();
  }

  @Test(expectedExceptions = WebApplicationException.class)
  @SuppressWarnings("serial")
  public void shouldThrowOnEmptyValueMetaName() {
    ValueMetaValidation.validate(new HashMap<String, String>() {
      {
        put("abc", "  1 2 3   ");
        put("ezaz", "do re mi     ");
        put("  ", "Bad");
      }
    });
  }

  @Test(expectedExceptions = WebApplicationException.class)
  @SuppressWarnings("serial")
  public void shouldThrowOnNullValueMetaName() {
    ValueMetaValidation.validate(new HashMap<String, String>() {
      {
        put("abc", "  1 2 3   ");
        put("ezaz", "do re mi     ");
        put(null, "Bad");
      }
    });
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldThrowOnTooManyValueMeta() {
    final Map<String, String> valueMeta = new HashMap<String, String>();
    for (int i = 0; i < 17; i++) {
      valueMeta.put(makeString(i, 255), makeString(i, 2048));
    }
    ValueMetaValidation.validate(valueMeta);
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldThrowOnValueMetaNameTooLarge() {
    final Map<String, String> valueMeta = new HashMap<String, String>();
    for (int i = 0; i < 16; i++) {
      valueMeta.put(makeString(i, 256), makeString(i, 2048));
    }
    ValueMetaValidation.validate(valueMeta);
  }

  @Test(expectedExceptions = WebApplicationException.class)
  public void shouldThrowOnValueMetaValueTooLarge() {
    final Map<String, String> valueMeta = new HashMap<String, String>();
    for (int i = 0; i < 16; i++) {
      valueMeta.put(makeString(i, 255), makeString(i, 2049));
    }
    ValueMetaValidation.validate(valueMeta);
  }
}
