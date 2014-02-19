package com.hpcloud.mon;

public class MonApiServiceRunner {
  public static void main(String... args) throws Exception {
    MonApiApplication.main(new String[] { "server", "config-test.yml" });
  }
}
