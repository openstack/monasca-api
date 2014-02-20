package com.hpcloud.mon.infrastructure.identity;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.hpcloud.http.rest.UnauthorizedException;
import com.hpcloud.mon.infrastructure.identity.IdentityServiceClient.TokenCallable;

/**
 * @author Jonathan Halterman
 */
@Test
public class IdentityServiceClientTest {
  private IdentityServiceClient client;

  @BeforeMethod
  protected void beforeMethod() {
    client = spy(new IdentityServiceClient(null, null));
  }

  public void shouldRefreshTokenAfterExpiration() {
    doReturn("12345").when(client).requestAuthToken();
    String foo = client.withToken(new TokenCallable<String>() {
      @Override
      public String call(String authToken) {
        return "foo";
      }
    });

    assertEquals(client.getAuthToken(), "12345");
    assertEquals(foo, "foo");

    doReturn("67890").when(client).requestAuthToken();
    final AtomicBoolean failed = new AtomicBoolean();
    foo = client.withToken(new TokenCallable<String>() {
      @Override
      public String call(String authToken) {
        if (!failed.get()) {
          failed.set(true);
          throw new UnauthorizedException("boo");
        }

        return "bar";
      }
    });

    assertEquals(client.getAuthToken(), "67890");
    assertEquals(foo, "bar");
  }

  public void shouldUseInternedToken() {
    doReturn("12345").when(client).requestAuthToken();
    assertEquals(client.getAuthToken(), client.getAuthToken());
  }
}
