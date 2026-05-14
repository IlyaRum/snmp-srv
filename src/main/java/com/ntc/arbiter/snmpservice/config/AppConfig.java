package com.ntc.arbiter.snmpservice.config;

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
  private String snmpAgentAddress;
  /**
   * Адрес smnp для отправки trap-сообщений в виде dp:127.0.0.1/162|public, где 127.0.0.1 - куда отправлять
   */
  private String snmpClientAddress;
  /**
   * Имя snmp-пользователя
   */
  private String snmpClientUsers;
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

  public String getSnmpAgentAddress() {
    return snmpAgentAddress;
  }

  public String getSnmpClientAddress() {
    return snmpClientAddress;
  }

  public String getSnmpClientUsers() {
    return snmpClientUsers;
  }
}
