package com.ntc.arbiter.snmpservice.service;

import com.ntc.arbiter.snmpservice.config.WebClientConfiguration;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class MonitoringService {

  private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

  private final WebClient webClient;
  private static final int REQUEST_TIMEOUT_MS = 30000;

  public MonitoringService(Vertx vertx) {
    WebClientOptions webClientOptions = WebClientConfiguration.createWebClientOptions();
    this.webClient = WebClient.wrap(vertx.createHttpClient(webClientOptions));
  }

  public Future<String> sendGetRequest(String url) {
    logger.info("Отправляем GET запрос к " + url + "...");

    return webClient.getAbs(url)
      .putHeader("Content-Type", "application/json")
      .timeout(REQUEST_TIMEOUT_MS)
      .send()
      .compose(response -> {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          logger.debug("Успешный ответ от " + url + ": " + response.bodyAsString());
          return Future.succeededFuture(response.bodyAsString());
        } else {
          return Future.failedFuture("HTTP error: " + response.statusCode());
        }
      })
      .onSuccess(v -> logger.info("GET запрос к " + url + " выполнен успешно"))
      .onFailure(err -> logger.error("Ошибка при GET запросе к " + url + ": " + err.getMessage()));
  }

  /**
   * Выполняет запрос curl -kI -H "Host: msk-arbitr-dev01.ntcees.ru" https://127.0.0.0
   */
  public Future<String> sendHeadRequest(String url, String hostHeader) {
    logger.info("Отправляем UI проверку к " + url + " с Host: " + hostHeader);

    return webClient.headAbs(url)
      .putHeader("Host", hostHeader)
      .timeout(REQUEST_TIMEOUT_MS)
      .send()
      .compose(response -> {
        if (response.statusCode() == 200) {
          logger.debug("Запрос к UI успешно. Статус: " + response.statusCode());
          return Future.succeededFuture("HTTP/1.1 200 OK");
        } else {
          return Future.failedFuture("HTTP error: " + response.statusCode());
        }
      })
      .onSuccess(v -> logger.info("UI-запрос к " + url + " выполнен успешно"))
      .onFailure(err -> logger.error("Ошибка при UI-запросе к " + url + ": " + err.getMessage()));
  }
}
