package com.hpcloud.mon.app.validation;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.testng.annotations.Test;

/**
 * @author Jonathan Halterman
 */
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
    }, "joe");
  }
}
