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

import org.testng.annotations.Test;

import java.util.Map;

@Test
public class ValidationTest {
  public void testSimpleParseAndValidateDimensions() {
    final Map<String, String> dimensions = Validation.parseAndValidateDimensions("aa:bb,cc:dd");
    assertEquals(dimensions.size(), 2);
    assertEquals(dimensions.get("aa"), "bb");
    assertEquals(dimensions.get("cc"), "dd");
  }

  public void testParseAndValidateDimensionsWithColon() {
    final Map<String, String> dimensions = Validation.parseAndValidateDimensions("aa:bb,url:http://localhost:8081/healthcheck");
    assertEquals(dimensions.size(), 2);
    assertEquals(dimensions.get("aa"), "bb");
    assertEquals(dimensions.get("url"), "http://localhost:8081/healthcheck");
  }
}
