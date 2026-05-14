package com.ntc.arbiter.snmpservice.service;

import com.ntc.arbiter.snmpservice.config.AppConfig;
import io.vertx.core.AbstractVerticle;

public class SnmpVerticle extends AbstractVerticle {

  private SnmpService snmpService;
  @Override
  public void start() {


    vertx.executeBlocking(() -> {
      try {
        AppConfig config = new AppConfig();
        snmpService = new SnmpService(config);
        snmpService.configureAgent();

      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }, false);
  }
}
