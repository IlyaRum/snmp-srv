package com.ntc.arbiter.snmpservice.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SnmpConfig {
  @JsonProperty("snmp-config.snmp-agent-address")
  private String snmpAgentAddress;

  @JsonProperty("snmp-config.snmp-client-address")
  private String snmpClientAddress;

  @JsonProperty("snmp-config.snmp-client-users")
  private String snmpClientUsers;

  @JsonProperty("iso.org.dod.internet.private.enterprises.ntc")
  private String companyId;

  @JsonProperty("ntc.snmp.system")
  private String system;

  @JsonProperty("ntc.snmp.trap")
  private String trap;

  @JsonProperty("ntc.snmp.state")
  private String state;

  @JsonProperty("ntc.snmp.alert")
  private String alert;

  @JsonProperty("ntc.snmp.stateTable")
  private String stateTable;

  @JsonProperty("ntc.snmp.severity")
  private String severity;

  @JsonProperty("ntc.system.available")
  private String available;

  @JsonProperty("ntc.system.api.alive")
  private String apiAccess;

  @JsonProperty("snmp.check.api.cron")
  private Long apiMonitoringTimer;

  // Геттеры
  public String getSnmpAgentAddress() { return snmpAgentAddress; }
  public String getSnmpClientAddress() { return snmpClientAddress; }
  public String getSnmpClientUsers() { return snmpClientUsers; }
  public String getCompanyId() { return companyId; }
  public String getSystem() { return system; }
  public String getTrap() { return trap; }
  public String getState() { return state; }
  public String getAlert() { return alert; }
  public String getStateTable() { return stateTable; }
  public String getSeverity() { return severity; }
  public String getAvailable() { return available; }
  public String getApiAccess() { return apiAccess; }
  public Long getApiMonitoringTimer() { return apiMonitoringTimer; }
}
