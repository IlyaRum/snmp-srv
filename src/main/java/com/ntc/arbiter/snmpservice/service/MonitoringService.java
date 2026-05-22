package com.ntc.arbiter.snmpservice.service;

import com.ntc.arbiter.snmpservice.config.WebClientConfiguration;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import com.ntc.arbiter.snmpservice.config.AppConfig;

public class MonitoringService {

  private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

  private final WebClient webClient;
  private static final int REQUEST_TIMEOUT_MS = 30000;

  public MonitoringService(Vertx vertx) {
    WebClientOptions webClientOptions = WebClientConfiguration.createWebClientOptions();
    this.webClient = WebClient.wrap(vertx.createHttpClient(webClientOptions));;
  }

  /**
   * Отправка GET-запроса к расчетному сервису
   */
  public Future<String> sendRequest() {
    logger.info("Отправляем GET запрос в арбитр АПИ... ");

    return webClient.getAbs(AppConfig.getApiUrl())
      .putHeader("Content-Type", "application/json")
      .timeout(REQUEST_TIMEOUT_MS)
      .send()
      .compose(response -> {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          logger.debug("Данные успешно отправлены. Ответ: " + response.bodyAsString());
          return Future.succeededFuture(response.bodyAsString());
        } else {
          return Future.failedFuture("HTTP error: " + response.statusCode() + " - " + response.bodyAsString());
        }
      })
      .onSuccess(v -> {
        logger.info("GET запрос выполнен успешно");
      })
      .onFailure(err -> logger.error("Ошибка при отправке GET запроса: " + err.getMessage()));
  }
}
