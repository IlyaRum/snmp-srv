package com.ntc.arbiter.snmpservice.config;

import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

public class AppConfig {
  private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

  private static final String CONFIG_FILE_NAME = "config.json";

  /**
   * Адрес snmp-агента для ответов на запросы в виде udp:0.0.0.0/161|public, где 161 - порт прослушивания
   */
  private static String snmpAgentAddress;
  /**
   * Адрес smnp для отправки trap-сообщений в виде dp:127.0.0.1/162|public, где 127.0.0.1 - куда отправлять
   */
  private static String snmpClientAddress;
  /**
   * Имя snmp-пользователя
   */
  private static String snmpClientUsers;

  private static String companyId;
  private static String system;
  private static String trap;
  private static String state;
  private static String alert;
  private static String stateTable;
  private static String severity;
  private static String available;
  private static String apiAccess;
  private static String calcAccess;
  private static String uiAccess;
  private static Long apiMonitoringTimer;

  private static Boolean isTrust;
  private static String certCrt;
  private static String apiUrl;
  private static String calcUrl;
  private static String uiUrl;

  public static void loadConfig(Vertx vertx) {

    JsonObject config = vertx.getOrCreateContext().config();

    if (config == null || config.isEmpty()) {
      try {
        String configContent = vertx.fileSystem().readFileBlocking(CONFIG_FILE_NAME).toString();
        config = new JsonObject(configContent);
        logger.info("Конфигурация загружена из файла config.json");
      } catch (Exception e) {
        logger.error(e.getMessage());
        throw new RuntimeException("Failed to load configuration file: " + CONFIG_FILE_NAME, e);
      }
    }

    snmpAgentAddress = config.getString("snmp-config.snmp-agent-address");
    snmpClientAddress = config.getString("snmp-config.snmp-client-address");
    snmpClientUsers = config.getString("snmp-config.snmp-client-users");

    companyId = config.getString("iso.org.dod.internet.private.enterprises.ntc");
    system = config.getString("ntc.snmp.system");
    trap = config.getString("ntc.snmp.trap");
    state = config.getString("ntc.snmp.state");
    alert = config.getString("ntc.snmp.alert");
    stateTable = config.getString("ntc.snmp.stateTable");
    severity = config.getString("ntc.snmp.severity");
    available = config.getString("ntc.system.available");
    apiAccess = config.getString("ntc.system.api.alive");
    calcAccess = config.getString("ntc.system.calc.alive");
    uiAccess = config.getString("ntc.system.ui.alive");
    apiMonitoringTimer = config.getLong("snmp.check.api.cron");
    isTrust = config.getBoolean("trust.all");
    certCrt = config.getString("cert.crt");
    apiUrl = config.getString("snmp-config.api-monitoring-url");
    calcUrl = config.getString("snmp-config.calc-monitoring-url");
    uiUrl = config.getString("snmp-config.ui-monitoring-url");
  }

  public static String getSnmpAgentAddress() {
    return snmpAgentAddress;
  }

  public static String getSnmpClientAddress() {
    return snmpClientAddress;
  }

  public static String getSnmpClientUsers() {
    return snmpClientUsers;
  }

  public static String getCompanyId() {
    return companyId;
  }

  public static String getSystem() {
    return system;
  }

  public static String getTrap() {
    return trap;
  }

  public static String getState() {
    return state;
  }

  public static String getAlert() {
    return alert;
  }

  public static String getStateTable() {
    return stateTable;
  }

  public static String getSeverity() {
    return severity;
  }

  public static String getAvailable() {
    return available;
  }

  public static String getApiAccess() {
    return apiAccess;
  }

  public static String getCalcAccess() {
    return calcAccess;
  }

  public static String getUiAccess() {
    return uiAccess;
  }

  public static Long getApiMonitoringTimer() {
    return apiMonitoringTimer;
  }

  public static Boolean isTrustAll() {
    return isTrust;
  }

  public static String getCertCrt() {
    return certCrt;
  }

  public static String getApiUrl() {
    return apiUrl;
  }

  public static String getCalcUrl() {
    return calcUrl;
  }

  public static String getUiUrl() {
    return uiUrl;
  }
}
