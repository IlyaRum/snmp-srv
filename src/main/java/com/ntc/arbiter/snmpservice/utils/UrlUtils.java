package com.ntc.arbiter.snmpservice.utils;

public class UrlUtils {

  /**
   * Извлекает host из URL для заголовка Host
   * Примеры:
   * - https://127.0.0.0:443 -> 127.0.0.0
   *
   * @param url URL для извлечения host
   * @return host без протокола, порта и пути, или null если url null или пустой
   */
  public static String extractHostFromUrl(String url) {
    if (url == null || url.isEmpty()) {
      return null;
    }
    String withoutProtocol = url.replaceFirst("^https?://", "");
    int slashIndex = withoutProtocol.indexOf('/');
    if (slashIndex > 0) {
      withoutProtocol = withoutProtocol.substring(0, slashIndex);
    }
    int colonIndex = withoutProtocol.indexOf(':');
    if (colonIndex > 0) {
      withoutProtocol = withoutProtocol.substring(0, colonIndex);
    }
    return withoutProtocol;
  }
}
