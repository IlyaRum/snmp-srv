package com.ntc.arbiter.snmpservice.service;

import org.snmp4j.smi.Integer32;

import java.util.concurrent.atomic.AtomicBoolean;

public class ContextService {
  private final String access;
  private final Integer32 variable;
  private final String serviceName;
  private final AtomicBoolean checkInProgress;
  private final String successMessage;
  private final String failureMessage;
  private final String url;

  public ContextService(String access, Integer32 variable, String serviceName, String url) {
    this.access = access;
    this.variable = variable;
    this.serviceName = serviceName;
    this.checkInProgress = new AtomicBoolean(false);
    this.url = url;
    this.successMessage = "Соединение восстановлено к " + serviceName;
    this.failureMessage = serviceName + " не доступен и не отвечает.";
  }

  public String getAccess() {
    return access;
  }

  public Integer32 getVariable() {
    return variable;
  }

  public String getServiceName() {
    return serviceName;
  }

  public AtomicBoolean getCheckInProgress() {
    return checkInProgress;
  }

  public String getSuccessMessage() {
    return successMessage;
  }

  public String getFailureMessage() {
    return failureMessage;
  }

  public String getUrl() {
    return url;
  }
}
