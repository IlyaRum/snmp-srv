package com.ntc.arbiter.snmpservice;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class AppLauncher {
  public static void main(String[] args) {

    VertxOptions options = new VertxOptions()
      .setBlockedThreadCheckInterval(5000);

    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
