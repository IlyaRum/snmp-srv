package com.ntc.arbiter.snmpservice.service;

import io.vertx.core.AbstractVerticle;

public class SnmpVerticle extends AbstractVerticle {

  private SnmpService snmpService;

  @Override
  public void start() {

    vertx.executeBlocking(() -> {
      try {
        snmpService = new SnmpService();
        snmpService.configureAgent();

      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }, false);
  }
}
