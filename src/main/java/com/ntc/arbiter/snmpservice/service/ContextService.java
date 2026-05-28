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
  private final String hostHeader;

  private final String connectionRestoredValue;
  private final String noAccessValue;
  private final String connectionRestoredMessage;
  private final String noAccessMessage;


  private ContextService(Builder builder) {
    this.access = builder.access;
    this.variable = builder.variable;
    this.serviceName = builder.serviceName;
    this.checkInProgress = new AtomicBoolean(false);
    this.url = builder.url;
    this.hostHeader = builder.hostHeader;
    this.successMessage = "Соединение восстановлено к " + this.serviceName + ".";
    this.failureMessage = this.serviceName + " не доступен и не отвечает.";
    this.connectionRestoredValue = builder.connectionRestoredValue;
    this.noAccessValue = builder.noAccessValue;
    this.connectionRestoredMessage = builder.connectionRestoredMessage;
    this.noAccessMessage = builder.noAccessMessage;
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

  public String getHostHeader() {
    return hostHeader;
  }

  public String getConnectionRestoredValue() {
    return connectionRestoredValue;
  }

  public String getNoAccessValue() {
    return noAccessValue;
  }

  public String getConnectionRestoredMessage() {
    return connectionRestoredMessage;
  }

  public String getNoAccessMessage() {
    return noAccessMessage;
  }

  public static class Builder {
    private String access;
    private Integer32 variable;
    private String serviceName;
    private String url;
    private String hostHeader;
    private String connectionRestoredValue;
    private String noAccessValue;
    private String connectionRestoredMessage;
    private String noAccessMessage;

    public Builder setAccess(String access) {
      this.access = access;
      return this;
    }

    public Builder setVariable(Integer32 variable) {
      this.variable = variable;
      return this;
    }

    public Builder setServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder setUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder setHostHeader(String hostHeader) {
      this.hostHeader = hostHeader;
      return this;
    }

    public Builder setConnectionRestoredValue(String connectionRestoredValue) {
      this.connectionRestoredValue = connectionRestoredValue;
      return this;
    }

    public Builder setNoAccessValue(String noAccessValue) {
      this.noAccessValue = noAccessValue;
      return this;
    }

    public Builder setConnectionRestoredMessage(String connectionRestoredMessage) {
      this.connectionRestoredMessage = connectionRestoredMessage;
      return this;
    }

    public Builder setNoAccessMessage(String noAccessMessage) {
      this.noAccessMessage = noAccessMessage;
      return this;
    }

    public ContextService build() {
      return new ContextService(this);
    }
  }
}
