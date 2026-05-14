package com.ntc.arbiter.snmpservice.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

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

  protected static String companyId;
  private static String system;
  private static String trap;
  private static String state;
  private static String alert;
  private static String stateTable;
  private static String severity;
  private static String available;
  private static String apiAccess;

  public static void loadConfig() {
    String configFile = System.getProperty("config.file", "config.json");

    try {
      Properties props = new Properties();
      String filePath = "./" + configFile;
      File file = new File(filePath);
      if (!file.exists()) {
        filePath = "./src/main/resources/" + configFile;
        file = new File(filePath);
      }

      System.out.println("Config file is here: " + filePath);

      ObjectMapper mapper = new ObjectMapper();
      props.putAll(mapper.readValue(file, new TypeReference<>() {
      }));

      snmpAgentAddress = props.getProperty("snmp-config.snmp-agent-address");
      snmpClientAddress = props.getProperty("snmp-config.snmp-client-address");
      snmpClientUsers = props.getProperty("snmp-config.snmp-client-users");

      companyId = props.getProperty("iso.org.dod.internet.private.enterprises.ntc");
      system = props.getProperty("ntc.snmp.system");
      trap = props.getProperty("ntc.snmp.trap");
      state = props.getProperty("ntc.snmp.state");
      alert = props.getProperty("ntc.snmp.alert");
      stateTable = props.getProperty("ntc.snmp.stateTable");
      severity = props.getProperty("ntc.snmp.severity");
      available = props.getProperty("ntc.system.available");
      apiAccess = props.getProperty("ntc.system.api.alive");

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
}
