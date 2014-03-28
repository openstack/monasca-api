package com.hpcloud.mon.resource;

import static org.testng.Assert.assertEquals;

import java.nio.charset.Charset;

import org.testng.annotations.Test;

import com.google.common.hash.Hashing;

/**
 * @author Jonathan Halterman
 */
@Test
public class LinksTest {
  public void shouldPrefixForHttps() {
    Links.accessedViaHttps = true;
    assertEquals(Links.prefixForHttps("http://abc123blah/blah/blah"),
        "https://abc123blah/blah/blah");
    assertEquals(Links.prefixForHttps("https://abc123blah/blah/blah"),
        "https://abc123blah/blah/blah");

    // Negative
    Links.accessedViaHttps = false;
    assertEquals(Links.prefixForHttps("http://abc123blah/blah/blah"), "http://abc123blah/blah/blah");
    assertEquals(Links.prefixForHttps("https://abc123blah/blah/blah"),
        "https://abc123blah/blah/blah");
  }

  public void foo() throws Exception {
    byte[] foo = Hashing.sha1()
        .newHasher()
        .putString("foo", Charset.defaultCharset())
        .putString("bar", Charset.defaultCharset())
        .hash()
        .asBytes();
    String text1 = new String(foo, Charset.forName("US-ASCII"));
    int i = 1;
  }
}
