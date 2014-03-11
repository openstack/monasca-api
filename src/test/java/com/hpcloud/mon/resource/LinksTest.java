package com.hpcloud.mon.resource;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * @author Jonathan Halterman
 */
@Test
public class LinksTest {
  public void shouldPrefixForHttps() {
    Links.accessedViaHttps = true;
    assertEquals(Links.prefixForHttps("http://abc123blah/blah/blah"), "https://abc123blah/blah/blah");
    assertEquals(Links.prefixForHttps("https://abc123blah/blah/blah"),
        "https://abc123blah/blah/blah");

    // Negative
    Links.accessedViaHttps = false;
    assertEquals(Links.prefixForHttps("http://abc123blah/blah/blah"), "http://abc123blah/blah/blah");
    assertEquals(Links.prefixForHttps("https://abc123blah/blah/blah"),
        "https://abc123blah/blah/blah");
  }
}
