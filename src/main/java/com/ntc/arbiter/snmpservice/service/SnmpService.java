package com.ntc.arbiter.snmpservice.service;

import com.ntc.arbiter.snmpservice.agent.SnmpAgent;
import com.ntc.arbiter.snmpservice.agent.StaticMOGroupExt;
import com.ntc.arbiter.snmpservice.config.AppConfig;
import com.ntc.arbiter.snmpservice.domain.*;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class SnmpService {

  private static final Logger logger = LoggerFactory.getLogger(SnmpService.class);

  private final Map<String, ContextService> contextServices = new ConcurrentHashMap<>();

  protected final String companyId = AppConfig.getCompanyId();
  private final String system = AppConfig.getSystem();
  private final String trap = AppConfig.getTrap();
  private final String state = AppConfig.getState();
  private final String alert = AppConfig.getAlert();
  private final String stateTable = AppConfig.getStateTable();
  private final String severity = AppConfig.getSeverity();
  private final String available = AppConfig.getAvailable();
  private final String apiAccess = AppConfig.getApiAccess();
  private final String calcAccess = AppConfig.getCalcAccess();

  private String trapId;
  private String systemId;
  private String systemStateId;
  private String systemStateTableId;
  private String alertId;

  private final static int ON = 1;
  private final static int OFF = 0;

  private SnmpAgent agent;
  private StaticMOGroupExt group;

  private SnmpState apiState;
  private SnmpState calcState;

  private boolean checkApiInProgress = false;
  private final MonitoringService monitoringService;

  public SnmpService(Vertx vertx) {
    this.monitoringService = new MonitoringService(vertx);
  }

  public void configureAgent() {
    configureAgent(AppConfig.getSnmpAgentAddress(), AppConfig.getSnmpClientAddress(), AppConfig.getSnmpClientUsers());
  }

  private void configureAgent(String agentAddress, String trapAddress, String users) {
    if (agentAddress == null || agentAddress.isEmpty()) {
      return;
    }
    systemId = companyId + '.' + system; //идентификатор системы
    systemStateId = systemId + '.' + state; //идентификатор состояния системы
    systemStateTableId = systemId + '.' + stateTable; //идентификатор состояния системы
    alertId = systemId + '.' + alert; //идентификатор атрибутов trap-сообщения
    trapId = companyId + '.' + trap; //идентификатор trap-сообщения
    try {
      agent = new SnmpAgent(agentAddress, trapAddress);
      if (users != null && !users.trim().isEmpty() && !"-".equals(users)) {
        for (String usrData : users.split("/")) {
          String[] usr = usrData.split(":");
          if (usr.length == 3) {
            agent.addUserSecurity(usr[0], usr[1], usr[2]);
          }
        }
      }
      agent.start();
      agent.unregisterManagedObject(agent.getSnmpv2MIB());
      List<VariableBinding> variables = new ArrayList<>();
      addCurrentDate(variables);
      addingVariables(variables);
      group = new StaticMOGroupExt(new OID(systemStateId), variables.toArray(new VariableBinding[0]));
      agent.registerManagedObject(group);
      agent.registerManagedObject(createStateTable());
    } catch (IOException e) {
      agent = null;
      //logger.log(Level.WARNING, "snmp.create-agent.failure", e.toString());
    }
  }

  private MOTable createStateTable() {
    OID tableRootOid = new OID(systemStateTableId + ".1");
    MOTableSubIndex[] subIndexes = new MOTableSubIndex[]{new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER)};
    MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

    DefaultMOTable table = new DefaultMOTable(tableRootOid, indexDef, SnmpState.columns());
    MOMutableTableModel model = (MOMutableTableModel) table.getModel();

    apiState = new SnmpState(apiAccess, TargetType.API, ObjectState.UNKNOWN);
    model.addRow(apiState.row());

    calcState = new SnmpState(calcAccess, TargetType.API, ObjectState.UNKNOWN);
    model.addRow(calcState.row());

    table.setVolatile(true);
    return table;
  }

  private OID getStateOID(String state) {
    return new OID(systemStateId + '.' + state);
  }

  private void addCurrentDate(List<VariableBinding> bind) {
    if (available != null) {
      bind.add(new VariableBinding(getStateOID(available), new CurrentDate()));
    }
  }

  private void addingVariables(List<VariableBinding> bind) {
    if (apiAccess != null) {

      Integer32 variable = new Integer32(ON);
      bind.add(new VariableBinding(getStateOID(apiAccess), variable));
      ContextService context = new ContextService.Builder()
        .setAccess(apiAccess)
        .setVariable(variable)
        .setServiceName("snmp.target-name.api")
        .setUrl(AppConfig.getApiUrl())
        .setConnectionRestoredValue("snmp.trap-message.key-value.api.connection.restored")
        .setNoAccessValue("snmp.trap-message.key-value.api-no-access")
        .setConnectionRestoredMessage("snmp.trap-message.alert-message.api.connection.restored")
        .setNoAccessMessage("snmp.trap-message.alert-message.api-no-access")
        .build();
      contextServices.put(apiAccess, context);
    }
    if (calcAccess != null) {
      Integer32 variable = new Integer32(ON);
      bind.add(new VariableBinding(getStateOID(calcAccess), variable));
      ContextService context = new ContextService.Builder()
        .setAccess(calcAccess)
        .setVariable(variable)
        .setServiceName("snmp.target-name.calc")
        .setUrl(AppConfig.getCalcUrl())
        .setConnectionRestoredValue("snmp.trap-message.key-value.calc.connection.restored")
        .setNoAccessValue("snmp.trap-message.key-value.calc-no-access")
        .setConnectionRestoredMessage("snmp.trap-message.alert-message.calc.connection.restored")
        .setNoAccessMessage("snmp.trap-message.alert-message.calc-no-access")
        .build();
      contextServices.put(calcAccess, context);
    }

    //todo добавить сюда остальные параметры
  }

  public void checkServicesAlive() {
    if (group == null) {
      return;
    }

    for (ContextService contextService : contextServices.values()) {
      checkSingleSeviceAlive(contextService);
    }
  }

  private void checkSingleSeviceAlive(ContextService context) {
    Integer32 variable = (Integer32) group.get(getStateOID(context.getAccess()));
    if (variable == null) {
      return;
    }

    context.getCheckInProgress().set(true);

    String url = context.getUrl();

    monitoringService.sendRequest(url)
      .onComplete(
        rez -> {
          try {
            if (rez.succeeded()) {
              String responseBody = rez.result();
              logger.info("Получен ответ от " + context.getServiceName() + ": " + responseBody);
              handleMonitoringValue(variable, context, true, context.getSuccessMessage());
            } else {
              handleMonitoringValue(variable, context, false, context.getFailureMessage());
            }
          } catch (Exception ex) {
            try {
              handleMonitoringValue(variable, context, false, context.getFailureMessage());
            } catch (Exception handleEx) {
              logger.error("Ошибка обработки: " + context.getServiceName() + ": " + ex.getMessage());
            }
          } finally {
            context.getCheckInProgress().set(false);
          }
        });
  }

  @Deprecated
  public void checkApiAlive(ContextService context) {
    if (group == null || apiAccess == null || checkApiInProgress) {
      return;
    }
    Integer32 apiAccessValue = (Integer32) group.get(getStateOID(apiAccess));
    if (apiAccessValue == null) {
      return;
    }
    checkApiInProgress = true;

    monitoringService.sendRequest()
      .onComplete(
        rez -> {
          try {
            if (rez.succeeded()) {
              String responseBody = rez.result();
              logger.info("Получен ответ от API: " + responseBody);
              handleMonitoringValue(apiAccessValue, context, true, "Соединение восстановлено к АПИ сервису.");
            } else {
              handleMonitoringValue(apiAccessValue, context, false, "API сервис не доступен и не отвечает.");
            }
          } catch (Exception ex) {
            try {
              handleMonitoringValue(apiAccessValue, context, false, "API сервис не доступен и не отвечает.");
            } catch (Exception handleEx) {
              logger.error("Ошибка обработки: " + ex.getMessage());
            }
          } finally {
            checkApiInProgress = false;
          }
        });
  }

  private void handleMonitoringValue(Integer32 value, ContextService context, boolean success, String logMessage) {
    Function<SnmpEvent.Severity, SnmpEvent> eventCreator = s -> SnmpEvent.createTrapEvent(trapId, alertId, s, success, context);
    if (success) {
      apiState.setState(ObjectState.OK);
      if (value.getValue() == OFF) {
        logger.info(logMessage);
        SnmpEvent.Severity severityValue = SnmpEvent.Severity.CLEARED;
        logger.info("Creating TRAP-message " + logMessage + " value " + severityValue.toString());
        sendTrapEvent(eventCreator.apply(severityValue));
      }
      value.setValue(ON);
    } else {
      logger.error(logMessage);
      apiState.setState(ObjectState.CRITICAL_ERROR);
      if (value.getValue() != OFF) {
        SnmpEvent.Severity severityValue = SnmpEvent.Severity.valueOf(severity);
        logger.info("Creating TRAP-message " + logMessage + " value " + severityValue.toString());
        sendTrapEvent(eventCreator.apply(severityValue));
      }
      value.setValue(OFF);
    }
  }

  private void sendTrapEvent(SnmpEvent event) {
    try {
      agent.sendTrap(event);
      logger.info("TRAP-message send");
    } catch (IOException e) {
      logger.error("Error on send TRAP-message. ", e);
    }
  }

  public void destroy() {
    try {
      agent.stop();
    } catch (Exception ignored) {
    }
  }
}
