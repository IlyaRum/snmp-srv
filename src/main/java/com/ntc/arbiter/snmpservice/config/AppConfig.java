package com.ntc.arbiter.snmpservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class AppConfig {
  /**
   * Имя файла конфигурации задач супервизора
   */
  private String configPath;
  /**
   * Кодировка сообщений клиентов текстового протокола SLICP
   */
  private String charset;
  /**
   * Порт для подключения клиентов текстового протокола SLICP
   */
  private Integer supervisorPort;
  /**
   * Таймаут по умолчанию при работе с tcp-сокетами
   */
  private int defaultSocketTimeoutMs;
//  /**
//   * Адрес snmp-агента для ответов на запросы в виде udp:0.0.0.0/161|public, где 161 - порт прослушивания
//   */
//  @JsonProperty("snmp-config.snmp-agent-address")
  private static String snmpAgentAddress;
//  /**
//   * Адрес smnp для отправки trap-сообщений в виде dp:127.0.0.1/162|public, где 127.0.0.1 - куда отправлять
//   */
//  @JsonProperty("snmp-config.snmp-client-address")
  private static String snmpClientAddress;
//  /**
//   * Имя snmp-пользователя
//   */
//  @JsonProperty("snmp-config.snmp-client-users")
  private static String snmpClientUsers;
  /**
   * Адрес службы api для проверки доступности БД со стороны API
   */
  private String apiMonitoringUrl;

  /**
   * Host для cspa-api-server
   */
  private String apiHost;
  /**
   * Макрос базовой директории
   */
  private String baseDirMacro;
  /**
   * Базовая директория файлов
   */
  private String baseDir;
  /**
   * Значение заголовка "User-Agent" службы api
   * Необходимо для обеспечения разного уровня логирования у фильтра регистрации HTTP-запросов
   */
  private String apiUserAgent;
  /**
   * Код неизвестной ошибки выполнения
   */
  private Integer unknownErrorCode;
  /**
   * Признак необходимости использовать безопасный режим планирования
   */
  private boolean safeScheduleMode;
  /**
   * Директория для отладочных файлов
   */
  private String tracingFilesDirectory;
  /**
   * Количество потоков на одно ядро, необходимо для расчета размера основного пула потоков Супервизора
   */
  private int processorPoolSize;
  /**
   * Конфигурация команды по TCP (ЦСПА old-style)
   */
  //@NestedConfigurationProperty
  //private TcpCommandConfig tcpCommandConfig;
  /**
   * Конфигурация команды по REST
   */
  //@NestedConfigurationProperty
  //private RestCommandConfig restCommandConfig;

  /**
   * Таймаут штатной остановки сервиса Супервизора
   */
  private int gracefulShutdownWaitSeconds;

//  @JsonProperty("iso.org.dod.internet.private.enterprises.ntc")
  private static String companyId;
//
//  @JsonProperty("ntc.snmp.system")
  private static String system;
//
//  @JsonProperty("ntc.snmp.trap")
  private static String trap;
//
//  @JsonProperty("ntc.snmp.state")
  private static String state;
//
//  @JsonProperty("ntc.snmp.alert")
  private static String alert;
//
//  @JsonProperty("ntc.snmp.stateTable")
  private static String stateTable;
//
//  @JsonProperty("ntc.snmp.severity")
  private static String severity;
//
//  @JsonProperty("ntc.system.available")
  private static String available;
//
//  @JsonProperty("ntc.system.api.alive")
  private static String apiAccess;
//
//  @JsonProperty("snmp.check.api.cron")
  private static Long apiMonitoringTimer;

  public static void loadConfig() throws IOException {
    String configFile = System.getProperty("config.file", "config.json");



    try {
//      Properties props = new Properties();
      String filePath = "./" + configFile;
      File file = new File(filePath);
      if (!file.exists()) {
        filePath = "./src/main/resources/" + configFile;
        file = new File(filePath);
      }


      System.out.println("Config file is here: " + filePath);

      ObjectMapper mapper = new ObjectMapper();

      SnmpConfig config = mapper.readValue(file, SnmpConfig.class);
      //props.putAll(mapper.readValue(file, new TypeReference<>() {}));

      snmpAgentAddress = config.getSnmpAgentAddress();
      snmpClientAddress = config.getSnmpClientAddress();
      snmpClientUsers = config.getSnmpClientUsers();

      companyId = config.getCompanyId();
      system = config.getSystem();
      trap = config.getTrap();
      state = config.getState();
      alert = config.getAlert();
      stateTable = config.getStateTable();
      severity = config.getSeverity();
      available = config.getAvailable();
      apiAccess = config.getApiAccess();
      apiMonitoringTimer = config.getApiMonitoringTimer();

    } catch (IOException e) {
      throw new RuntimeException("Failed to load configuration file: " + configFile, e);
    }
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

  public static Long getApiMonitoringTimer() {
    return apiMonitoringTimer;
  }
}
