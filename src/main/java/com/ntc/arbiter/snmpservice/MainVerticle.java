package com.ntc.arbiter.snmpservice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;


//java -jar .\target\snmp-service-1.0.0-main.jar

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {

    //vertx.deployVerticle(new SnmpAgentVerticle());

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.get("/health").handler(ctx -> {
      ctx.json(new JsonObject().put("status", "UP").put("snmpAgent", "running"));
    });

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8888)
      .onSuccess(http -> System.out.println("HTTP server started on port 8888"))
      .onFailure(Throwable::printStackTrace);
  }
}
