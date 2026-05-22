package com.ntc.arbiter.snmpservice.config;

import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Класс для настройки опции WEB клиентов
 */
public class WebClientConfiguration {

  private static final boolean TRUST_ALL = AppConfig.isTrustAll();
  private static final Logger logger = LoggerFactory.getLogger(WebClientConfiguration.class);
  static {
    if(TRUST_ALL){
      logger.warn("Включен режим доверия всем сертификатам сервера. SSL-проверка отключена.");
    }
  }

  public static WebClientOptions createWebClientOptions() {
    WebClientOptions options = new WebClientOptions()
      .setKeepAlive(true)
      .setConnectTimeout(5000);
    if (TRUST_ALL) {
      options
        .setSsl(false)
        .setTrustAll(true)
        .setVerifyHost(false);
    } else {
      PemTrustOptions trustOptions = new PemTrustOptions().addCertPath(AppConfig.getCertCrt());

      options
        .setSsl(true)
        .setTrustAll(false)
        .setTrustOptions(trustOptions)
        .setVerifyHost(false);
    }

    return options;
  }
}
