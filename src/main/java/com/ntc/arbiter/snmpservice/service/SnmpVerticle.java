package com.ntc.arbiter.snmpservice.service;

import com.ntc.arbiter.snmpservice.config.AppConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

public class SnmpVerticle extends AbstractVerticle {

  private SnmpService snmpService;
  private long monitoringTimerId;

  private static final Logger logger = LoggerFactory.getLogger(SnmpVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("Starting SNMP Verticle...");

    vertx.executeBlocking(() -> {
      try {
        snmpService = new SnmpService(new MonitoringService(vertx));
        snmpService.configureAgent();

        logger.info("SNMP Agent configured successfully");

        startMonitoring();

      } catch (Exception e) {
        e.printStackTrace();
        logger.error("Failed to configure SNMP Agent", e);
        throw new RuntimeException(e);
      }
      return null;
    }, false)
      .onComplete(res->{
        if(res.succeeded()) {
          startPromise.complete();
          logger.info("SNMP Verticle started successfully");
        }
        else {
          startPromise.fail(res.cause());
        }
      });
  }

  private void startMonitoring() {
    monitoringTimerId = vertx.setPeriodic(AppConfig.getApiMonitoringTimer() * 1000, id -> {
      if (snmpService != null) {
        try {
          snmpService.checkServicesAlive();
          logger.debug("API health check performed");
        } catch (Exception e) {
          logger.warn("Health check failed: " + e.getMessage());
        }
      }
    });

    logger.info("Monitoring started");
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    logger.info("Stopping SNMP Verticle...");

    if (monitoringTimerId != 0) {
      vertx.cancelTimer(monitoringTimerId);
    }

    if (snmpService != null) {
      vertx.executeBlocking(() -> {
          snmpService.destroy();
          return null;
        }, false)
        .onComplete(ar -> {
          logger.info("SNMP Verticle stopped");
          stopPromise.complete();
        });
    } else {
      stopPromise.complete();
    }
  }
}
